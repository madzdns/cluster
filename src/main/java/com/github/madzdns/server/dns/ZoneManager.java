package com.github.madzdns.server.dns;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.github.madzdns.server.dns.utils.DnsHelper;
import com.github.madzdns.server.core.config.entry.dns.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.AAAARecord;
import org.xbill.DNS.ACNAMERecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.BCNAMERecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.DYRecord;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.RCNAMERecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Zone;

import com.github.madzdns.server.core.api.FRfra;
import com.github.madzdns.server.core.api.LookupService;
import com.github.madzdns.server.core.api.XbillDnsService.NameRecord;
import com.github.madzdns.server.core.config.Config;
import com.github.madzdns.server.core.config.EntryResolveModes;
import com.github.madzdns.server.core.config.NameserverConfig;
import com.github.madzdns.server.core.config.Resource;
import com.github.madzdns.server.core.config.Soa;

import com.github.madzdns.server.core.utils.DomainHelper;

public class ZoneManager {
	
	private static Logger log = LoggerFactory.getLogger(ZoneManager.class);
			
	private static ConcurrentHashMap<Name,Zone> zones = null;
	
	private static List<String> fakeNameServers;
	
	static {
		
		fakeNameServers = new ArrayList<String>();
		fakeNameServers.add(new StringBuilder("simple.fns1.").append(Config.MAIN_DOMAIN_STR).toString());
		fakeNameServers.add(new StringBuilder("simple.fns2.").append(Config.MAIN_DOMAIN_STR).toString());
	}
	
	public static void setupZones() {
		
		Map<String, Resource> resz = Config.getZoneResz(null);
		
		try {
			
			if(resz == null) {
				
				log.error("No loaded resources found");
				return;	
			}
			
			if(Config.getRootSoa() == null) {
				
				log.error("No root SOA found");
				return;
			}
			
			ConcurrentHashMap<Name,Zone> tmpZones = 
					new ConcurrentHashMap<Name,Zone>();	
			
			List<Record> arpaDnsRecords = new ArrayList<Record>();
			
			List<Record> rootNsRecords = null;
			
			Name rootZonebaseNameForDomainAsName = null;
			
			List<Record> rootZoneDnsRecords = new ArrayList<>();

			for(Iterator<Entry<String, Resource>> it = resz.entrySet().iterator();
					it.hasNext();) {
				
				Entry<String, Resource> g = it.next();
				
				if(g.getValue().getDnsList() != null && g.getValue().getDnsList().size() > 0) {
				
					List<String> definedNsList = new ArrayList<String>();
					
					if(Config.MainDomainNameserverConfig != null) {
					
						NameserverConfig nsc = Config.MainDomainNameserverConfig;
						
						String namePrefix = g.getValue().getId();
						
						for(int i =0; i < nsc.getCount(); i++) {
							
							String definedNs = new StringBuilder(namePrefix).
									append("-").append(i+1).
									append(".").append(nsc.getName()).
									append(".").append(Config.MAIN_DOMAIN_STR).
									toString();
							
							definedNsList.add(definedNs);
							
							rootZoneDnsRecords.add(new DYRecord(Name.fromString(definedNs), nsc.getTtl(), 
									DYRecord.RESOLVE_WITH_DNS_SERVER));
						}
					}
					
					for(Iterator<EntryDns> edIt = g.getValue().getDnsList().iterator();
						edIt.hasNext();) {
						
						EntryDns eDns = edIt.next();

						if(eDns.getEntry() != null) {
							
							byte rm = (byte)eDns.getEntry().getResolveMode();
							
							if(rm == EntryResolveModes.PROTO_RESOLVE_PROXY.getValue() ||
									rm == EntryResolveModes.PROTO_RESOLVE.getValue() ) {

								rootZoneDnsRecords.add(new DYRecord(Name.fromString(eDns.getEntry().getFullAName()), eDns.getEntry().getTtl(), DYRecord.RESOLVE_WITH_DNS_SERVER));
							}
							else {
								
								rootZoneDnsRecords.add(new DYRecord(Name.fromString(eDns.getEntry().getFullAName()), eDns.getEntry().getTtl(), DYRecord.RESOLVE_DYNAMICALLY));
							}
						}
						
						Soa commonSoa = createCommonSoa(Config.getRootSoa());
						
						String baseNameForDomain = eDns.getName();
						
						log.debug("Creating zone for name {}", baseNameForDomain);
						
						Name baseNameForDomainAsName = Name.fromString(baseNameForDomain);
						
						List<Record> simpleDnsRecords = null;
						
						if(!eDns.isRoot()) {
							
							if(Config.getApp().isCheckForNsValidity()) {

								List<NameRecord> nsNameRecords = LookupService.nsLookupWithDelegation(baseNameForDomain, definedNsList);
								
								if(nsNameRecords == null || nsNameRecords.size() == 0) {
									
									log.warn("Name Server config was wrong for domain {} - discarding dns setting", baseNameForDomain);
									continue;
								}
								
								simpleDnsRecords = new ArrayList<Record>();
								
								for(Iterator<NameRecord> nsNameRecordsIt = nsNameRecords.iterator(); 
										nsNameRecordsIt.hasNext();) {
								
									NameRecord field = nsNameRecordsIt.next();
									
									Name nameServer = field.getName();
									
									String theName = nameServer.toString();
									
									if(!nameServer.isAbsolute()) {
										
										theName = new StringBuilder(theName).append(".").toString();
										
										nameServer = Name.fromString(theName);
									}
									
									simpleDnsRecords.add(new NSRecord(baseNameForDomainAsName,
											DClass.IN,
											field.getTTL(),
											nameServer));

									//I meant this should only be answered with incoming interface address
									rootZoneDnsRecords.add(new DYRecord(nameServer, field.getTTL(), DYRecord.RESOLVE_WITH_DNS_SERVER));	
								}

								String primaryNs = nsNameRecords.get(0).getName().toString();
								
								if(!primaryNs.endsWith(".")) {
									
									primaryNs = new StringBuilder(primaryNs).append(".").toString();
								}
								
								commonSoa.setNs(primaryNs);
							}
							else {
								//In real life does not matter
								//because it should not reach here
								simpleDnsRecords = new ArrayList<Record>();
								
								for(int rr = 0; rr < fakeNameServers.size(); rr++) {
									
									Record r = new NSRecord(baseNameForDomainAsName,
											DClass.IN,
											3600,
											Name.fromString(fakeNameServers.get(rr)));
									simpleDnsRecords.add(r);
									
									//I meant this should only be answered with incoming interface address
									rootZoneDnsRecords.add(new DYRecord(Name.fromString(fakeNameServers.get(rr)), 3600, DYRecord.RESOLVE_WITH_DNS_SERVER));

								}
								
								commonSoa.setNs(fakeNameServers.get(0));
							}
						}
						else {
							
							simpleDnsRecords = rootZoneDnsRecords;
							
							rootZonebaseNameForDomainAsName = baseNameForDomainAsName;
						}
						
						if(eDns.getSoaRecord() != null) {
							
							Soa rec = eDns.getSoaRecord();
							
							simpleDnsRecords.add(new SOARecord(baseNameForDomainAsName,
									DClass.IN,
									rec.getTtl(),
									Name.fromString(rec.getNs()),
									Name.fromString(rec.getMail()),
									rec.getSerial(),
									rec.getRefresh(),
									rec.getRetry(),
									rec.getExpire(),
									rec.getMinttl()));
						}
						else {
							
							simpleDnsRecords.add(new SOARecord(baseNameForDomainAsName,
									DClass.IN,
									commonSoa.getTtl(),
									Name.fromString(commonSoa.getNs()),
									Name.fromString(commonSoa.getMail()),
									commonSoa.getSerial(),
									commonSoa.getRefresh(),
									commonSoa.getRetry(),
									commonSoa.getExpire(),
									commonSoa.getMinttl()));
						}
						
						Name dnsName = null;
						
						Name target = null;
						
						if(eDns.getNsRecords() != null) {
							
							if(eDns.isRoot()) {
								
								rootNsRecords = new ArrayList<Record>();
							}
							
							for(Iterator<ENs> dnsFieldIt = eDns.getNsRecords().iterator(); dnsFieldIt.hasNext();) {
								
								ENs rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString());
									
									target = null;
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									Record r = new NSRecord(dnsName, DClass.IN, rec.getTtl(), target);
									
									if(eDns.isRoot()) {
										
										rootNsRecords.add(r);
									}
									
									simpleDnsRecords.add(r);

								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getAcnameRecord() != null && eDns.getEntry() != null) {
							
							EAcn rec = eDns.getAcnameRecord();
							
							try {
								
								dnsName = baseNameForDomainAsName;
								
								target = null;
								
								if(rec.getTarget() != null && !rec.getTarget().equals("")) {
									
									if(!rec.getTarget().endsWith(".")) {
										
										target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
												.append(Config.MAIN_DOMAIN_STR).toString());
									}
									else {
										
										target = Name.fromString(rec.getTarget());
									}
								}
								else {
									
									/*target = baseNameForDomainAsName;*/
									target = Name.fromString(eDns.getEntry().getFullAName());
								}
								
								simpleDnsRecords.add(new ACNAMERecord(dnsName, DClass.IN,
								rec.getTtl(), target));	
								
							} catch (Exception e) {
								
								log.error("",e);
							}
						}
						else if(eDns.getBcnameRecord() != null) {
							
							EBcn rec = eDns.getBcnameRecord();
							
							try {
								
								dnsName = baseNameForDomainAsName;
								
								target = null;
								
								if(rec.getTarget() != null && !rec.getTarget().equals("")) {
									
									if(!rec.getTarget().endsWith(".")) {
										
										target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
												.append(baseNameForDomain).toString());
									}
									else {
										
										target = Name.fromString(rec.getTarget());
									}
								}
								else {
									
									target = baseNameForDomainAsName;
								}

								simpleDnsRecords.add(new BCNAMERecord(dnsName, DClass.IN,
								rec.getTtl(),target));	
								
							} catch (Exception e) {
								
								log.error("",e);
							}
						}
						else if(eDns.getRcnameRecord() != null && eDns.getEntry() != null) {
							
							ERcn rec = eDns.getRcnameRecord();
							
							try {
								
								dnsName = baseNameForDomainAsName;
								
								target = null;
								
								String fullAname = eDns.getEntry().getFullAName();
								
								if(rec.getTarget() != null && !rec.getTarget().equals("")) {
									
									String targetName = rec.getTarget();
									
									if(!targetName.endsWith(".")) {
										
										targetName = new StringBuilder(targetName).append(".")
												.append(baseNameForDomain).toString();
									}
									
									DomainHelper dh = new DomainHelper(targetName);

									if(dh.IsFullDomain()) {
										
										targetName = dh.GetPureDomain();
										fullAname = new StringBuilder(dh.getApiPart()).append(".").append(fullAname).toString();
									}
									else {
										
										fullAname = new StringBuilder(Config.getApp().getApiPrefix()).
												append("-").append(FRfra.FD_FORWARD).append(".").
												append(fullAname).toString();
									}
									
									target = Name.fromString(targetName);
								}
								else {
									
									fullAname = new StringBuilder(Config.getApp().getApiPrefix()).
											append("-").append(FRfra.FD_FORWARD).append(".").
											append(fullAname).toString();
									target = baseNameForDomainAsName;
								}

								simpleDnsRecords.add(new RCNAMERecord(dnsName, DClass.IN,
								rec.getTtl(),target, Name.fromString(fullAname)));	
								
							} catch (Exception e) {
								
								log.error("",e);
							}
						}
						
						if(eDns.getMxRecords() != null) {
							
							for(Iterator<EMx> dnsFieldIt = eDns.getMxRecords().iterator(); dnsFieldIt.hasNext();) {
								
								EMx rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString());
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new MXRecord(dnsName, DClass.IN,
									rec.getTtl(), rec.getPeriority(),target));
									
								} catch (Exception e) {
									
									log.error("",e);
								}
								
							}
						}
						
						if(eDns.getCnameRecords() != null) {
							
							for(Iterator<ECn> dnsFieldIt = eDns.getCnameRecords().iterator(); dnsFieldIt.hasNext();) {
								
								ECn rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
											.append(".").append(baseNameForDomain).toString());
									
									target = null;
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new CNAMERecord(dnsName, DClass.IN,
									rec.getTtl(),target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getaRecords() != null) {
							
							for(Iterator<EA> dnsFieldIt = eDns.getaRecords().iterator(); dnsFieldIt.hasNext();) {
								
								EA rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString()); 
									
									simpleDnsRecords.add(new ARecord(dnsName, DClass.IN,
									rec.getTtl(),InetAddress.getByName(rec.getTarget())));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getAaaaRecords() != null) {
							
							for(Iterator<EAa> dnsFieldIt = eDns.getAaaaRecords().iterator(); dnsFieldIt.hasNext();) {
								
								EAa rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString()); 
									
									simpleDnsRecords.add(new AAAARecord(dnsName, DClass.IN,
											rec.getTtl(),InetAddress.getByName(rec.getTarget())));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getPtrRecords() != null) {
							
							for(Iterator<EPtr> dnsFieldIt = eDns.getPtrRecords().iterator(); dnsFieldIt.hasNext();) {
								
								EPtr rec = dnsFieldIt.next();
								
								try {
									
									String ptrName = DnsHelper.getPtrIP(rec.getName());
									
									if(ptrName == null) {
										
										log.warn("Wrong Ptr Name {}", rec.getName());
										continue;
									}
									
									if(!eDns.isRoot()) {
									
										List<NameRecord> nsNameRecords = LookupService.nsLookupWithDelegation(ptrName, definedNsList);
										
										if(nsNameRecords == null || nsNameRecords.size() == 0) {
											
											log.warn("PTR delegation config was wrong for PTR {} - discarding all PTR settings", ptrName);
											break;
										}
									}
									
									dnsName = Name.fromString(ptrName);
									
									//PTR Target should be always in its user's zone
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(new StringBuilder(rec.getTarget())
													.append(baseNameForDomain).toString());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									arpaDnsRecords.add(new PTRRecord(dnsName, DClass.IN,
											rec.getTtl(),target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getTxtRecords() != null) {
							
							for(Iterator<ETxt> dnsFieldIt = eDns.getTxtRecords().iterator(); dnsFieldIt.hasNext();) {
								
								ETxt rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString()); 
									
									simpleDnsRecords.add(new TXTRecord(dnsName, DClass.IN,
											rec.getTtl(),rec.getTarget()));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getSrvRecords() != null) {
							
							for(Iterator<ESrv> dnsFieldIt = eDns.getSrvRecords().iterator(); dnsFieldIt.hasNext();) {
								
								ESrv rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString());

									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new SRVRecord(dnsName, DClass.IN, rec.getTtl(), rec.getPeriority(),
											rec.getWight(), rec.getPort(), target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getNaptrRecords() != null) {
							
							for(Iterator<ENaPtr> dnsFieldIt = eDns.getNaptrRecords().iterator(); dnsFieldIt.hasNext();) {
								
								ENaPtr rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
													.append(baseNameForDomain).toString());
									
									if(rec.getReplacement() != null && !rec.getReplacement().equals("")) {
										
										if(!rec.getReplacement().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getReplacement()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getReplacement());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new NAPTRRecord(dnsName, DClass.IN, rec.getTtl(), rec.getOrder(), rec.getPreference(), rec.getFlag(),
											rec.getService(), rec.getRegex(), target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						if(eDns.getDnameRecord() != null) {
							
							EDn rec = eDns.getDnameRecord();
							
							try {
								
								dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
										baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
										.append(".").append(baseNameForDomain).toString());
								
								target = null;
								
								if(rec.getTarget() != null && !rec.getTarget().equals("")) {
									
									if(!rec.getTarget().endsWith(".")) {
										
										target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
												.append(baseNameForDomain).toString());
									}
									else {
										
										target = Name.fromString(rec.getTarget());
									}
								}
								else {
									
									target = baseNameForDomainAsName;
								}
								
								simpleDnsRecords.add(new DNAMERecord(dnsName, DClass.IN,
								rec.getTtl(),target));	
								
							} catch (Exception e) {
								
								log.error("",e);
							}
						}
						
						if(eDns.getSelfResolveRecords() != null) {
							
							for(Iterator<EDy> dnsFieldIt = eDns.getSelfResolveRecords().iterator(); dnsFieldIt.hasNext();) {
								
								EDy rec = dnsFieldIt.next();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
											.append(".").append(baseNameForDomain).toString());

									simpleDnsRecords.add(new DYRecord(dnsName, rec.getTtl(), DYRecord.RESOLVE_WITH_DNS_SERVER));

								} catch (Exception e) {
									
									log.error("",e);
								}
							}
						}
						
						int simpleDnsRecordsSize = simpleDnsRecords.size();
						
						if( simpleDnsRecordsSize > 0 ) {
							
							if(!eDns.isRoot()) {
								
								Record[] recordArrs = new Record[simpleDnsRecordsSize];

								recordArrs = simpleDnsRecords.toArray(recordArrs);
								
								Zone dnsSimpleZone = new Zone(baseNameForDomainAsName, recordArrs);
								
								tmpZones.put(baseNameForDomainAsName, dnsSimpleZone);	
							}
						}
					}	
				}
			}
			
			if(rootZonebaseNameForDomainAsName == null 
					|| rootZoneDnsRecords.size() == 0) {
				
				log.warn("Could not find root zone");
				return;
			}
			
			Record[] recordArrs = new Record[rootZoneDnsRecords.size()];

			recordArrs = rootZoneDnsRecords.toArray(recordArrs);
			
			Zone rootZone = new Zone(rootZonebaseNameForDomainAsName, recordArrs);
			
			if(rootNsRecords == null || rootNsRecords.size() == 0) {
				
				throw new Exception("No ns records was defined for root");
			}
			
			tmpZones.put(rootZonebaseNameForDomainAsName, rootZone);	

			//IF arpaDnsRecords.size > 0 then we have the rootSoa and NS fields for sure
			if( arpaDnsRecords.size() > 0 ) {
				
				try {
					
					Soa commonSoa = createCommonSoa(Config.getRootSoa());
					
					Name arpaName = Name.fromString(DnsHelper.ARPA_ZONE);
					
					arpaDnsRecords.add(new SOARecord(arpaName,
							DClass.IN,
							commonSoa.getTtl(),
							((NSRecord)rootNsRecords.get(0)).getTarget(),
							Name.fromString(commonSoa.getMail()),
							commonSoa.getSerial(),
							commonSoa.getRefresh(),
							commonSoa.getRetry(),
							commonSoa.getExpire(),
							commonSoa.getMinttl()));
					
					for(int i = 0; i < rootNsRecords.size(); i++) {
						
						NSRecord r = (NSRecord)rootNsRecords.get(i);
						
						arpaDnsRecords.add(new NSRecord(arpaName, DClass.IN, r.getTTL(), r.getTarget()));
					}
					
					recordArrs = new Record[arpaDnsRecords.size()];
					
					recordArrs = arpaDnsRecords.toArray(recordArrs);

					Zone arpaZone = new Zone(arpaName, recordArrs);
					
					tmpZones.put(arpaName, arpaZone);
					
				} catch (Exception e) {
					
					log.error("Could not create ARPA Records", e);
				}
			}
			
			zones = tmpZones;
			
		} catch (Exception e) {
			
			log.error("",e);
		}
	}

	/*public static void setupZones() {
		
		Map<String, GeoResolver> resz = Config.getZoneResz(null);
		
		try {
			
			if(resz == null) {
				
				log.error("No loaded resources found");
				return;	
			}
			
			if(Config.getRootSoa() == null) {
				
				log.error("No root SOA found");
				return;
			}
			
			ConcurrentHashMap<Name,Zone> tmpZones = 
					new ConcurrentHashMap<Name,Zone>();	
			
			List<Record> arpaDnsRecords = new ArrayList<Record>();
			
			List<Record> rootNsRecords = null;
			
			Name rootZonebaseNameForDomainAsName = null;
			
			List<Record> rootZoneDnsRecords = new ArrayList<>();

			for(Iterator<Entry<String, GeoResolver>> it = resz.entrySet().iterator();
					it.hasNext();) {
				
				Entry<String, GeoResolver> g = it.next();
				
				String zone = new StringBuilder(g.getKey()).
						append(".").append(Config.MAIN_DOMAIN_STR).
						toString();
				
				List<String> definedNsList = new ArrayList<String>();
				
				if(Config.MainDomainNameserverConfig != null) {
				
					NameserverConfig nsc = Config.MainDomainNameserverConfig;
					
					String zoneNamePrefix = g.getKey().substring(0, g.getKey().indexOf('.'));
					
					for(int i =0; i < nsc.getCount(); i++) {
						
						String definedNs = new StringBuilder(zoneNamePrefix).
								append("-").append(i+1).
								append(".").append(nsc.getName()).
								append(".").append(Config.MAIN_DOMAIN_STR).
								toString();
						
						definedNsList.add(definedNs);
						
						rootZoneDnsRecords.add(new DYRecord(Name.fromString(definedNs), nsc.getTtl(), 
								DYRecord.RESOLVE_WITH_DNS_SERVER));
					}
				}
				
				for(Iterator<MyEntry> gmIt = g.getValue().getEntries().iterator(); gmIt.hasNext();) {
					
					MyEntry gm = gmIt.next();
					
					if(gm.getFullAName() == null) {
						
						String fullAName = new StringBuilder(gm.getA()).
								append(".").append(zone).toString();
						
						gm.setFullAName(fullAName);
					}
					
					log.debug("Proceccing entry {}", gm.getFullAName());
					
					byte rm = (byte)gm.getResolveMode();
					
					if(rm == EntryResolveModes.PROTO_RESOLVE_PROXY.getValue()) {
						
						rm = (byte)EntryResolveModes.PROTO_RESOLVE.getValue();
					}
					
					rootZoneDnsRecords.add(new DYRecord(Name.fromString(gm.getFullAName()), gm.getTtl(), rm));
					
					try {
						
						if(gm.getEntryDns() != null && gm.getEntryDns().getName() != null) {

							Soa commonSoa = createCommonSoa(Config.getRootSoa());
							
							String baseNameForDomain = gm.getEntryDns().getName();
							
							log.debug("Creating zone for name {}", baseNameForDomain);
							
							Name baseNameForDomainAsName = Name.fromString(baseNameForDomain);
							
							List<Record> simpleDnsRecords = null;

							if(!gm.isRoot()) {
	
								if(Config.getApp().isCheckForNsValidity()) {

									List<NameRecord> nsNameRecords = LookupService.nsLookupWithDelegation(baseNameForDomain, definedNsList);
									
									if(nsNameRecords == null || nsNameRecords.size() == 0) {
										
										log.warn("Name Server config was wrong for domain {} - discarding dns setting", baseNameForDomain);
										continue;
									}
									
									simpleDnsRecords = new ArrayList<Record>();
									
									for(Iterator<NameRecord> nsNameRecordsIt = nsNameRecords.iterator(); 
											nsNameRecordsIt.hasNext();) {
									
										NameRecord field = nsNameRecordsIt.next();
										
										Name nameServer = field.getName();
										
										String theName = nameServer.toString();
										
										if(!nameServer.isAbsolute()) {
											
											theName = new StringBuilder(theName).append(".").toString();
											
											nameServer = Name.fromString(theName);
										}
										
										simpleDnsRecords.add(new NSRecord(baseNameForDomainAsName,
												DClass.IN,
												field.getTTL(),
												nameServer));

										//I meant this should only be answered with incoming interface address
										rootZoneDnsRecords.add(new DYRecord(nameServer, field.getTTL(), DYRecord.RESOLVE_WITH_DNS_SERVER));	
									}

									String primaryNs = nsNameRecords.get(0).getName().toString();
									
									if(!primaryNs.endsWith(".")) {
										
										primaryNs = new StringBuilder(primaryNs).append(".").toString();
									}
									
									commonSoa.setNs(primaryNs);
								}
								else {
									//In real life does not matter
									//because it should not reach here
									simpleDnsRecords = new ArrayList<Record>();
									
									for(int rr = 0; rr < fakeNameServers.size(); rr++) {
										
										Record r = new NSRecord(baseNameForDomainAsName,
												DClass.IN,
												3600,
												Name.fromString(fakeNameServers.get(rr)));
										simpleDnsRecords.add(r);
										
										//I meant this should only be answered with incoming interface address
										rootZoneDnsRecords.add(new DYRecord(Name.fromString(fakeNameServers.get(rr)), 3600, DYRecord.RESOLVE_WITH_DNS_SERVER));

									}
									
									commonSoa.setNs(fakeNameServers.get(0));
								}
							}
							else {
								
								simpleDnsRecords = rootZoneDnsRecords;
								
								rootZonebaseNameForDomainAsName = baseNameForDomainAsName;
							}
							
							if(gm.getEntryDns().getSoaRecord() != null) {
								
								Soa rec = gm.getEntryDns().getSoaRecord();
								
								simpleDnsRecords.add(new SOARecord(baseNameForDomainAsName,
										DClass.IN,
										rec.getTtl(),
										Name.fromString(rec.getNs()),
										Name.fromString(rec.getMail()),
										rec.getSerial(),
										rec.getRefresh(),
										rec.getRetry(),
										rec.getExpire(),
										rec.getMinttl()));
							}
							else {
								
								simpleDnsRecords.add(new SOARecord(baseNameForDomainAsName,
										DClass.IN,
										commonSoa.getTtl(),
										Name.fromString(commonSoa.getNs()),
										Name.fromString(commonSoa.getMail()),
										commonSoa.getSerial(),
										commonSoa.getRefresh(),
										commonSoa.getRetry(),
										commonSoa.getExpire(),
										commonSoa.getMinttl()));
							}
							
							Name dnsName = null;
							
							Name target = null;
							
							if(gm.getEntryDns().getNsRecords() != null) {
								
								if(gm.isRoot()) {
									
									rootNsRecords = new ArrayList<Record>();
								}
								
								for(Iterator<ENs> dnsFieldIt = gm.getEntryDns().getNsRecords().iterator(); dnsFieldIt.hasNext();) {
									
									ENs rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString());
										
										target = null;
										
										if(rec.getTarget() != null && !rec.getTarget().equals("")) {
											
											if(!rec.getTarget().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(rec.getTarget());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										Record r = new NSRecord(dnsName, DClass.IN, rec.getTtl(), target);
										
										if(gm.isRoot()) {
											
											rootNsRecords.add(r);
										}
										
										simpleDnsRecords.add(r);

									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getAcnameRecord() != null) {
								
								EAcn rec = gm.getEntryDns().getAcnameRecord();
								
								try {
									
									dnsName = baseNameForDomainAsName;
									
									target = null;
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new ACNAMERecord(dnsName, DClass.IN,
									rec.getTtl(),target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
							else if(gm.getEntryDns().getBcnameRecord() != null) {
								
								EBcn rec = gm.getEntryDns().getBcnameRecord();
								
								try {
									
									dnsName = baseNameForDomainAsName;
									
									target = null;
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}

									simpleDnsRecords.add(new BCNAMERecord(dnsName, DClass.IN,
									rec.getTtl(),target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
							else if(gm.getEntryDns().getRcnameRecord() != null) {
								
								ERcn rec = gm.getEntryDns().getRcnameRecord();
								
								try {
									
									dnsName = baseNameForDomainAsName;
									
									target = null;
									
									String fullAname = gm.getFullAName();
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										String targetName = rec.getTarget();
										
										if(!targetName.endsWith(".")) {
											
											targetName = new StringBuilder(targetName).append(".")
													.append(baseNameForDomain).toString();
										}
										
										DomainHelper dh = new DomainHelper(targetName);

										if(dh.IsFullDomain()) {
											
											targetName = dh.GetPureDomain();
											fullAname = new StringBuilder(dh.getApiPart()).append(".").append(fullAname).toString();
										}
										else {
											
											fullAname = new StringBuilder(Config.getApp().getApiPrefix()).
													append("-").append(FRfra.FD_FORWARD).append(".").
													append(fullAname).toString();
										}
										
										target = Name.fromString(targetName);
									}
									else {
										
										fullAname = new StringBuilder(Config.getApp().getApiPrefix()).
												append("-").append(FRfra.FD_FORWARD).append(".").
												append(fullAname).toString();
										target = baseNameForDomainAsName;
									}

									simpleDnsRecords.add(new RCNAMERecord(dnsName, DClass.IN,
									rec.getTtl(),target, Name.fromString(fullAname)));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
							
							if(gm.getEntryDns().getMxRecords() != null) {
								
								for(Iterator<EMx> dnsFieldIt = gm.getEntryDns().getMxRecords().iterator(); dnsFieldIt.hasNext();) {
									
									EMx rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString());
										
										if(rec.getTarget() != null && !rec.getTarget().equals("")) {
											
											if(!rec.getTarget().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(rec.getTarget());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										simpleDnsRecords.add(new MXRecord(dnsName, DClass.IN,
										rec.getTtl(), rec.getPeriority(),target));
										
									} catch (Exception e) {
										
										log.error("",e);
									}
									
								}
							}
							
							if(gm.getEntryDns().getCnameRecords() != null) {
								
								for(Iterator<ECn> dnsFieldIt = gm.getEntryDns().getCnameRecords().iterator(); dnsFieldIt.hasNext();) {
									
									ECn rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
												.append(".").append(baseNameForDomain).toString());
										
										target = null;
										
										if(rec.getTarget() != null && !rec.getTarget().equals("")) {
											
											if(!rec.getTarget().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(rec.getTarget());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										simpleDnsRecords.add(new CNAMERecord(dnsName, DClass.IN,
										rec.getTtl(),target));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getaRecords() != null) {
								
								for(Iterator<EA> dnsFieldIt = gm.getEntryDns().getaRecords().iterator(); dnsFieldIt.hasNext();) {
									
									EA rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString()); 
										
										simpleDnsRecords.add(new ARecord(dnsName, DClass.IN,
										rec.getTtl(),InetAddress.getByName(rec.getTarget())));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getAaaaRecords() != null) {
								
								for(Iterator<EAa> dnsFieldIt = gm.getEntryDns().getAaaaRecords().iterator(); dnsFieldIt.hasNext();) {
									
									EAa rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString()); 
										
										simpleDnsRecords.add(new AAAARecord(dnsName, DClass.IN,
												rec.getTtl(),InetAddress.getByName(rec.getTarget())));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getPtrRecords() != null) {
								
								for(Iterator<EPtr> dnsFieldIt = gm.getEntryDns().getPtrRecords().iterator(); dnsFieldIt.hasNext();) {
									
									EPtr rec = dnsFieldIt.next();
									
									try {
										
										String ptrName = DnsHelper.getPtrIP(rec.getName());
										
										if(ptrName == null) {
											
											log.warn("Wrong Ptr Name {}", rec.getName());
											continue;
										}
										
										if(!g.getValue().isRoot()) {
										
											List<NameRecord> nsNameRecords = LookupService.nsLookupWithDelegation(ptrName, definedNsList);
											
											if(nsNameRecords == null || nsNameRecords.size() == 0) {
												
												log.warn("PTR delegation config was wrong for PTR {} - discarding all PTR settings", ptrName);
												break;
											}
										}
										
										dnsName = Name.fromString(ptrName);
										
										//PTR Target should be always in its user's zone
										if(rec.getTarget() != null && !rec.getTarget().equals("")) {
											
											if(!rec.getTarget().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(new StringBuilder(rec.getTarget())
														.append(baseNameForDomain).toString());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										arpaDnsRecords.add(new PTRRecord(dnsName, DClass.IN,
												rec.getTtl(),target));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getTxtRecords() != null) {
								
								for(Iterator<ETxt> dnsFieldIt = gm.getEntryDns().getTxtRecords().iterator(); dnsFieldIt.hasNext();) {
									
									ETxt rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString()); 
										
										simpleDnsRecords.add(new TXTRecord(dnsName, DClass.IN,
												rec.getTtl(),rec.getTarget()));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getSrvRecords() != null) {
								
								for(Iterator<ESrv> dnsFieldIt = gm.getEntryDns().getSrvRecords().iterator(); dnsFieldIt.hasNext();) {
									
									ESrv rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString());

										if(rec.getTarget() != null && !rec.getTarget().equals("")) {
											
											if(!rec.getTarget().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(rec.getTarget());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										simpleDnsRecords.add(new SRVRecord(dnsName, DClass.IN, rec.getTtl(), rec.getPeriority(),
												rec.getWight(), rec.getPort(), target));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getNaptrRecords() != null) {
								
								for(Iterator<ENaPtr> dnsFieldIt = gm.getEntryDns().getNaptrRecords().iterator(); dnsFieldIt.hasNext();) {
									
									ENaPtr rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName()).append(".")
														.append(baseNameForDomain).toString());
										
										if(rec.getReplacement() != null && !rec.getReplacement().equals("")) {
											
											if(!rec.getReplacement().endsWith(".")) {
												
												target = Name.fromString(new StringBuilder(rec.getReplacement()).append(".")
														.append(baseNameForDomain).toString());
											}
											else {
												
												target = Name.fromString(rec.getReplacement());
											}
										}
										else {
											
											target = baseNameForDomainAsName;
										}
										
										simpleDnsRecords.add(new NAPTRRecord(dnsName, DClass.IN, rec.getTtl(), rec.getOrder(), rec.getPreference(), rec.getFlag(),
												rec.getService(), rec.getRegex(), target));	
										
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							if(gm.getEntryDns().getDnameRecord() != null) {
								
								EDn rec = gm.getEntryDns().getDnameRecord();
								
								try {
									
									dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
											baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
											.append(".").append(baseNameForDomain).toString());
									
									target = null;
									
									if(rec.getTarget() != null && !rec.getTarget().equals("")) {
										
										if(!rec.getTarget().endsWith(".")) {
											
											target = Name.fromString(new StringBuilder(rec.getTarget()).append(".")
													.append(baseNameForDomain).toString());
										}
										else {
											
											target = Name.fromString(rec.getTarget());
										}
									}
									else {
										
										target = baseNameForDomainAsName;
									}
									
									simpleDnsRecords.add(new DNAMERecord(dnsName, DClass.IN,
									rec.getTtl(),target));	
									
								} catch (Exception e) {
									
									log.error("",e);
								}
							}
							
							if(gm.getEntryDns().getSelfResolveRecords() != null) {
								
								for(Iterator<EDy> dnsFieldIt = gm.getEntryDns().getSelfResolveRecords().iterator(); dnsFieldIt.hasNext();) {
									
									EDy rec = dnsFieldIt.next();
									
									try {
										
										dnsName = (rec.getName() == null || rec.getName().equals("")) ? 
												baseNameForDomainAsName: Name.fromString(new StringBuilder(rec.getName())
												.append(".").append(baseNameForDomain).toString());

										simpleDnsRecords.add(new DYRecord(dnsName, rec.getTtl(), DYRecord.RESOLVE_WITH_DNS_SERVER));
	
									} catch (Exception e) {
										
										log.error("",e);
									}
								}
							}
							
							int simpleDnsRecordsSize = simpleDnsRecords.size();
							
							if( simpleDnsRecordsSize > 0 ) {
								
								if(!gm.isRoot()) {
									
									Record[] recordArrs = new Record[simpleDnsRecordsSize];

									recordArrs = simpleDnsRecords.toArray(recordArrs);
									
									Zone dnsSimpleZone = new Zone(baseNameForDomainAsName, recordArrs);
									
									tmpZones.put(baseNameForDomainAsName, dnsSimpleZone);	
								}
							}	
						}
					}
					catch(Exception e) {
						
						log.error("",e);
					}
				}
			}
			
			if(rootZonebaseNameForDomainAsName == null 
					|| rootZoneDnsRecords.size() == 0) {
				
				log.warn("Could not find root zone");
				return;
			}
			
			Record[] recordArrs = new Record[rootZoneDnsRecords.size()];

			recordArrs = rootZoneDnsRecords.toArray(recordArrs);
			
			Zone rootZone = new Zone(rootZonebaseNameForDomainAsName, recordArrs);
			
			if(rootNsRecords == null || rootNsRecords.size() == 0) {
				
				throw new Exception("No ns records was defined for root");
			}
			
			tmpZones.put(rootZonebaseNameForDomainAsName, rootZone);	

			//IF arpaDnsRecords.size > 0 then we have the rootSoa and NS fields for sure
			if( arpaDnsRecords.size() > 0 ) {
				
				try {
					
					Soa commonSoa = createCommonSoa(Config.getRootSoa());
					
					Name arpaName = Name.fromString(DnsHelper.ARPA_ZONE);
					
					arpaDnsRecords.add(new SOARecord(arpaName,
							DClass.IN,
							commonSoa.getTtl(),
							((NSRecord)rootNsRecords.get(0)).getTarget(),
							Name.fromString(commonSoa.getMail()),
							commonSoa.getSerial(),
							commonSoa.getRefresh(),
							commonSoa.getRetry(),
							commonSoa.getExpire(),
							commonSoa.getMinttl()));
					
					for(int i = 0; i < rootNsRecords.size(); i++) {
						
						NSRecord r = (NSRecord)rootNsRecords.get(i);
						
						arpaDnsRecords.add(new NSRecord(arpaName, DClass.IN, r.getTTL(), r.getTarget()));
					}
					
					recordArrs = new Record[arpaDnsRecords.size()];
					
					recordArrs = arpaDnsRecords.toArray(recordArrs);

					Zone arpaZone = new Zone(arpaName, recordArrs);
					
					tmpZones.put(arpaName, arpaZone);
					
				} catch (Exception e) {
					
					log.error("Could not create ARPA Records", e);
				}
			}
			
			zones = tmpZones;
			
		} catch (Exception e) {
			
			log.error("",e);
		}
	}*/
	
	public static Zone findZone(Name name) {
		
		if(zones == null) {
			
			return null;
		}
		
		Zone foundzone = zones.get(name);
		
		if (foundzone != null) {

			return foundzone;	
		}
		
		int labels = name.labels();
		
		for (int i = 1; i < labels; i++) {
			
			Name tname = new Name(name, i);
			
			foundzone = zones.get(tname);
			
			if (foundzone != null)
				
				return foundzone;
		}
		
		return null;
	}
	
	private static Soa createCommonSoa(Soa rootSoa) {
		
		Soa commonSoa = new Soa();
		commonSoa.setExpire(rootSoa.getExpire());//4 weeks
		
		commonSoa.setNs(rootSoa.getNs());
		commonSoa.setMail(rootSoa.getMail());
		commonSoa.setMinttl(rootSoa.getMinttl());
		commonSoa.setRefresh(rootSoa.getRefresh());
		commonSoa.setRetry(rootSoa.getRetry());
		
		commonSoa.setSerial(rootSoa.getSerial());
		commonSoa.setTtl(rootSoa.getTtl());
		
		return commonSoa;
	}
	
	@SuppressWarnings("unused")
	private static Soa createSoa() {
		
		Soa commonSoa = new Soa();
		commonSoa.setExpire(2419200);//4 weeks
		String soaMail = null;
		
		if(Config.getApp().getSoaResponsible().endsWith(".")) {
		
			soaMail = new StringBuilder(Config.getApp().getSoaResponsible())
			.append(Config.MAIN_DOMAIN_STR).toString();
		}
		else {
			
			soaMail = new StringBuilder(Config.getApp().getSoaResponsible())
			.append(".").append(Config.MAIN_DOMAIN_STR).toString();
		}
		
		//XXX what about NS?
		commonSoa.setMail(soaMail);
		commonSoa.setMinttl(3600);
		commonSoa.setRefresh(10000);
		commonSoa.setRetry(2400);
		Calendar calendar = Calendar.getInstance();
		Calendar serial = Calendar.getInstance();
		serial.clear();
		serial.set(Calendar.MONTH, calendar.get(Calendar.MONTH));
		serial.set(Calendar.YEAR, calendar.get(Calendar.YEAR));
		serial.set(Calendar.HOUR, calendar.get(Calendar.HOUR));
		
		commonSoa.setSerial(serial.getTimeInMillis() & 0x00000000ffffffffL);
		commonSoa.setTtl(21599);
		
		return commonSoa;
	}
}
