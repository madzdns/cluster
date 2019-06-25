package com.github.madzdns.server.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ACNAMERecord;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.BCNAMERecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.DYRecord;
import org.xbill.DNS.ExtendedFlags;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.RCNAMERecord;
import org.xbill.DNS.RPRecord;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.Record;
import org.xbill.DNS.Header;
import org.xbill.DNS.Section;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;
import org.xbill.DNS.Zone;

import com.github.madzdns.server.core.api.ResolveResult;
import com.github.madzdns.server.core.api.Resolver;
import com.github.madzdns.server.core.utils.DomainHelper;

public class DnsResolver {
	
	private Logger log = LoggerFactory.getLogger(DnsResolver.class);
	
	static final int FLAG_DNSSECOK = 1;
	static final int FLAG_SIGONLY = 2;
	
	private Message query;
	
	private Record question;
	
	private Message error = null;
	
	private boolean bad_query = false;
	
	private Name name = null;
	
	private int type = 0;
	
	private int dclass = 0;
	
	private int flags = 0;
	
	private Set<Name> loopDetectionNames = new HashSet<Name>();
	
	public DnsResolver(final byte[] message) throws IOException {
		
		query = new Message(message);
	}
	
	public void processQuery(){
		
		final Header header = query.getHeader();
		question = query.getQuestion();	
		name = question.getName();
		type = question.getType();
		dclass = question.getDClass();
		
		if(checkError(header)) {
			
			return;
		}
			
		final OPTRecord queryOPT = query.getOPT();
		
		if (queryOPT != null && (queryOPT.getFlags() & ExtendedFlags.DO) != 0) {
			
			flags = FLAG_DNSSECOK;	
		}	
	}
	
	private boolean checkError(final Header header) {
		
		if (header.getFlag(Flags.QR))
			
			return bad_query = true;
		
		if (header.getRcode() != Rcode.NOERROR)
			
			return errorMessage(query, Rcode.FORMERR);
		
		if (header.getOpcode() != Opcode.QUERY)
			
			return errorMessage(query, Rcode.NOTIMP);
		
		if (!Type.isRR(type) && type != Type.ANY)
			
			return errorMessage(query, Rcode.NOTIMP);
		
		return false;
	}
	
	private boolean errorMessage(final Message query, final int rcode) {
		
		bad_query = true;
		
		buildErrorMessage(query.getHeader(), rcode,
					 query.getQuestion());
		
		return bad_query;
	}
	
	private void buildErrorMessage(final Header header, final int rcode, final Record question) {
		
		error = new Message();
		error.setHeader(header);
		for (int i = 0; i < 4; i++)
			error.removeAllRecords(i);
		error.addRecord(question, Section.QUESTION);
		header.setRcode(rcode);
	}
	
	public byte[] setupResponse(final String incomming_ip, final String client_ip) {
		
		if(bad_query && error == null)
			
			return null;
		
		if(bad_query)
			
			return error.toWire();
		
		final Message response = new Message(query.getHeader().getID());
		
		response.getHeader().setFlag(Flags.QR);
		
		if (query.getHeader().getFlag(Flags.RD))
			
			response.getHeader().setFlag(Flags.RD);
		
		response.addRecord(question,Section.QUESTION);
		
		//changed iterate from 0 to 0
		makeAnswer(response,name,type,dclass,0,flags,incomming_ip,client_ip, Section.ANSWER);
		
		return response.toWire();
	}
	
	private byte
	makeAnswer(final Message response, final Name fullname, final int type, final int dclass,
		  final int iterations, final int flags, final String incomming_ip, final String client_ip, int section) {
		
		return makeAnswer(response, fullname, type, dclass,iterations, flags, incomming_ip, client_ip, section, null);	
	}
	
	private byte
	makeAnswer(final Message response, final Name fullname, final int type, final int dclass,
		  final int iterations, final int flags, final String incomming_ip, final String client_ip,
		  int section, Name originalFullnameTrace) {
		
		if(loopDetectionNames.contains(fullname)) {
			
			log.warn("A Naming Loop is detected for name {}, Refusing to iterate more.", fullname);
			
			if(section == Section.ANSWER) {
				
				response.getHeader().setRcode(Rcode.NOTZONE);
			}
			return Rcode.REFUSED;
		}
		
		Zone zone = null;
		
		SetResponse sr = null;
		
		byte rcode = Rcode.NOERROR;
		
		ARecord[] A = null;
		
		//--EntryDnsConfig entryConfig = null;
		
		log.debug("local:{} remote:{} Name: {}, type: {}, dclass {}, fullnameForACNAME {}",incomming_ip,client_ip,fullname.toString(),Type.string(type),DClass.string(dclass), originalFullnameTrace);
		
		if(Type.A != type) {
			
			zone = ZoneManager.findZone(fullname);
			
			if (zone != null)
				
				sr = zone.findRecords(fullname,type);
			
			if(zone == null || sr == null) {
				
				log.debug("Not a zone result for {}", fullname);
				
				if(section == Section.ANSWER) {
					
					response.getHeader().setRcode(Rcode.NOTZONE);
				}
				
				return Rcode.NOTZONE;
			}
		}
		else {
			
			Name name = null;
			
			byte serviceProto = 0;
			short servicePort = 0;
			
			final DomainHelper helper = new DomainHelper(fullname);
			
			if(helper.IsFullDomain()) {
				
				name = helper.GetPureDomainToName();
				
				servicePort = helper.GetSrvPort();
				
				serviceProto = helper.GetSrvProto();
				
				log.debug("full domain, servicePort={}, serviceProto ={}", servicePort, serviceProto);
			}
			else {

				name = fullname;
			}
			
			zone = ZoneManager.findZone(name);
			
			if(zone == null) {

				log.debug("zone == null, Could not find assosiated Zone for name {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
				if(section == Section.ANSWER) {
				
					response.getHeader().setRcode(Rcode.NOTZONE);
				}
				
				return Rcode.NOTZONE;
			}
			
			sr = zone.findRecords(name,type);
			
			if(sr == null) {
				
				if(section == Section.ANSWER) {
				
					response.getHeader().setRcode(Rcode.NOTZONE);
				}
				
				log.debug("SR == NULL, Could not resolve {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
				
				return Rcode.NOTZONE;
			}
			
			//--entryConfig = zone.getEntryDnsConf(name.toString());
			
			//--if(entryConfig != null ) {
			
			if(sr.isDynamicNAME()) {
				
				DYRecord dynamic = sr.getDYNAME();
				
				ResolveResult cfg = null;
				
				//--final long ttl = entryConfig.getTtl();
				
				final long ttl = dynamic.getTTL();
				
				//--if(entryConfig.isDnsResolve()) {
					
				if(dynamic.isDynamicResolve()) {
					
					try {
						
						cfg = Resolver.getBestServer(client_ip, name.toString(), serviceProto, servicePort, false);
						
						if(cfg == null ) {
							
							log.error("Could not find proper node - REQUEST FROM ({}) MODE ({}) ORIGNAL NAME <{}> NAME ({}) -",client_ip, "DNS_RESOLVE", fullname.toString(), name.toString());
							
							if(section == Section.ANSWER) {

								response.getHeader().setRcode(Rcode.NXRRSET);
							}
							
							return Rcode.NXRRSET;
						}
						
						log.debug("ANSWERING WITH DNS RESOLVE - RESOLVED NODE ({})  REQUEST SROURCE ({}) TTL ({})",
								cfg.getServers(), client_ip, ttl);
						
						String[] servers = cfg.getServers();
						
						A = new ARecord[servers.length];
						
						Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
						
						for(int c = 0; c < servers.length; c++) {
						
							A[c]= new ARecord(candidateFullName,DClass.IN,ttl,InetAddress.getByName(servers[c]));

							response.addRecord(A[c], section);
						}
						
					} catch (UnknownHostException e) {
						
						log.error("UnknownHostException - REQUEST FROM ({}) MODE (DNS_RESOLVE> ORIGNAL NAME <{}> NAME <{}> -",client_ip, fullname.toString(), name.toString(), e);
						
						if(section == Section.ANSWER) {
							
							response.getHeader().setRcode(Rcode.NOTZONE);
						}
						
						return Rcode.NOTZONE;
						
					} catch(Exception e) {
						
						log.error("EXCEPTION GETTING BEST SERVER - REQUEST FROM <{}> MODE <{}> ORIGNAL NAME <{}> NAME <{}> -",client_ip, "PROTO_RESOLVE", fullname.toString(), name.toString(), e);
						
						if(section == Section.ANSWER) {
							
							response.getHeader().setRcode(Rcode.NXRRSET);
						}
						
						return Rcode.NXRRSET;
					}
					
					//--sr = zone.findRecords(name,type);
				}
				//--else if(entryConfig.isProtoResolved()) {
				else if(dynamic.isResolveWithDnsServer()) {
					
					try {
						
						Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
						
						A = new ARecord[]{ new ARecord(candidateFullName, DClass.IN,ttl,InetAddress.getByName(incomming_ip))};
						
						response.addRecord(A[0], section);
						
						log.debug("PROTO resolving using {} for name {}", incomming_ip, name.toString());
						
					} catch (UnknownHostException e) {
						
						log.error("EXCEPTION CREATING SERVER - INCOMMING IP ({}) REQUEST FROM ({}) MODE (PROTO_RESOLVE) ORIGNAL NAME ({}) NAME ({}) -",incomming_ip, client_ip, fullname.toString(), name.toString(), e);
						
						if(section == Section.ANSWER) {
						
							response.getHeader().setRcode(Rcode.NXRRSET);
						}
						
						return Rcode.NXRRSET;
					}
					
					//--sr = zone.findRecords(name,type);
				}
				/*--else {
					
					log.debug("Its only HA DNS");
					
					entryConfig = null; //To make it non dynamic
					
					sr = zone.findRecords(name,type);
				}*/
			}
			/*--else { //see what we can do!

				log.debug("Could not find entry config for {}, trying our best ...", fullname);
				sr = zone.findRecords(fullname,type);
			}*/
		}

		//--if(entryConfig != null && A != null) {
		
		if(A != null) {
			
			if(section == Section.ANSWER) {
				
				if(originalFullnameTrace != null) {
					
					addRRset(originalFullnameTrace, response, zone.getNS(), Section.AUTHORITY, flags);
				}
				else {
					
					addRRset(fullname, response, zone.getNS(), Section.AUTHORITY, flags);
				}
			}

			return rcode;
		}

		/*--if(sr == null) {
			
			if(section == Section.ANSWER) {
			
				response.getHeader().setRcode(Rcode.NOTZONE);
			}
			
			log.debug("SR == NULL, Could not resolve {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
			
			return Rcode.NOTZONE;
		}*/
		
		if (sr.isNXDOMAIN()) {
				
			if (zone != null) {
				
				if(section == Section.ANSWER) {
					
					Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
					
					boolean result = tryDelegationRecord(zone, response, candidateFullName, type, dclass, iterations, flags, incomming_ip, client_ip, section);

					if(result) {
						
						log.debug("NXDOMAIN DELEGATION Record, Could not resolve {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
						
						if(type == Type.CNAME && originalFullnameTrace != null) {
							
							return Rcode.NXDOMAIN;
						}
						
						return rcode;
					}
					else {
						
						response.addRecord(zone.getSOA(),Section.AUTHORITY);	
						
						if (iterations == 0) {
							
							response.getHeader().setFlag(Flags.AA);	
						}
					}
				}
			}
			
			if(section == Section.ANSWER) {
				
				response.getHeader().setRcode(Rcode.NXDOMAIN);		
			}
			
			log.debug("SR == NXDOMAIN, Could not resolve {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
			
			rcode = Rcode.NXDOMAIN;	
		}
		else if (sr.isNXRRSET()) {
			
			if(zone != null && section == Section.ANSWER) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				boolean result = tryDelegationRecord(zone, response, candidateFullName, type, dclass, iterations, flags, incomming_ip, client_ip, section);
				
				if(result) {
					
					log.debug("NXRRSET DELEGATION Record, Could not resolve {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
					
					if(type == Type.CNAME && originalFullnameTrace != null) {
						
						return Rcode.NXRRSET;
					}
					
					return rcode;
				}
				
				response.addRecord(zone.getSOA(),Section.AUTHORITY);
				
				if (iterations == 0)
					
					response.getHeader().setFlag(Flags.AA);
			}
			
			if(section == Section.ANSWER) {
				
				response.getHeader().setRcode(Rcode.NXRRSET);		
			}
			
			rcode = Rcode.NXRRSET;
			
			log.debug("SR == NXRRSET for {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
		}
		else if (sr.isSuccessful()) {
			
			RRset [] rrsets = sr.answers();
			
			if(Type.CNAME == type) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				Name target = ((CNAMERecord)rrsets[0].first()).getTarget();
				
				log.debug("Result for CNAME {} was succesful trying to go deeper for {}", fullname, target);
				
				byte returnedCode = makeAnswer(response, target, type, dclass, iterations, flags, incomming_ip, client_ip, section, candidateFullName);	

				if(returnedCode != Rcode.NOERROR) {
					
					log.debug("Deeper result for {} faild. Adding current to response", target);
					
					CNAMERecord crecord = null;
					RRset rr = null;
					
					for (int i = 0; i < rrsets.length; i++) {
						
						crecord = ((CNAMERecord)rrsets[i].first());
						rr = new RRset(new CNAMERecord(candidateFullName, crecord.getDClass(), crecord.getTTL(), crecord.getTarget()));
						addRRset(candidateFullName, response, rr,
								section,flags);
					}
				}
			}
			else {
			
				for (int i = 0; i < rrsets.length; i++) {
					
					addRRset(fullname, response, rrsets[i],
							section,flags);
				}
			}
			
			if(section == Section.ANSWER) {
				
				response.getHeader().setRcode(Rcode.NOERROR);
			}
			
			log.debug("SR was successful for {}, fullnameForACNAME {}", fullname, originalFullnameTrace);
			
			if (zone != null && section == Section.ANSWER) {
				
				addNS(response,zone,flags);
				
				if (iterations == 0)
					
					response.getHeader().setFlag(Flags.AA);
			}
		}
		else if(originalFullnameTrace != null 
				&& type != Type.CNAME) {
			
			log.error("BAD query. How come a query for ACNAME has not been handled yet?");
			return Rcode.NOTZONE;
		}
		else if(sr.isCNAME()) {
			
			CNAMERecord record = sr.getCNAME();
			
			log.debug("SR was CNAME {} for {}", record.getTarget(), fullname);
				
			addSimpleRecord(response, record, flags, section, fullname);
			
			loopDetectionNames.add(fullname);
			
			return makeAnswer(response, record.getTarget(), type, dclass, iterations, flags, incomming_ip, client_ip, section);
			
		}
		else if(sr.isDNAME()) {
			
			DNAMERecord record = sr.getDNAME();
			
			log.debug("SR was DNAME {} for {}", record.getTarget(), fullname);

			addSimpleRecord(response, record, flags, section, fullname);
			
			loopDetectionNames.add(fullname);
			
			return makeAnswer(response, record.getTarget(), type, dclass, iterations, flags, incomming_ip, client_ip, section);
		}
		else if(sr.isACNAME()) {
			
			ACNAMERecord record = sr.getACNAME();
			
			if(Type.CNAME == type) {
				
				log.debug("SR was ACNAME {} for CNAME request of {}", record.getTarget(), fullname);
				
				/*Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new CNAMERecord(candidateFullName, DClass.IN, record.getTTL(),
						record.getTarget()), section);*/
				
				/*I changed my mind, If request is for CNAME we respond with norrset because if
				 * some one has set ACNAME he does not want CNAME for root domain */
				if(section == Section.ANSWER) {
					
					response.getHeader().setRcode(Rcode.NXRRSET);		
				}
				
				rcode = Rcode.NXRRSET;
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
				
			}
			else if(Type.RP == type) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new RPRecord(candidateFullName, DClass.IN, record.getTTL(), record.getTarget(), record.getTarget()), section);
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response, zone, flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
			else if(Type.TXT == type) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new TXTRecord(candidateFullName, DClass.IN, record.getTTL(),
						record.getTarget().toString()), section);
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
			else {
				
				log.debug("SR was ACNAME {} for {}", record.getTarget(), fullname);
				
				//We don't add ACNAME to records
				
				loopDetectionNames.add(fullname);
				
				return makeAnswer(response, record.getTarget(), type, dclass, iterations, flags, incomming_ip, client_ip, section, fullname);	
			}
		}
		else if(sr.isBCNAME()) {
			
			BCNAMERecord record = sr.getBCNAME();
			
			if(Type.A == type) {
			
				log.debug("SR was BCNAME {} for {}", record.getTarget(), fullname);
				
				Record[] rs = new Lookup(record.getTarget(), Type.A, DClass.IN).run();
				
				if(rs != null) {
					
					for(int i=0; i< rs.length; i++) {
						
						ARecord a = (ARecord) rs[i];
						
						response.addRecord(new ARecord(fullname, DClass.IN, a.getTTL(),
								a.getAddress()), section);
					}	
				}
				else {
					
					if(section == Section.ANSWER) {
					
						response.getHeader().setRcode(Rcode.REFUSED);
					}
					
					return Rcode.REFUSED;
				}
			}
			else if(Type.CNAME == type) {
				
				log.debug("SR was BCNAME {} requesting CNAME for {}", record.getTarget(), fullname);
				
				/*Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new CNAMERecord(candidateFullName, DClass.IN, record.getTTL(),
						record.getTarget()), section);*/
				
				/*I changed my mind, If request is for CNAME we respond with norrset because if
				 * some one has set ACNAME he does not want CNAME for root domain */
				
				if(section == Section.ANSWER) {
					
					response.getHeader().setRcode(Rcode.NXRRSET);		
				}
				
				rcode = Rcode.NXRRSET;
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
		}
		else if(sr.isRCNAME()) {

			RCNAMERecord record = sr.getRCNAME();
			
			if(Type.A == type) {
				
				log.debug("SR was RCNAME {} for {}", record.getTarget(), fullname);
				
				try {
					
					Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
					
					Record[] rs = new ARecord[]{ new ARecord(candidateFullName, DClass.IN, record.getTTL(), InetAddress.getByName(incomming_ip))};
					
					response.addRecord(rs[0], section);
					
				} catch (UnknownHostException e) {
					
					log.error("EXCEPTION CREATING SERVER - INCOMMING IP ({}) REQUEST FROM ({}) MODE (PROTO_RESOLVE) ORIGNAL NAME ({}) NAME ({}) -",incomming_ip, client_ip, fullname.toString(), name.toString(), e);
					
					if(section == Section.ANSWER) {
					
						response.getHeader().setRcode(Rcode.NXRRSET);
					}
					
					return Rcode.NXRRSET;
				}
			}
			else if(Type.CNAME == type) {
				
				log.debug("SR was RCNAME {} for CNAME request of {}", record.getTarget(), fullname);

				if(section == Section.ANSWER) {
					
					response.getHeader().setRcode(Rcode.NXRRSET);		
				}
				
				rcode = Rcode.NXRRSET;
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
			else if(Type.RP == type) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new RPRecord(candidateFullName, DClass.IN, record.getTTL(), record.getTarget(),
						record.getConfigName()), section);
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
			else if(Type.TXT == type) {
				
				Name candidateFullName = originalFullnameTrace == null? fullname: originalFullnameTrace;
				
				response.addRecord(new TXTRecord(candidateFullName, DClass.IN, record.getTTL(),
						record.getTarget().toString()), section);
				
				if (zone != null && section == Section.ANSWER) {
					
					addNS(response,zone,flags);
					
					if (iterations == 0)
						
						response.getHeader().setFlag(Flags.AA);
				}
			}
		}
		
		return rcode;
	}
	
	private void
	addRRset(final Name name, final Message response, final RRset rrset, final int section, final int flags) {
		
		for (int s = 1; s <= section; s++) {
			
			if (response.findRRset(name, rrset.getType(), s)) {

				return;
			}
		}
		
		if ((flags & FLAG_SIGONLY) == 0) {
			
			@SuppressWarnings("unchecked")
			Iterator<Record> it = rrset.rrs();
			
			while (it.hasNext()) {
				
				Record r = (Record) it.next();
				
				if (r.getName().isWild() && !name.isWild())
					
					r = r.withName(name);
				
				response.addRecord(r, section);
			}
		}
		
		if ((flags & (FLAG_SIGONLY | FLAG_DNSSECOK)) != 0) {
			
			@SuppressWarnings("unchecked")
			Iterator<Record> it = rrset.sigs();
			
			while (it.hasNext()) {
				
				Record r = (Record) it.next();
				
				if (r.getName().isWild() && !name.isWild())
					
					r = r.withName(name);
				
				response.addRecord(r, section);
			}
		}
	}
	
	private final void
	addNS(final Message response, final Zone zone, final int flags) {
		
		RRset nsRecords = zone.getNS();
		
		addRRset(nsRecords.getName(), response, nsRecords,
			 Section.AUTHORITY, flags);
	}
	
	private void 
	addSimpleRecord(final Message response, Record record, final int flags, final int section, final Name name) {
		
		if ((flags & FLAG_SIGONLY) == 0) {
			
			if (record.getName().isWild() && !name.isWild())
				
				record = record.withName(name);
		
			response.addRecord(record, section);
		}
	}
	
	private boolean
	tryDelegationRecord(final Zone zone, final Message response, final Name fullname, final int type, final int dclass,
			  final int iterations, final int flags, final String incomming_ip, final String client_ip, int section) {
		
		SetResponse sr = zone.findRecords(fullname,Type.NS);

		if(sr != null && sr.isSuccessful()) {
			
			RRset [] rrsets = sr.answers();
			
			for (int i = 0; i < rrsets.length; i++) {
				
				addRRset(fullname, response, rrsets[i],
						 Section.AUTHORITY,flags);
				
				/*if(type != Type.A && type != Type.AAAA) {
					
					continue;
				}*/
				
				@SuppressWarnings("unchecked")
				Iterator<Record> it = rrsets[0].rrs();
				
				while (it.hasNext()) {
					
					NSRecord r = (NSRecord) it.next();
					
					loopDetectionNames.add(fullname);

					makeAnswer(response, r.getTarget(), Type.A, dclass, iterations, flags, incomming_ip, client_ip, Section.ADDITIONAL);
					
					makeAnswer(response, r.getTarget(), Type.AAAA, dclass, iterations, flags, incomming_ip, client_ip, Section.ADDITIONAL);
				}
			}
			
			return true;
		}
		
		return false;
	}
}
