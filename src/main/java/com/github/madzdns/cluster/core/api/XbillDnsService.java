package com.github.madzdns.cluster.core.api;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.CNAMERecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.DNAMERecord;
import org.xbill.DNS.ExtendedResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NSRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.RPRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import org.xbill.DNS.URIRecord;

import java.lang.StringBuilder;


public class XbillDnsService {

	public static class NameRecord {
		
		private Name name;
		
		private Name secondName;
		
		private long ttl;
		
		private NameRecord(Name name, long ttl) {

			this.name = name;
			this.ttl = ttl;
		}
		
		private NameRecord(String name, long ttl) throws TextParseException {

			this.name = Name.fromString(name);
			this.ttl = ttl;
		}
		
		private NameRecord(Name name, Name secondName, long ttl) {

			this.name = name;
			this.secondName = secondName;
			this.ttl = ttl;
		}

		public Name getName() {
			
			return name;
		}
		
		public long getTTL() {
			
			return ttl;
		}
		
		@Override
		public String toString() {
			
			return new StringBuilder(name.toString()).append("/").append(ttl).toString();
		}

		public Name getSecondName() {
			
			return secondName;
		}
	}

	private static Logger log = LoggerFactory.getLogger(XbillDnsService.class);

	public static List<NameRecord> getCNAMES(String hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.CNAME, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> cNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			CNAMERecord c = (CNAMERecord) rs[i];
			
			cNames.add(new NameRecord(c.getTarget(), c.getTTL()));
		}
		
		return cNames;
	}
	
	
	public static List<NameRecord> getCNAMES(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.CNAME, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> cNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			CNAMERecord c = (CNAMERecord) rs[i];
			cNames.add(new NameRecord(c.getTarget(), c.getTTL()));
		}
		
		return cNames;
	}
	
	public static List<NameRecord> getDNAMES(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.DNAME, DClass.IN).run();
		
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> dNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			DNAMERecord c = (DNAMERecord) rs[i];
			dNames.add(new NameRecord(c.getTarget(), c.getTTL()));
		}
		
		return dNames;
	}
	
	
	public static List<String> getIPAddresses(String hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.A, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<String> ips = new ArrayList<String>();
		
		for(int i=0; i< rs.length; i++) {
			
			ARecord a = (ARecord) rs[i];
			ips.add(a.getAddress().getHostAddress());
		}
		
		return ips;
	}
	
	public static List<NameRecord> getNSNames(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.NS, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> nsNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {

			NSRecord c = (NSRecord) rs[i];
			nsNames.add(new NameRecord(c.getTarget(), c.getTTL()));
		}
		
		return nsNames;
	}
	
	public static List<NameRecord> getNSwithDelegateNames(Name hostName, String[] rootServers) throws TextParseException, UnknownHostException {

		Lookup look = new Lookup(hostName, Type.NS, DClass.IN);
		
		look.setResolver(new ExtendedResolver(rootServers));
		
		Record[] rs = look.run();
		
		if(rs == null) {
			
			log.error("Looking up to {} faild", hostName);
			return null;
		}
		
		List<NameRecord> nsNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			NSRecord c = (NSRecord) rs[i];
			nsNames.add(new NameRecord(c.getTarget(), c.getTTL()));
		}
		
		return nsNames;
	}
	
	public static List<NameRecord> getTXTS(String hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.TXT, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> txtNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			TXTRecord c = (TXTRecord) rs[i];
			
			@SuppressWarnings("unchecked")
			List<String> target = c.getStrings();
			
			if(target == null || target.size() == 0) {
				
				continue;
			}
			
			try {
			
				txtNames.add(new NameRecord(target.get(0), c.getTTL()));
				
			} catch (TextParseException e) {

			}
		}
		
		return txtNames;
	}
	
	
	public static List<NameRecord> getTXTS(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.CNAME, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> txtNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			TXTRecord c = (TXTRecord) rs[i];
			
			@SuppressWarnings("unchecked")
			List<String> target = c.getStrings();
			
			if(target == null || target.size() == 0) {
				
				continue;
			}
			
			try {
				
				txtNames.add(new NameRecord(target.get(0), c.getTTL()));
				
			} catch (TextParseException e) {

			}
		}
		
		return txtNames;
	}
	
	public static List<NameRecord> getURIs(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.URI, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> uriNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			URIRecord c = (URIRecord) rs[i];

			String target = c.getTarget();
			
			if(target == null || target == "") {
				
				continue;
			}
			
			try {
				
				uriNames.add(new NameRecord(target, c.getTTL()));
				
			} catch (TextParseException e) {

			}
		}
		
		return uriNames;
	}
	
	
	public static List<NameRecord> getRPs(Name hostName) throws TextParseException {
		
		Record[] rs = new Lookup(hostName, Type.RP, DClass.IN).run();
		
		if(rs == null) {
			
			return null;
		}
		
		List<NameRecord> rpNames = new ArrayList<NameRecord>();
		
		for(int i=0; i< rs.length; i++) {
			
			RPRecord c = (RPRecord) rs[i];

			Name target = c.getMailbox();
			Name textDomain = c.getTextDomain();
			
			if(target == null) {
				
				continue;
			}
				
			rpNames.add(new NameRecord(target, textDomain, c.getTTL()));
		}
		
		return rpNames;
	}
	
	private List<NameRecord> resolvedCNames = null;
	
	private List<String> resolvedIPs = null;
	
	public boolean checkIfCnameExists(String hostName, String smapleHostName) {
		
		try {
			
			Name nameHostName = Name.fromString(hostName);
					
			Name s = Name.fromString(smapleHostName);
			
			return checkIfCnameExists(nameHostName, s);
		}
		catch(TextParseException e) {
			
		}

		return false;
	}
	
	public boolean checkIfCnameExists(Name hostName, Name smapleHostName) {
		
		try {
			
			resolvedCNames = getCNAMES(hostName);
			
			if(resolvedCNames == null) {
				
				return false;
			}
			
			Name s = smapleHostName;
			
			if(s == null) {

				return false;
			}
			
			for(int i=0; i< resolvedCNames.size(); i++) {
				
				NameRecord nr = resolvedCNames.get(i);
				
				if(nr == null) {
					
					continue;
				}
				
				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				if(c.equals(s) || c.subdomain(s)) {
					
					return true;
				}
			}
		}
		catch(TextParseException e) {
			
		}

		return false;
	}
	
	
	private NameRecord getMatchedName(Name hostName, Name smapleHostName, List<NameRecord> resolvedNames) {
		
		try {
			
			if(resolvedNames == null) {
				
				return null;
			}
			
			Name s = smapleHostName;
			
			if(s == null) {

				return null;
			}
			
			for(int i=0; i< resolvedNames.size(); i++) {
				
				NameRecord nr = resolvedNames.get(i);
				
				if(nr == null) {
					
					continue;
				}

				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				if(c.equals(s) || c.subdomain(s)) {
					
					return nr;
				}
			}
		}
		catch(Exception e) {
			
		}

		return null;
	}
	
	public NameRecord getMatchedCName(Name hostName, Name smapleHostName) {
		
		
		try {
			
			return getMatchedName(hostName, smapleHostName, getCNAMES(hostName));
			
		} catch (TextParseException e) {
			
		}
		return null;
	}
	
	public NameRecord getMatchedDName(Name hostName, Name smapleHostName) {

		try {
			
			return getMatchedName(hostName, smapleHostName, getDNAMES(hostName));
			
		} catch (TextParseException e) {
			
		}
		return null;
	}
	
	public NameRecord getMatchedTXTName(Name hostName, Name smapleHostName) {
		
		
		try {
			
			return getMatchedName(hostName, smapleHostName, getTXTS(hostName));
			
		} catch (TextParseException e) {
			
		}
		return null;
	}
	
	public NameRecord getMatchedURIName(Name hostName, Name smapleHostName) {
		
		
		try {
			
			return getMatchedName(hostName, smapleHostName, getURIs(hostName));
			
		} catch (TextParseException e) {
			
		}
		return null;
	}
	
	public NameRecord getMatchedRPName(Name hostName, Name smapleHostName) {	
		
		try {
			
			return getMatchedName(hostName, smapleHostName, getRPs(hostName));
			
		} catch (TextParseException e) {}
		
		return null;
	}
	
	public List<NameRecord> getMatchedNSWithPrefix(Name hostName, String baseDomain, String prefix, String[] rootServers) throws UnknownHostException {
		
		try {

			List<NameRecord> resolvedNames =  getNSwithDelegateNames(hostName, rootServers);
			
			boolean found = false;
			
			if(resolvedNames == null) {
				
				return null;
			}
			
			for(int i=0; i< resolvedNames.size(); i++) {
				
				NameRecord nr = resolvedNames.get(i);
				
				if(nr == null) {
					
					continue;
				}

				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				String rn = c.toString();

				if(rn.endsWith(baseDomain)) {

					if(!rn.startsWith(prefix)) {
						
						log.error("Resolved nameserver was {} which is incorrect for prefix {}", rn, prefix );
						return null;
					}
					
					found = true;
				}
				else {
					
					log.error("Resolved nameserver was {} which is incorrect for basename {}", rn, baseDomain);
					return null;
				}
			}
			
			if(found) {
				
				return resolvedNames;
			}
			
		} catch (TextParseException e) {
		
		}
		
		return null;
	}
	
	public List<NameRecord> getMatchedNSWithPrefix(Name hostName, String baseDomain, String prefix) {
		
		try {
			
			List<NameRecord> resolvedNames =  getNSNames(hostName);
			boolean found = false;
			
			for(int i=0; i< resolvedNames.size(); i++) {
				
				NameRecord nr = resolvedNames.get(i);
				
				if(nr == null) {
					
					continue;
				}

				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				String rn = c.toString(false);
				
				if(rn.endsWith(baseDomain)) {
					
					if(!rn.startsWith(prefix)) {
						
						return null;
					}
					
					found = true;
				}
				else {
					
					return null;
				}
			}
			
			if(found) {
				
				return resolvedNames;
			}
			
		} catch (TextParseException e) {
		
		}
		
		return null;
	}
	
	
	/////////////////////
	public List<NameRecord> getMatchedNS(Name hostName, List<String> names, String[] rootServers) throws UnknownHostException {
		
		try {

			List<NameRecord> resolvedNames =  getNSwithDelegateNames(hostName, rootServers);
			
			if(resolvedNames == null) {
				
				return null;
			}
			
			for(int i=0; i< resolvedNames.size(); i++) {
				
				NameRecord nr = resolvedNames.get(i);
				
				if(nr == null) {
					
					continue;
				}

				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				String rn = c.toString();
				
				for(int j=0; j< names.size(); j++) {
					
					String name = names.get(j);
					
					if(rn.equals(name)) {
						
						return resolvedNames;
					}
					else {
						
						log.error("Resolved nameserver was {} which is incorrect for name {}", rn, name);
					}
				}
			}
			
			return null;
			
		} catch (TextParseException e) {}
		
		return null;
	}
	
	public List<NameRecord> getMatchedNS(Name hostName, List<String> names) {
		
		try {
			
			List<NameRecord> resolvedNames =  getNSNames(hostName);
			
			for(int i=0; i< resolvedNames.size(); i++) {
				
				NameRecord nr = resolvedNames.get(i);
				
				if(nr == null) {
					
					continue;
				}

				Name c = nr.getName();
				
				if(c == null) {
					
					continue;
				}
				
				String rn = c.toString(false);
				
				for(int j=0; j< names.size(); j++) {
					
					String name = names.get(j);
					
					if(rn.equals(name)) {
						
						return resolvedNames;
					}
					else {
						
						log.error("Resolved nameserver was {} which is incorrect for name {}", rn, name);
					}
				}
			}
			
			return null;
			
		} catch (TextParseException e) {}
		
		return null;
	}
	/////
	
	
	public boolean checkIfIpExists(String hostName, String smapleIp) {
		
		try {
			
			resolvedIPs = getIPAddresses(hostName);
			
			if(resolvedIPs == null) {

				return false;
			}

			return resolvedIPs.contains(smapleIp);
		}
		catch(TextParseException e) {
		
			return false;
		}
	}


	public List<NameRecord> getResolvedCNames() {
		
		return resolvedCNames;
	}


	public List<String> getResolvedIPs() {
		
		return resolvedIPs;
	}
}
