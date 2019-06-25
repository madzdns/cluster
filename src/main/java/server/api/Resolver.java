package server.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.api.Geo2.Location;
import server.config.ResolveHint;
import server.backend.node.INode;
import server.backend.node.dynamic.INFOResponceErrs;
import server.backend.node.dynamic.INFOResponse;
import server.backend.node.dynamic.ServiceInfo;
import server.backend.node.dynamic.SystemInfo;
import server.backend.node.dynamic.impl.Node;
import server.backend.node.dynamic.impl.Node.EntryParam;
import server.backend.node.statics.HeartbeatWorker;
import server.backend.node.statics.impl.HostNode;
import server.config.Config;
import server.config.Contact;
import server.config.EntryResolveModes;
import server.config.GeoMapper;
import server.config.GeoResolver;
import server.config.MyEntry;
import server.config.RequestCount;
import server.core.NodeNameResolver;
import server.core.RequestReportServiceCall;
import server.service.ThreadPoolService;
import server.utils.Dictionary;

public class Resolver { 
	
	private static Logger log = LoggerFactory.getLogger(Resolver.class);
	 
	private static ConcurrentMap<String,Dictionary<Set<INode>, MyEntry>> api_mapper = 
			new ConcurrentHashMap<String,Dictionary<Set<INode>, MyEntry>>();
	
	private static ConcurrentMap<String,INode> node_mapper = 
			new ConcurrentHashMap<String,INode>();
	
	private static ConcurrentMap<String,Dictionary<String,Long>> sessionNodeMapper = 
			new ConcurrentHashMap<String,Dictionary<String,Long>>();
	
	private static ConcurrentMap<String,Long> blackListMap = 
			new ConcurrentHashMap<String,Long>();
	
	private static Geo2 geo = new Geo2();
	
	private static volatile long resolverStart = new Date().getTime();
	
	private final static long NO_INTEZRVAL_CHECK_TIMEOUT = 300000;
	
	private final static ThreadPoolService threadService = new ThreadPoolService();
	
	static {
		
		try {
			
			log.info("Initiating Geo2 api");
			geo.initGeo(Config.getApp().getGeo());
			
		} catch (IOException e) {
			
			geo = null;
			log.error("",e);
			System.exit(1);
		}
	}
	
	public static Location testGeo(String ip) {
		
		return geo.getGeo(ip);
	}
	
	public static ConcurrentMap<String, INode> getMapperNode() {
		
		return node_mapper;
	}
	
	public static ConcurrentMap<String, Dictionary<Set<INode>, MyEntry>> getApiMapper() {
		
		return api_mapper;
	}
	
	public static void updateBlackList(String server) {
		
		blackListMap.put(server, new Date().getTime());
	}
	
	public static void removeBlackList(String server) {
		
		blackListMap.remove(server);
	}
	
	public static boolean checkBlackList(String server) {
		
		Long l = blackListMap.get(server);
		
		if(l != null) {
			
			long now = new Date().getTime();
			
			if((now - l) < 180000L) {
				
				return true;
			}
			
			removeBlackList(server);
		}
		
		return false;
	}
	
	public static INode
	updateMappers(
			final String server,
			final String key,
			List<String> geomaps,
			final List<SystemInfo> sysz,
			final List<ServiceInfo> services,
			final List<Contact> contacts,
			final String domain,
			final String ndomain,
			final short interval,
			final boolean auto,
			final boolean needbackends,
			//possible values are one of BPOLICY or WPOLICY or BPOLICY_RR or WPOLICY_RR
			final byte nosys_policy,
			//holds underlying session id
			final long id,
			final String gfactor,
			final boolean isfixed,
			final MyEntry theEntry,
			//this isroundrobined is just for fixed GeoResolvers and sysnex roundrobin is a value with nosys_policy
			final boolean isroundrobined,
			final byte nogeo_policy,
			final short version,
			final List<ResolveHint> resolveHints,
			final GeoMapper geoMapper) {

		/*for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
		    //System.out.println(ste);
			log.info("statck trace {}", ste);
		}*/
		
		Map<String,MyEntry> myEntries = null;
		MyEntry gm = null;
		GeoMapper theGeoMapper = null;
		//theEntry == null means it is for a host node
		if(!isfixed || theEntry == null) {
			
			myEntries = Config.getMyEntry(key);
			
			if((myEntries==null
					||myEntries.size()==0)) {
				
				log.error("Rcvd key {} is wrong for node {}. it was trying to get {} domain", key, server, domain);
				
				Node e = new Node(server, id,version);
				e.setStatus(INFOResponse.RSP_TYPE_ERR_NOFATTAL);
				
				String errrrr = new StringBuffer()
				.append(INFOResponceErrs.WRONG_HANDSHAKE)
				.append(" for ")
				.append(domain).toString();
				
				e.setErrBody(errrrr);
				return e;
			}
			
			gm = myEntries.get(domain);
			
			if(!isfixed && gm.getResolver().getConcurrents() == 0) {
				
				log.error("Node {} is attemting to heartbeat for static entry {}",server, domain);
				
				Node e = new Node(server, id,version);
				e.setStatus(INFOResponse.RSP_TYPE_ERR_NOFATTAL);
				
				String errrrr = new StringBuffer()
				.append(INFOResponceErrs.WRONG_ACCESS_FOR_FOR_FIXED_ENTRY)
				.append(" for ")
				.append(domain).toString();
				
				e.setErrBody(errrrr);
				return e;
			}
		}
		else {
			
			gm = theEntry;	
			
			theGeoMapper = geoMapper;
		}
		
		if(gm == null/* || !gm.getResolver().isActive()*/) {
			
			log.error("Inactive resource or wrong entry {} for node {}.",domain,server);
			return null;
		}
		
		if(gm.isJustHaDnsMode()) {
			
			log.error("Entry {} for node {} was just a HA DNS",domain,server);
			return null;
		}
		
		if(isfixed && theGeoMapper == null) {
			
			theGeoMapper = gm.getGeomapper(server);
			
			if(theGeoMapper == null) {
				
				log.error("Server {} was not defined as satic geomapper in entry {}",server, domain);
				
				Node e = new Node(server, id,version);
				e.setStatus(INFOResponse.RSP_TYPE_ERR_NOFATTAL);
				
				String errrrr = new StringBuffer()
				.append(INFOResponceErrs.WRONG_ACCESS_FOR_FOR_FIXED_ENTRY)
				.append(" for ")
				.append(domain).toString();
				
				e.setErrBody(errrrr);
				return e;
			}
		}
		
		Node e = (Node)node_mapper.get(server);

		if(e != null) {
			
			if(log.isDebugEnabled()) {
			
				log.debug("Getting existing entry for node {}/{} info [key:{},\n geoz:{},\n statistics:{} ,\n services:{},\n domain:{},\n nDomain:{},\n needbackends:{},\n auto_geo:{},\n interval:{},\n nosys_policy:{},\n id:{},\n gfactor:{},\ngeozNex_policy:{}, \n resolveHints: {}, \nis_fixed_rr:{}]\n",
						server,id,key,geomaps,sysz,services,domain,ndomain,needbackends,auto,interval,
						(int)nosys_policy,id,gfactor,(int)nogeo_policy, resolveHints, isroundrobined);
			}
			
			/*
			 * To prevent fixed and dynamic at the same time
			 */
			
			EntryParam nodeEntryParam = e.getEntryParam(domain);
			
			if(nodeEntryParam != null) {
			
				boolean existingNodeFixedState = nodeEntryParam.isFixed();
				
				if(isfixed != existingNodeFixedState) {
					
					log.warn("There was already a Node<server={}, fixed={}> in the dataset that confilicts with new Node<server={}, fixed={}>",
							e.getServer(), existingNodeFixedState, server, isfixed);
					
					if(!isfixed || theGeoMapper.isWithHeartbeatFromHost()) {
						
						e.setStatus(INFOResponse.RSP_TYPE_ERR);
						e.setErrBody(INFOResponceErrs.CONFILIC_WITH_FIXED);
						return e;
					}
					
					return null;
				}	
			}
			
			if(e.getId() == id || id == -1 || e.getId() == -1) {
				
				//to fix problem when server just got started and nodes are connected 
				if(resolverStart > 0) {
					
					long now = new Date().getTime();
					
					if(now - resolverStart > NO_INTEZRVAL_CHECK_TIMEOUT) {
						
						resolverStart = 0;
					}
				}
				
				if(!isfixed) {
				
					if( resolverStart == 0 && nodeEntryParam != null 
							&& !nodeEntryParam.checkInterval() ) {
						
						log.error("Node {} is working faster than {} which is registered with. Current reported interval is {}",server,e.getInterval(),interval);
						e.setStatus(INFOResponse.RSP_TYPE_ERR);
						e.setErrBody(new StringBuilder().append(INFOResponceErrs.INTERVAL_VIOLATION).append(e.getInterval()).toString());
						
						//we put peer in blacklist for 3 min
						updateBlackList(server);
						
						return e;
					}
					
					if(!e.checkUpdateResolverContexts(gm, contacts)) {
						
						log.error("GeoEntry {} exceeds concurrents limit with Node {}",gm.getResolver().getZone(),server);
						e.setStatus(INFOResponse.RSP_TYPE_ERR);
						e.setErrBody(new StringBuilder().append(INFOResponceErrs.CONCURENT_VIOLATION).append(gm.getResolver().getConcurrents()).toString());
						
						//we put peer in blacklist for 3 min
						updateBlackList(server);
						
						return e;
					}
				}
				
				if(auto) {
					
					if(geomaps == null)
						geomaps = new ArrayList<String>();
					setGeneratedGeoz(server,geomaps);
				}

				if(!isfixed || (isfixed && theEntry != null)) {
					
					EntryParam ep = e.updateEntryParams(gm,domain, ndomain, geomaps,
							gfactor,services,sysz,nosys_policy,nogeo_policy,
							resolveHints, isfixed, isroundrobined, 
							theGeoMapper == null ? GeoMapper.HEARTBEAT_NONE: 
								theGeoMapper.getHeartbeatType());
					
					nodeEntryParam = ep;
					
					if( !ep.getUpdateEntryParamResault()) {
						
						resolveNames(server,ep.getGeoParams(),ndomain, gm);	
					}
					
					updateApiMapper(e,gm);
				}
				
				if(!isfixed) {
						
					nodeEntryParam.updateLastReport();
					
					e.setNeedBackends(needbackends);
					
					return e;	
				}
			}

			if(isfixed && theEntry == null
					&& theGeoMapper.isWithHeartbeatFromHost()) {
				
				e.setStatus(INFOResponse.RSP_TYPE_NORMAL);
				e.setErrBody(null);
				
				if( resolverStart == 0 && nodeEntryParam != null
						&& !nodeEntryParam.checkInterval() ) {
					
					HostNode n = new HostNode(id,version);
					log.error("Node {} is working faster than {} which is registered with. Current reported interval is {}",server,e.getInterval(),interval);
					n.setStatus(INFOResponse.RSP_TYPE_ERR);
					n.setErrBody(new StringBuilder().append(INFOResponceErrs.INTERVAL_VIOLATION).append(e.getInterval()).toString());
					
					//we put peer in blacklist for 3 min
					updateBlackList(server);
					
					return n;
				}
				
				HostNode n = new HostNode(id,version);
				
				if(nodeEntryParam != null && nodeEntryParam.updateLastReportForHostNode(gm.getResolver().getMinInterval())) {
					
					e.setNeedBackends(true);
				}
				else {
					
					e.setNeedBackends(false);
				}
				
				n.setNeedBackends(e.isNeedBackends());
				n.setUnderliyingNode(e);
				
				return n;
			}
			
			if(isfixed && theGeoMapper.isWithHeartbeatFromCloud()) {
				
				String nndomain = ndomain;
				
				if(nndomain == null) {
					
					nndomain = server;
				}
				
				HeartbeatWorker.addJob(e, domain, nndomain, 80, interval);
				return e;
			}
			
			if(isfixed) {
				
				return e;
			}
			
			String sessKey = new StringBuilder()
			.append(server)
			.append(id).toString();
			
			if(sessionNodeMapper.containsKey(sessKey)) {
				
				/*
				 * 
				 */
				
				//we do not count down resolvers here and let the close Handler do that
				//TODO Does not count downing here makes a big deal?
				/*
				 * Now that I'm looking around I see not only there is no need
				 * to count down here, but also its wrong! because we already put 
				 * this node in sessionNodeMapper and we counted down it for sure
				 */
				
				log.error("Previous session {} is still alive while another session {} is reporting for Node {}",id,e.getId(),server);
				e = new Node(server, id,version);
				e.setStatus(INFOResponse.RSP_TYPE_ERR);
				e.setErrBody(INFOResponceErrs.DUPLICATE_INSTANCES);
				return e;
			}
			
			log.error("Sounds Node {} has been lost its session and has not been closed yet. removing previous entries",server);
			
			String nodeAddressKey = new StringBuilder()
			.append(server)
			.append(e.getId()).toString();
			
			/*
			 * We put previous node that was reporting into sessionNodeMapper
			 * and trust the new new one. If it reports again, we know something
			 * is wrong
			 */
			
			sessionNodeMapper.put(nodeAddressKey,new Dictionary<String, Long>(server, e.getId()));
			
			node_mapper.remove(server);
			
			for(Entry<String, EntryParam> entry:e.getEntryParamsMapper().entrySet())
					removeNodeFromApizSafe(entry.getKey(),e);
			
				e.countDwnResolvers();
		}
		
		if(log.isDebugEnabled()) {
		
			log.debug("Creating new entry for node {}/{} info [key:{},\n geoz:{},\n statistics:{} ,\n services:{},\n domain:{},\n nDomain:{},\n needbackends:{},\n auto_geo:{},\n interval:{},\n nosys_policy:{},\n id:{},\n gfactor:{},\n isfixed:{},\ngeozNex_policy:{}, \nresolveHints:{}, \nis_fixed_rr:{} \n]\n",
					server,id,key,geomaps,sysz,services,domain,ndomain,needbackends,auto,interval,
					(int)nosys_policy,id,gfactor,isfixed,(int)nogeo_policy,
					resolveHints, isroundrobined);
		}
		
		if(isfixed && theEntry == null) {
			
			//We don't create a new Node when request comes for host nodes
			//before context had created them.
			
			e = new Node(server, id,version);
			
			log.error("Wrong access for Host Node {} for domain {}", server, domain);
			e.setStatus(INFOResponse.RSP_TYPE_ERR);
			e.setErrBody(INFOResponceErrs.WRONG_ACCESS_FOR_HOST_NODE);
			return e;
		}
		
		e = new Node(server, interval, id, version);
		
		if(isfixed && theGeoMapper.isWithHeartbeatFromCloud()) {
			
			String nndomain = ndomain;
			
			if(nndomain == null) {
				
				nndomain = server;
			}
			
			HeartbeatWorker.addJob(e, domain, nndomain, 80, interval);
		}
		
		EntryParam ep = e.updateEntryParams(gm,domain,ndomain,geomaps,gfactor,
				services,sysz,nosys_policy,nogeo_policy,
				resolveHints, isfixed, isroundrobined, 
				theGeoMapper == null ? GeoMapper.HEARTBEAT_NONE: 
					theGeoMapper.getHeartbeatType());
		
		if(!isfixed) {
		
			if(interval < gm.getResolver().getMinInterval()) {
				
				log.error("Node {} has set interval {} which is less than {}",server,interval,gm.getResolver().getMinInterval());
				e.setStatus(INFOResponse.RSP_TYPE_ERR);
				e.setErrBody(INFOResponceErrs.INTERVAL_VIOLATION+gm.getResolver().getMinInterval());
				return e;
			}
			
			if(!e.checkUpdateResolverContexts(gm, contacts)) {
				
				log.error("GeoEntry {} exceeds concurrents limit with Node {}",gm.getResolver().getZone(),server);
				e.setStatus(INFOResponse.RSP_TYPE_ERR);
				e.setErrBody(INFOResponceErrs.CONCURENT_VIOLATION+gm.getResolver().getConcurrents());
				return e;
			}
			
			ep.updateLastReport();
			
			e.setNeedBackends(true);
		}
		
		if(auto) {
			
			if(geomaps==null)
				geomaps = new ArrayList<String>();
			
			setGeneratedGeoz(server,geomaps);
		}
		
		if(!ep.getUpdateEntryParamResault()) {
			
			resolveNames(server,ep.getGeoParams(),ndomain, gm);	
		}
		
		log.info("putting server {} in node_mapper", server);
		
		/*
		 * If while we were putting the node in the node_mapper, another fixed node came (or vice versa)
		 * we simply put it too, it'll replace it, then later it'll be fixed with the above codes
		 */
		node_mapper.put(server,e);
		
		updateApiMapper(e,gm);
		
		return e;
	}
	
	public static ResolveResult getSomeServer(final String id) throws Exception{
		
		if(api_mapper.size()==0) {
			
			return null;	
		}
		
		Dictionary<Set<INode>, MyEntry> dict = api_mapper.get(id);
		
		if(dict == null) {
			
			return null;
		}

		Set<INode> servingNodes = dict.getValue1();
		
		if(servingNodes == null) {
			
			return null;
		}
		
		Node n = (Node)servingNodes.iterator().next();

		return new ResolveResult(n.getEntryParam(id).getResolveHints(), n.getServer(), false);
	}
	
	public static ResolveResult getBestServer(final String client_ip,final String id) throws Exception {
		
		return getBestServer(client_ip,id,(byte)0,(short)0, false, null, false, null);
	}
	
	public static ResolveResult getBestServer(final String client_ip,
			final String id,
			final byte proto,
			final short port,
			final boolean tryNodeDomain) throws Exception {
		
		return getBestServer(client_ip, id, proto, port, tryNodeDomain, null, false, null);
	}
	
	public static ResolveResult getBestServer(final String client_ip,
			final String id,
			final byte proto,
			final short port,
			final boolean tryNodeDomain,
			final String suggestedServer,
			final boolean canProxy,
			final String interfaceIp) throws Exception {

		if(api_mapper.size()==0) {
			
			log.debug("api_mappers was empty");
			return null;	
		}
		
		Dictionary<Set<INode>, MyEntry> dict = api_mapper.get(id);
		
		if(dict == null) {
			
			log.debug("Api Mapper DICT was null for {}", id);
			
			return null;
		}
		
		Set<INode> servingNodes = dict.getValue1();
		
		if(log.isDebugEnabled()) {
			
			log.debug("ServingNodes for {} are {}", id, servingNodes);
		}
		
		if(servingNodes == null) {
			
			return null;
		}
		
		GeoResolver gr = dict.getValue2().getResolver();
		
		if(gr.getRequestCount().isRanOUt()) {
			
			log.error("RequestCount resource {} is ran out for {}", gr.getRequestCount(), id);
			return null;
		}
		
		Node best_node = null;
		
		if(suggestedServer != null) {
			
			log.debug("Checking suggested server ip {} for domain {}", suggestedServer, id);
			
			for(Iterator<INode> it = servingNodes.iterator(); it.hasNext(); ) {
				
				Node node = (Node) it.next();
				
				if(node.getServer().equals(suggestedServer)) {
					
					if(node.isQuestionedSrvAlive(id, proto, port)) {
						
						log.debug("Sugested server ip {} was OK for domain {}", suggestedServer, id);
						best_node = node;
					}
					
					break;
				}	
			}
		}
		
		boolean isUsingProxy = dict.getValue2().getResolveMode() == EntryResolveModes.PROTO_RESOLVE_PROXY.getValue()
				&& canProxy;
		
		double tmp_factor = Node.FAILED_LOAD;
		double factor = Node.DUMMY_LOAD;
		
		double alternative_factor = Node.DUMMY_LOAD;
		
		Node alternative_node = null;
		
		if(best_node == null) {
			
			Location loc = null;
			
			if(isUsingProxy) {
				
				//This way we get nearest server to our server for proxy
				loc = geo.getGeo(interfaceIp);
			}
			else {
				
				loc = geo.getGeo(client_ip);
			}
			
			EntryParam eep = null;
		
			for(Iterator<INode> it = servingNodes.iterator(); it.hasNext(); ) {
				
				Node node = (Node) it.next();
				
				eep = node.getEntryParam(id);
				
				if(eep == null || !eep.checkIfHostNodeIsAlive()) {

					continue;
				}
				
				tmp_factor = node.getFactor(id,loc,proto,port);

				if(factor < tmp_factor) {
					
					alternative_factor = factor;
					alternative_node = best_node;
					
					factor = tmp_factor;
					best_node = node;
				}
				else if(alternative_factor <= tmp_factor) {
					
					alternative_factor = tmp_factor;
					alternative_node = node;
				}
			}
		}

		if(best_node == null) {
			
			return null;	
		}
		
		//log.info("BestNode={}, isRoundRobin={}, AlternativeNode={}", best_node.getServer(), best_node.isRoundRobined(), alternative_node.getServer());
		
		EntryParam ep = best_node.getEntryParam(id);
		
		List<ResolveHint> resolveHints = ep.getResolveHints();
		
		if((ep.isFixed() && ep.isRoundRobined())
				|| (ep != null && ep.getNodeParams().isRounroubined())) {

			best_node.updateRound();
			//we don't serve two addresses for round robins
			alternative_node = null;
		}
		
		Node.GeoParams gp = ep.getGeoParams();
		
		String server = best_node.getServer();
		
		RequestCount rc = gr.getRequestCount();
		
		int inc = 1;
		
		int condition = Config.getApp().getRequestCountIncrementCondition();
		
		boolean resolveModeWithProxy = false;
		
		if(isUsingProxy) {
			
			resolveModeWithProxy = true;
			
			inc = 10;
			
			condition *= 2;
		}
		
		if(rc.increaseCurrentUsedValueBy(inc, condition)) {

			threadService.call(new RequestReportServiceCall(gr.getZone(), dict.getValue2().getKey(), gr.getPlanZoneId(), rc, Config.getApp().getApiCallProto(),
					Config.MAIN_DOMAIN_STR_WITHOUT_TRAILING_DOT, Config.getApp().getRequestCountApiPath()));
			
			/* Only to test in localhost
			 * threadService.call(new RequestReportServiceCall(gr.getZone(), dict.getValue2().getKey(), gr.getPlanZoneId(), rc, Config.getApp().getApiCallProto(),
					"frfra.localhost", Config.getApp().getRequestCountApiPath()));*/
		}
		
		log.debug("Request Count DEBUG for {} -> {}", id, rc);
		
		//It means we are resolving for dns
		if(!tryNodeDomain) {
			
			String[] servers = null;
			
			if(alternative_node != null) {
				
				servers = new String[] {server, alternative_node.getServer() };
			}
			else {

				servers = new String[] {server };
			}
			
			return new ResolveResult(resolveHints, servers, resolveModeWithProxy);
		}
		
		if(gp!=null) {
			
			if(gp.getNodeDomain()==null || gp.getNodeDomain().equals("")) {
				
				if(ep.isFixed()) {
					
					resolveNames(best_node.getServer(),gp,gp.candidateNodeDomain,ep.getEntry());	
				}
			}
			else {
				
				server = gp.getNodeDomain();
			}
		}
		
		return new ResolveResult(resolveHints, server, resolveModeWithProxy);
	}
	
	private static void setGeneratedGeoz(final String server,List<String> geomaps) {
		
		Location loc = geo.getGeo(server);
		
		if(loc.isp!=Geo2.UNKNOWN
				&&!geomaps.contains(loc.isp))
			
			geomaps.add(loc.isp);
		
		if(loc.city!=Geo2.UNKNOWN
				&&!geomaps.contains(loc.city))
			
			geomaps.add(loc.city);
		
		if(loc.region!=Geo2.UNKNOWN
				&&!geomaps.contains(loc.region))
			
			geomaps.add(loc.region);
		
		if(loc.country!=Geo2.UNKNOWN
				&&!geomaps.contains(loc.country))
			
			geomaps.add(loc.country);
		
		if(loc.continent!=Geo2.UNKNOWN
				&&!geomaps.contains(loc.continent))
			
			geomaps.add(loc.continent);
	}
	
	private static void resolveNames(final String server,final Node.GeoParams gp,
			final String ndomain, final MyEntry entry) {
		
		if(gp == null) {
			
			log.error("Parameter GeoParams is null for node {}",server);
			return;
		}
		
		if(gp.nameTriedDate != null) {
			
			Date now = new Date();
			
			if((now.getTime() - gp.nameTriedDate.getTime()) < gp.maxNameTriedWaitTime ) {
				
				return;	
			}
		}

		String fullNdomain = new StringBuilder()
		.append(ndomain)
		.append('.').toString();
		
		if(entry.getFullAName().equals(fullNdomain)) {
			
			/*
			 * If someone set ndomain to domain
			 * but its not a dnsResolve entry, later causes
			 * a loop on server. This prevent it.
			 */
			if(!entry.isDnsResolve()) {
				
				//He configured the wrong one. We increment
				gp.setNodeDomain(server);

				gp.nameTriedDate = new Date();
				
				gp.maxNameTriedWaitTime += Node.DOMAIN_FAIL_WAIT_MILISECOND;
				
			}
			else {
				
				gp.setNodeDomain(ndomain);
				gp.nameTriedDate = null;
				log.debug("Domain {} set successfuly for node {}",ndomain,server);
			}
			
			return;
		}
		
		threadService.call(new NodeNameResolver(server, gp, fullNdomain, entry));
	}
	
	public static void removeNodeFromApizSafe(final String key, final Node e) {
		
		Dictionary<Set<INode>, MyEntry> dict = getApiMapper().get(key);
		
		if(dict == null) {

			return;
		}
		
		Set<INode> nodes = dict.getValue1();
		
		if(nodes!=null) {
			
			nodes.remove(e);
			if(nodes.size()==0) {
				
				log.debug("Removing entire api {}",key);
				getApiMapper().remove(key);
			}
		}
	}
	
	public static void sessNodeCleanUp(final String node,final long id) {
		
		log.debug("Cleaning up node {} session entry",node);
		
		String sessId = new StringBuilder().append(node).append(id).toString();
		sessionNodeMapper.remove(sessId);
	}
	
	public static void updateApiMapper(final Node e,final MyEntry entry) {
		
		synchronized (entry.getMapperLock()) {
			
			Dictionary<Set<INode>, MyEntry> dict = api_mapper.get(entry.getFullAName());
			
			Set<INode> nodeSet = null;
			
			if(dict == null) {
				
				nodeSet = Collections.newSetFromMap(new ConcurrentHashMap<INode,Boolean>());

				dict = new Dictionary<Set<INode>, MyEntry>(nodeSet, entry);

				api_mapper.put(entry.getFullAName(), dict);
				api_mapper.put(entry.getApikey(), dict);
			}
			else {
				
				nodeSet = dict.getValue1();
			}
			
				
			//If it is already there, wont be added, because its a set
			nodeSet.add(e);	
		}
	}
	
	//Counting down GeoResolvers count should be done before calling this (if needed)
	public static void purgeNodeMapper(final Node node) {
		
		log.debug("Removing Node[server={}]",node.getServer());
		sessNodeCleanUp(node.getServer(), node.getId());
		node_mapper.remove(node.getServer());
	}
	
	public static void setupFixedEntries(final List<MyEntry> entryList) {
		
		if(entryList == null)
			
			return;

		for(int i = 0;i<entryList.size();i++) {
			
			MyEntry entry = entryList.get(i);
			
			if(entry.getResolver()==null
					/*||!entry.getResolver().isActive()*/) {
				
				log.info("Fixed entry {} is not active",entry.getFullAName());
				continue;
			}
			
			List<GeoMapper> gmList = entry.getGeoMapper();
			
			if(gmList==null)
				
				continue;
			
			for(int j =0; j < gmList.size();j++) {
				
				GeoMapper gm = gmList.get(j);
				List<String> geoz = Arrays.asList("*");
				List<Contact> contacts = null;
				boolean auto = false;
				
				if(gm.getGeoz()!=null) {
					
					geoz = gm.getGeoz().getGeoz();
					auto = gm.getGeoz().isAuto();
				}
				
				/*TODO I commented this out because why would 
				 * anyone wants to set contacts on fixed?
				 * if(entry.getResolver().getContacts()!=null)
					
					contacts = entry.getResolver().getContacts().getContacts();*/

				INode node = updateMappers(gm.getAddr(),null,geoz,null,null,contacts,
						entry.getFullAName(),gm.getNdomain(),(short)entry.getResolver().getMinInterval(),auto,false,
						//TODO see if there is a need for make a work for nosys and nogeo polices
						(byte)1,-1,"1",true,entry,gm.isWithRoundRobin(),(byte)1,(short)0,
						//TODO should we get hint from fixids?
						null, gm);
				
				Node e = (Node) node;
				
				if( e != null && e.getStatus() == INFOResponse.RSP_TYPE_NORMAL) {
					
					Node.GeoParams egparams = e.getGeoParams(entry.getFullAName());
					
					if(egparams!=null
							&&egparams.getGeoz()!=null
							&&egparams.getGeoz().size()>0
							&&gm.getGeoz()!=null
							&&gm.getGeoz().isAuto()) {
						
						gm.getGeoz().setGeoz(egparams.getGeoz());
						gm.getGeoz().setAuto(false);
					}
				}
			}
		}
	}
	
	
	/***
	 * The goal is to handle concurrent access to a resolver.
	 * there is two implementation. One is in each node. when monitor detect that
	 * a resource is changes, it should remove alapiMapperUpdateMutxl Georesolver that is changed or removed.
	 * right now, the algorithm is:
	 * Once an node start reports, Resolver calls chkUpdateConcurrentResolvers of the node.
	 * it checks if the given resolver is ok to use.
	 * If it is, then node will keep the resolver, if not node will be rejected.
	 * Then in next reports Resolver calls chkUpdateConcurrentResolvers again, if the node had that
	 * GeoResolver, then it is safe and can go on, else check if there is available concurrent left
	 * if there was, then the node adds that GeoResolver and continues else Resolver will reject it.
	 * In every resource changes (modification or deletion) Monitor would remove affected GeoResolver
	 * From containing nodes so they need to check availability of it again.
	 * this method is not used right now and I keep it in the case of previous approach did not worke out 
	 * @param resolver
	 * @param node
	 * @return
	private static ConcurrentMap<String,Set<String>> concurrents = 
		new ConcurrentHashMap<String, Set<String>>();
	private static Object concMutx = new Object();
	public static boolean chkUpdateConcurrents(GeoResolver resolver,String node)
	{
		synchronized (concMutx) {
			Set<String> nodes = concurrents.get(resolver.getZone());
			if(nodes==null)
			{
				if(resolver.getConcurrents()>0)
				{
					nodes = Collections.newSetFromMap(new ConcurrentHashMap<String,Boolean>());
					nodes.add(node);
					concurrents.put(resolver.getZone(),nodes);
					return true;
				}
				return false;
			}
			if(nodes.contains(nodes))
				return true;
			if(nodes.size()<resolver.getConcurrents())
			{
				nodes.add(node);
				return true;
			}
			return false;
		}
	}
	*/
}