package server.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;

import server.api.XbillDnsService.NameRecord;
import server.config.Config;

public class LookupService {
	
	private static class CacheState {
		
		public LookupResult result;
		
		public long sottl;
		
		public long cacheDuration;
	}
	
	private static class RootServers {
		
		private String[] servers;
		
		private long ttl;
		
		private long ttlDuration;
		
		private boolean checkIfTTLisValid() {
			
			return new Date().getTime() - ttl < ttlDuration;
		}

		public RootServers(String[] servers, long ttl, long ttlDuration) {

			this.servers = servers;
			this.ttl = ttl;
			this.ttlDuration = ttlDuration;
		}
	}
	
	//in milli
	private final static long CNAME_SUCCESS_lookupTTL = Config.getApp().getCnameOkLookupTTL() * 1000;
	
	//in milli
	private final static long CNAME_FAILD_lookupTTL = Config.getApp().getCnameFailLookupTTL() * 1000;
	
	//in milli
	private final static long NS_SUCCESS_lookupTTL = Config.getApp().getNsOkLookupTTL() * 1000;
		
	//in milli
	private final static long NS_FAILD_lookupTTL = Config.getApp().getNsFailLookupTTL() * 1000;

	private static ConcurrentMap<String, CacheState> cnameLookupCache = new ConcurrentHashMap<String, CacheState>();
	
	private static ConcurrentMap<String, CacheState> nsLookupCache = new ConcurrentHashMap<String, CacheState>();
	
	private static RootServers rootServers;
	
	private static Logger log = LoggerFactory.getLogger(LookupService.class);
	
	public static void dnsConfigLookup(final String host, final AsyncLookupResult asyncResult) {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
			
				asyncResult.setResult(LookupService.dnsConfigLookup(host));
			}
		}).start();
	}
	
	public static LookupResult dnsConfigLookup(String host) {
		
		try {
			
			Name target = Name.fromString(host);
			
			if(Config.MAIN_DOMAIN_NAME_WITH_CLUSTER_PREFIX.subdomain(target)) {
				
				//TODO I don't think any host name has a trailing dot 
				return new LookupResult(host, host);
			}
			
			long lookingUp = new Date().getTime();
			
			CacheState state = cnameLookupCache.get(host);
			
			if(state != null && (lookingUp - state.sottl <  state.cacheDuration)) {
					
				return state.result;
			}
			
			XbillDnsService dnsService = new XbillDnsService();
			
			NameRecord cname = dnsService.getMatchedCName(target, Config.MAIN_DOMAIN_NAME_WITH_CLUSTER_PREFIX);

			LookupResult result = null;
			
			long cacheDuration = CNAME_FAILD_lookupTTL;
			
			if(cname == null) {
				
				/*
				 * I changed from TXT to RP record. This way I don't need to force to create a
				 * separate TXT record for ACNAME when one exists*/
				//cname = dnsService.getMatchedTXTName(target, Config.MAIN_DOMAIN_NAME_WITH_CLUSTER_PREFIX);
				cname = dnsService.getMatchedRPName(target, Config.MAIN_DOMAIN_NAME_WITH_CLUSTER_PREFIX);
			}
			
			if(cname != null) {
				
				result = new LookupResult(cname.getName().toString(false), cname.getSecondName() == null? 
						null:cname.getSecondName().toString(false));
				
				if(cname.getTTL() < CNAME_SUCCESS_lookupTTL) {
					
					cacheDuration = CNAME_SUCCESS_lookupTTL;
				}
				else {
					
					cacheDuration = cname.getTTL();
				}
			}
			
			state = new CacheState();
			state.result = result;
			state.sottl = lookingUp;
			state.cacheDuration = cacheDuration;
			
			cnameLookupCache.put(host, state);
			
			return result;
			
		} catch (Exception e) {
			
			return null;
		}
	}
	
	public static List<NameRecord> nsLookup(String domain, String prefix) {
		
		try {
			
			long lookingUp = new Date().getTime();
			
			Name target = Name.fromString(domain);
			
			CacheState state = nsLookupCache.get(prefix);
			
			if(state != null && (lookingUp - state.sottl <  state.cacheDuration)) {
				
				if(state.result != null) {
				
					return state.result.getNsNameRecords();
				}
				else {
					
					return null;
				}
			}
			
			XbillDnsService dnsService = new XbillDnsService();
			
			List<NameRecord> ns = dnsService.getMatchedNSWithPrefix(target, Config.MAIN_DOMAIN_STR, prefix);

			LookupResult result = null;
			
			long cacheDuration = NS_FAILD_lookupTTL;
			
			if(ns != null && ns.size()>0) {
				
				result = new LookupResult(ns);
				
				if(ns.get(0).getTTL() < NS_SUCCESS_lookupTTL) {
					
					cacheDuration = NS_SUCCESS_lookupTTL;
				}
				else {
					
					cacheDuration = ns.get(0).getTTL();
				}
			}
			
			state = new CacheState();
			state.result = result;
			state.sottl = lookingUp;
			state.cacheDuration = cacheDuration;
			
			nsLookupCache.put(prefix, state);
			
			if(result != null) {
				
				return result.getNsNameRecords();
			}
			
			return null;
			
		} catch (Exception e) {
			
			return null;
		}
	}
	
	public static List<NameRecord> nsLookupWithDelegation(String domain, String prefix) {
		
		try {
			
			if(rootServers == null || !rootServers.checkIfTTLisValid()) {
				
				List<NameRecord> rs = XbillDnsService.getNSNames(Name.fromString("."));
				
				if(rs == null) {
					
					return null;
				}
				
				List<String> servers = new ArrayList<String>();
				
				for(int i=0; i< rs.size(); i++) {
					
					NameRecord c = rs.get(i);
					
					servers.add(c.getName().toString(true));
				}
				
				if(servers.size() > 0) {
					
					String[] srvArrs = new String[servers.size()];
					srvArrs = servers.toArray(srvArrs);
					rootServers = new RootServers(srvArrs, new Date().getTime(), NS_SUCCESS_lookupTTL);
				}
				else {
					
					return null;
				}
			}
				
			long lookingUp = new Date().getTime();
			
			Name target = Name.fromString(domain);
			
			CacheState state = nsLookupCache.get(prefix);
			
			if(state != null && (lookingUp - state.sottl <  state.cacheDuration)) {
				
				if(state.result != null) {
				
					return state.result.getNsNameRecords();
				}
				else {
					
					return null;
				}
			}
			
			XbillDnsService dnsService = new XbillDnsService();

			List<NameRecord> ns = dnsService.getMatchedNSWithPrefix(target, Config.MAIN_DOMAIN_STR, prefix, rootServers.servers);

			LookupResult result = null;
			
			long cacheDuration = NS_FAILD_lookupTTL;
			
			if(ns != null && ns.size()>0) {
				
				result = new LookupResult(ns);
				
				if(ns.get(0).getTTL() < NS_SUCCESS_lookupTTL) {
					
					cacheDuration = NS_SUCCESS_lookupTTL;
				}
				else {
					
					cacheDuration = ns.get(0).getTTL();
				}
			}
			
			state = new CacheState();
			state.result = result;
			state.sottl = lookingUp;
			state.cacheDuration = cacheDuration;
			
			nsLookupCache.put(prefix, state);
			
			if(result != null) {
				
				return result.getNsNameRecords();
			}
			
			return null;	
			
		} catch (Exception e) {
			
			log.error("", e);
			return null;
		}
	}
	
	public static List<NameRecord> nsLookupWithDelegation(String domain, List<String> names) {
		
		try {
			
			if(rootServers == null || !rootServers.checkIfTTLisValid()) {
				
				List<NameRecord> rs = XbillDnsService.getNSNames(Name.fromString("."));
				
				if(rs == null) {
					
					return null;
				}
				
				List<String> servers = new ArrayList<String>();
				
				for(int i=0; i< rs.size(); i++) {
					
					NameRecord c = rs.get(i);
					
					servers.add(c.getName().toString(true));
				}
				
				if(servers.size() > 0) {
					
					String[] srvArrs = new String[servers.size()];
					srvArrs = servers.toArray(srvArrs);
					rootServers = new RootServers(srvArrs, new Date().getTime(), NS_SUCCESS_lookupTTL);
				}
				else {
					
					return null;
				}
			}
				
			long lookingUp = new Date().getTime();
			
			Name target = Name.fromString(domain);
				
			CacheState state = nsLookupCache.get(domain);
			
			if(state != null && (lookingUp - state.sottl <  state.cacheDuration)) {
				
				if(state.result != null) {
				
					return state.result.getNsNameRecords();
				}
				else {
					
					return null;
				}
			}
			
			XbillDnsService dnsService = new XbillDnsService();

			List<NameRecord> ns = dnsService.getMatchedNS(target, names, rootServers.servers);

			LookupResult result = null;
			
			long cacheDuration = NS_FAILD_lookupTTL;
			
			if(ns != null && ns.size()>0) {
				
				result = new LookupResult(ns);
				
				if(ns.get(0).getTTL() < NS_SUCCESS_lookupTTL) {
					
					cacheDuration = NS_SUCCESS_lookupTTL;
				}
				else {
					
					cacheDuration = ns.get(0).getTTL();
				}
			}
			
			state = new CacheState();
			state.result = result;
			state.sottl = lookingUp;
			state.cacheDuration = cacheDuration;
			
			nsLookupCache.put(domain, state);
			
			if(result != null) {
				
				return result.getNsNameRecords();
			}
			
			return null;
		
		} catch (Exception e) {
			
			log.error("", e);
			return null;
		}
	}
}
