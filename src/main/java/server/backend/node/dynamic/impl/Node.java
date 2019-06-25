package server.backend.node.dynamic.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javassist.NotFoundException;
import server.config.ResolveHint;
import server.backend.node.BaseNodeV1;
import server.backend.node.dynamic.GeoNexTypes;
import server.backend.node.dynamic.ServiceInfo;
import server.backend.node.dynamic.SysNexTypes;
import server.backend.node.dynamic.SystemInfo;
import server.config.Contact;
import server.config.GeoMapper;
import server.config.GeoResolver;
import server.config.MyEntry;
import server.api.Geo2;
import server.api.Geo2.Location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * A note about concurrency
 * Apache MINA promised that the same Session wont
 * get called concurrently.
 * Provided that, I made an Id for each node representing
 * its session Id, Every time a new request comes, I know
 * its only from one node.
 * Now there are three kinds of resources, one that are shared
 * between Nodes (a) and one that are specified to a node
 * but can be accessed in different threads(b) and one is used
 * inside each node. For those resources that are used inside
 * each node, there is no worry.
 * 
 * Also there are three kinds of threads.
 * One is Apache Mina's thread per node request (A), Other is
 * Apache Mina's thread that are searching for proper node
 * to answer client's requests (B) and the other is one that
 * catches up idle or stoped nodes (C).
 * 
 * 1) A always tries to check if node interval is sane. for that,
 * it checks some integer and datetime values like 
 * intervalDiff, intervallastReport, intervalMili, intervalViolateCounter
 * becase the only thread that access those resources per node is A, according
 * the the mentioned rule of MINA, no need to make them concurrent.
 * 
 * 2) A always tries to check concurrent access and contacts sanity.
 * because those are something about the GeoResolvers that each node
 * are serving and there is possibility that multiple node accessing
 * the same getResolver. So concurrents count and Contants count should
 * be synchronized inside GeoResolvers.
 * Also there are contactsPerDomain and contacts inside node.
 * contactsPerDomain is a hashmap that keeps contacts per domain name
 * it only get accessed via A when updating concats and when somethnig
 * goes wrong inside updateEntryParams(). Inside updateEntryParams, contacts
 * per domains, is passed to reporter. But because contacts inside contactsPerDomain
 * always get subsituated, no worry about them. and because contactsPerDomain always
 * is accessed via thread A no worry about it too.
 * contacts maybe called with 2 different A threads and that's when 2 different sessions
 * are reporting for the same server (for example Administrator forgot to close one instance
 * of Node and start other) in that case we attemt to count down previouse node from new session.
 * in countDown don't worry for contacts, we make it null. Also if that previouse Session reports
 * in a different thread simultaniously, both have their own resolver map and contacts and
 * contactsPerDomain. But another thread that could access countDown, is C. There is possibility
 * that when we countDown other node in A, at the same time, C is counting down it too.
 * for that case, resolver is a ConcurrentMap. But that is not enough. Because the problem is
 * Resoulvers get countdown twise! so we make it sunchronized. Another thing might happen is that
 * when we counting down a node whether in A or in C, that node somehow is updating its resolver!
 * TODO I don't see any problem for that case
 * 
 * 3) Other case is when A is updating  
 *  
 */

public class Node extends BaseNodeV1 {
	
	public class GeoParams {
		
		private List<String> geoz = null;
		private String nodeDomain = null;
		public double GEO_FACTOR = 0.5;
		public Date nameTriedDate = null;
		public int maxNameTriedWaitTime = 0;
		public int multiplyer = 1;
		public String candidateNodeDomain = null;
		private double noGeoPolicy = -2;
		public int noGeoPolicyPosition = 0;
		
		public GeoParams(final List<String> geoz,final String geo_factor,final byte nogeo_policy) {
			
			this.geoz = geoz;
			
			if(geo_factor!=null) {
				
				try {
					
					GEO_FACTOR = Double.parseDouble(geo_factor);
					
				}catch(Exception e) {
					
					log.error("",e);
				}
			}
			
			this.noGeoPolicy = (2*(this.noGeoPolicyPosition = nogeo_policy))-2;
		}
		
		public List<String> getGeoz() {
			
			return geoz;
		}
		
		public String getNodeDomain() {
			
			return nodeDomain;
		}
		
		public void setNodeDomain(final String ndomain) {
			
			this.nodeDomain = ndomain;
		}
		
		public double getNoGeoPolicy() {
			
			return noGeoPolicy;
		}
		
		public void setNoGeoPolicy(byte noGeoPolicy) {
			
			this.noGeoPolicy = (2*(this.noGeoPolicyPosition = noGeoPolicy))-2;
		}
	}
	
	public class NodeParams {
		
		private List<ServiceInfo> srvInfoz = null;
		private List<SystemInfo> sysInfoz = null;
		private List<ServiceInfo> downSrvz = null;
		private List<SystemInfo> loadedSysz = null;
		private double loads = DUMMY_LOAD;
		private double diff = 0;
		private double minSum = 0;
		private boolean noSysPolicy = false;
		private boolean rounroubined = false;
		
		private final Object loadLock = new Object();
		
		public NodeParams(final List<ServiceInfo> srvInfoz,
				final List<SystemInfo> sysInfoz,final byte noSysPolicy) {
			
			setSrvInfoz(srvInfoz);
			setSysInfoz(sysInfoz);
			
			if( sysInfoz != null && sysInfoz.size() > 0 ) {
				
				double maxSum = 0;
				
				for(Iterator<SystemInfo> sIt = sysInfoz.iterator(); sIt.hasNext();) {
					
					SystemInfo s = sIt.next();
					maxSum += s.xthreshold;
					minSum += s.nthreshold;
				}
				
				diff = (maxSum-minSum);
			}
			
			if(noSysPolicy==SysNexTypes.BPOLICY)
				this.noSysPolicy = true;
			else if(noSysPolicy==SysNexTypes.WPOLICY_RR)
				this.rounroubined = true;
			else if(noSysPolicy==SysNexTypes.BPOLICY_RR)
				this.rounroubined = this.noSysPolicy = true;
		}
		
		public void setSysInfoz(final List<SystemInfo> sysInfoz) {
			
			rstLoads();
			
			if(sysInfoz!=null
					&&sysInfoz.size()>0) {
				
				for(Iterator<SystemInfo> sIt = sysInfoz.iterator(); sIt.hasNext();) {
					
					SystemInfo s = sIt.next();
					s.value = (byte)(SystemInfo.isNetType(s.type)?getNetParam(s.type):s.value);
				}
			}
			checkSysz(sysInfoz);
			this.sysInfoz = sysInfoz;
		}
		
		public void setSrvInfoz(final List<ServiceInfo> srvInfoz) {
			
			checkSrvz(srvInfoz);
			this.srvInfoz = srvInfoz;
		}
		
		public double getLoads() {
			
			synchronized (loadLock) {
				
				if(loads > DUMMY_LOAD)
					
					return loads;
				
				double failed = sysFailed();
				
				if(failed <= FAILED_LOAD)
					
					return loads = failed;
				
				if(sysInfoz!=null && sysInfoz.size()>0) {
					
					loads = 0;
					
					for(Iterator<SystemInfo> sIt = sysInfoz.iterator(); sIt.hasNext();) {
						
						SystemInfo s = sIt.next();
						
						if(SystemInfo.isNetType(s.type))
							loads += getNetParam(s.type);
						else
							loads += s.value;
					}
				}
				
				if(loads == DUMMY_LOAD) {
					
					if(noSysPolicy)
						
						return loads = ISP_FACTOR;
					
					return loads = FAILED_LOAD;
				}
				
				if( diff == 0 )
					
					return DUMMY_LOAD;
				
				loads = ((ISP_FACTOR/-diff)*(loads-minSum))+ISP_FACTOR;
				
				return loads;	
			}
		}
		
		public synchronized void rstLoads() {
			
			synchronized (loadLock) {
			
				loads = DUMMY_LOAD;
			}
		}
		
		private void checkSrvz(final List<ServiceInfo> srvInfoz) {
			
			if(srvInfoz == null) {
				
				return;	
			}
			
			downSrvz = new ArrayList<ServiceInfo>();
			
			for(Iterator<ServiceInfo> sIt = srvInfoz.iterator(); sIt.hasNext();) {
				
				ServiceInfo s = sIt.next();
				
				if(s.value==ServiceInfo.DOWN)
					downSrvz.add(s);
			}
		}
		
		private void checkSysz(final List<SystemInfo> sysInfoz) {
			
			if(sysInfoz == null)
				return;
			
			loadedSysz = new ArrayList<SystemInfo>();
			
			for(Iterator<SystemInfo> sIt = sysInfoz.iterator(); sIt.hasNext();) {
				
				SystemInfo s = sIt.next();
				
				if(s.value>=s.xthreshold)
					
					loadedSysz.add(s);
			}
		}
		
		private double sysFailed() {
			
			if(loadedSysz==null) {
			
				return 0;
			}
			
			double failed = 0;
			
			for(Iterator<SystemInfo> sIt = sysInfoz.iterator(); sIt.hasNext();) {
				
				SystemInfo sys = sIt.next();
				
				if(sys.critical==SystemInfo.ISCRITICAL)
					failed += FAILED_LOAD;
			}
			
			return failed;
		}
		
		public List<ServiceInfo> getSrvInfoz() {
			
			return srvInfoz;
		}
		
		public List<ServiceInfo> getDownSrvz() {
			
			return downSrvz;
		}
		
		public void setDownSrvz(final List<ServiceInfo> downSrvz) {
			
			this.downSrvz = downSrvz;
		}
		
		public List<SystemInfo> getSysInfoz() {
			
			return sysInfoz;
		}
		
		public List<SystemInfo> getLoadedSysz() {
			
			return loadedSysz;
		}
		
		public boolean isRounroubined() {
			
			return rounroubined;
		}
	}
	
	public class EntryParam {
		
		private NodeParams nodeParams;
		
		private GeoParams geoParams;
		
		private MyEntry entry;
		
		private List<ResolveHint> resolveHints;
		
		private boolean updateEntryParamResault = true;
		
		private boolean fixed = false;
		private boolean roundRobined = false;
		private byte fixedNodeType = 0;
		
		private Date lastReport = null;/*this just is used to check Nodes being alive.
		* meaning this is not the true per interval.
		* */
		
		private long intervallastReport = 0;//this is used to check (true interval)
		
		private int intervalViolateCounter = 0;
		
		private long intervalDiff = 0;
		
		public EntryParam(final NodeParams nodeParams,final GeoParams geoParams, MyEntry entry,
				List<ResolveHint> resolveHints, final boolean isFixed, final boolean isRoundRobin, 
				final byte heartbeatType) {
			
			this.nodeParams = nodeParams;
			this.geoParams = geoParams;
			this.entry = entry;
			this.resolveHints = resolveHints;
			
			this.fixed = isFixed;
			this.roundRobined = isRoundRobin;
			this.fixedNodeType = heartbeatType;
		}
		
		public NodeParams getNodeParams() {
			
			return nodeParams;
		}
		
		public void setNodeParams(NodeParams nodeParams) {
			
			this.nodeParams = nodeParams;
		}
		
		public GeoParams getGeoParams() {
			
			return geoParams;
		}
		
		public void setGeoParams(GeoParams geoParams) {
			
			this.geoParams = geoParams;
		}

		public MyEntry getEntry() {
			
			return entry;
		}

		public void setEntry(MyEntry entry) {
			
			this.entry = entry;
		}

		public List<ResolveHint> getResolveHints() {
			
			return resolveHints;
		}

		public void setResolveHints(List<ResolveHint> resolveHints) {
			
			this.resolveHints = resolveHints;
		}

		public boolean getUpdateEntryParamResault() {
			
			return updateEntryParamResault;
		}

		public void setUpdateEntryParamResault(boolean updateEntryParamResault) {
			
			this.updateEntryParamResault = updateEntryParamResault;
		}

		public boolean isFixed() {
			
			return fixed;
		}

		public boolean isRoundRobined() {
			
			return roundRobined;
		}

		public byte getFixedNodeType() {
			
			return fixedNodeType;
		}

		public void setFixed(boolean fixed) {
			
			this.fixed = fixed;
		}

		public void setRoundRobined(boolean roundRobined) {
			
			this.roundRobined = roundRobined;
		}

		public void setFixedNodeType(byte fixedNodeType) {
			
			this.fixedNodeType = fixedNodeType;
		}
		
		public void updateLastReport() {
			
			if(isFixed()) {
				
				return;
			}	
			
			this.lastReport = new Date();
				
			nodeLatestReport = this.intervallastReport = this.lastReport.getTime();
		}
		
		public boolean updateLastReportForHostNode(int interval) {
			
			this.lastReport = new Date();
			
			long intervalMil = interval * 1000;
				
			boolean shouldSendBackends = false;
			long intervallastReportTmp = nodeLatestReport = this.lastReport.getTime();
				
			//for fixed-check-alive nodes we check if it had not report for a while send backends
			shouldSendBackends = (intervallastReportTmp - intervallastReport - intervalMil) > (intervalMil/3);
			this.intervallastReport = intervallastReportTmp;
			
			return shouldSendBackends;
		}
		
		public void updateLastReportForHostNode() {
			
			this.lastReport = new Date();
			
			nodeLatestReport = this.intervallastReport = this.lastReport.getTime();
		}
		
		public Date getLastReport() {
			
			return this.lastReport;
		}
		
		public boolean checkInterval() {
			
			this.intervalDiff+= ((new Date().getTime() - this.intervallastReport)-(intervalMili));
			
			if(this.intervalDiff < 0 && this.intervalDiff >= -10)
				
				this.intervalDiff = 0;
			
			if(this.intervalDiff < 0) {
				
				this.intervalViolateCounter ++;
				this.intervalDiff = 0;
			}
			else
				this.intervalViolateCounter = 0;
			
			return this.intervalViolateCounter <= MAX_ALLOWED_INTERVAL_VOILATION;
		}
		
		public boolean checkIfHostNodeIsAlive() {
			
			if(!isFixed()) {
				
				return true;
			}
	
			MyEntry entry = getEntry();
			
			long intervalMil = entry.getResolver().getMinInterval() * 1000;

			if(entry != null && getFixedNodeType() != GeoMapper.HEARTBEAT_NONE) {
				
				return (new Date().getTime() - this.intervallastReport - intervalMil) <= (intervalMil/3); 
			}
			
			return true;
		}
	}
	
	public class NetParams {
		
		private Date last;
		private byte value;
		private byte type;
		
		public NetParams(final byte type,final byte value) {
			
			this.type = type;
			this.value = value;
			this.last = new Date();
		}
		
		public Date getLast() {
			
			return last;
		}
		
		public void setLast(final Date last) {
			
			this.last = last;
		}
		
		public byte getValue() {
			
			return value;
		}
		
		public void setValue(final byte value) {
			
			this.value = value;
		}
		
		public byte getType() {
			
			return type;
		}
		
		public void setType(final byte type) {
			
			this.type = type;
		}
	}
	
	public static final double ISP_FACTOR = 10;
	public static final double CITY_FACTOR = 8;
	public static final double STATE_FACTOR = 6;
	public static final double COUNTRY_FACTOR = 4;
	public static final double CONTINENT_FACTOR = 2;
	public static final double DEFULT_FACTOR = 0;
	
	public final static double DUMMY_LOAD = -1000*ISP_FACTOR;
	public final static double FAILED_LOAD = -ISP_FACTOR;
	private final static double GEO_POS_ADJUSTER = 0.01;
	
	private final static int STALE_FACTOR = 2000;
	private final static int MAX_ALLOWED_INTERVAL_VOILATION = 3;
	
	public final static int DOMAIN_FAIL_WAIT_MILISECOND = 300000; //5 min but get doubled in every failure
	
	private Logger log = LoggerFactory.getLogger(Node.class);
	private ConcurrentMap<String,EntryParam> EntryParamsMapper = new ConcurrentHashMap<String,EntryParam>(); 
	private ConcurrentMap<Byte,NetParams> NetParamsMapper = new ConcurrentHashMap<Byte,NetParams>();
	private ConcurrentMap<String,GeoResolver> resolvers = new ConcurrentHashMap<String,GeoResolver>();
	
	private String server = null;
	
	private Date firstReport = null;
	private long nodeLatestReport = 0;
	
	private short interval = 60;
	private int intervalMili = 60000;
	private String nodeKey;
	private boolean nodeKeyRecvd = false;
	private boolean needBackends = false;
	
	
	private List<Contact> contacts = new ArrayList<Contact>(); /* This holds all contacts for different domain
	 * to make counting down simple
	 */
	
	private Map<String, List<Contact>> contactsPerDomain = new HashMap<String, List<Contact>>();
	
	private long round = 0;
	
	private Object conCountMutext = new Object();
	
	private long latestLastModifiedForCluster = 0;
	
	public EntryParam 
	updateEntryParams(MyEntry entry,
			String domain,
			String nodeDomain,
			List<String> geoz,
			String geo_factor,
			List<ServiceInfo> srvInfoz,
			List<SystemInfo> sysInfoz,
			byte noSysPolicy,
			byte nogeo_policy,
			List<ResolveHint> resolveHints,
			final boolean isFixed, final boolean isRoundRobin, 
			final byte heartbeatType) {
		
		String apikey = entry.getApikey();
		EntryParam ep = getEntryParamsMapper().get(apikey);
		
		if( ep != null ) {
			
			if( geoz != null && geoz.size() > 0 ) {
				
				ep.getGeoParams().geoz = geoz;
			}
			//If because of a cron policy, nogeo_policy is changed, this makes sure nogeo_policy
			//is updated to new value.
			// problem is If there were no geo-nex policy, then If a cron policy removes all geo-setz then 
			//nogeo_policy stays in no_value and Balancer will use old geoz information instead of making it null.
			//this fixes above issue
			if(nogeo_policy == GeoNexTypes.update_alert_value) {
				
				ep.getGeoParams().setNoGeoPolicy(GeoNexTypes.no_value);
				ep.getGeoParams().geoz = null;
			}
			
			if(nogeo_policy > GeoNexTypes.no_value) {
				
				ep.getGeoParams().setNoGeoPolicy(nogeo_policy);
				ep.getGeoParams().geoz = null;
			}
			
			if(geo_factor!=null) {
				
				try {
					
					ep.getGeoParams().GEO_FACTOR = Double.parseDouble(geo_factor);
					
				}catch(Exception e) {
					
					ep.getGeoParams().GEO_FACTOR = 0.5;
					log.error("",e);
				}
			}
			
			ep.getNodeParams().setSrvInfoz(srvInfoz);
			ep.getNodeParams().setSysInfoz(sysInfoz);
			
			ep.setFixed(isFixed);
			ep.setRoundRobined(isRoundRobin);
			ep.setFixedNodeType(heartbeatType);
		}
		else {
			
			ep = new EntryParam(new NodeParams(srvInfoz,sysInfoz,noSysPolicy),
					new GeoParams(geoz,geo_factor,nogeo_policy),
					entry, resolveHints, isFixed, isRoundRobin, heartbeatType);
			
			getEntryParamsMapper().put(apikey,ep);
			getEntryParamsMapper().put(domain,ep);	
		}
		
		
		/* After implementing real report, uncomment this
		 * List<Contact> contacts = contactsPerDomain.get(domain);
		
		if(contacts!=null)
			Reporter.updateEvents(getServer(), 
					contacts, 
					getTimeout(), 
					ep.getNodeParams().getDownSrvz(),
					ep.getNodeParams().getLoadedSysz());*/
		
		ep.setUpdateEntryParamResault(checkNdomain(ep.getGeoParams(),nodeDomain));
		
		return ep;
	}
	
	public void updateNetParams(Byte type,Byte value) {
		
		getNetParamsMapper().put(type,new NetParams(type,value));
	}
	
	public int getNetParam(Byte type) {
		
		NetParams np = getNetParamsMapper().get(type);
		return np!=null?
				((nodeLatestReport - np.getLast().getTime()) > getTimeout()?
						100:np.getValue()):(new Date().getTime()-firstReport.getTime())>getTimeout()?
								100:0;
	}
	
	private boolean checkNdomain(GeoParams gparam,String nodeDomain) {
		
		if(nodeDomain==null || nodeDomain.equals("")) {
			
			if(gparam.getNodeDomain() == null || gparam.getNodeDomain().equals(""))
				
				gparam.setNodeDomain(getServer());
			
			return true;
		}
		
		if(gparam.getNodeDomain() == null ||
				gparam.getNodeDomain().equals("")) {
			
			if(nodeDomain.equals(getServer())) {
				
				gparam.setNodeDomain(getServer());
				return true;
			}
			else
				return false;
		}
		
		if(gparam.getNodeDomain().equals(nodeDomain))
			
			return true; 
		
		return false;
	}
	
	public boolean checkUpdateResolverContexts(MyEntry entry, List<Contact> contacts) {
		
		GeoResolver resolver = entry.getResolver();
		
		GeoResolver r = resolvers.get(resolver.getZone());
		
		boolean contactsApplied = false; 
		
		if(r != null) {
			
			/*
			 *	If this node gets a GeoResolver different than the
			 *  one it already e.g. when resources get changed in monitor, it'll
			 *  recreate all resolvers then concurrent counter would be lost
			 *  Then I let the rest of the code checks it again and replace with the new one
			*/
			if(resolver.equals(r) ) {
				
				/*
				 * If this node has already setup contacts, its ok then.
				 * because we know a node can't change its configuration
				 * without restablishing a new session
				 */
				if(contacts != null && contacts.size()>0
						&& contactsPerDomain.get(entry.getFullAName()) == null) {
					
					 List<Contact> safeContacts = resolver.checkApplyContacts(getId(), contacts);
					 
					 if(safeContacts != null) {
						 
						 contactsPerDomain.put(entry.getFullAName(), safeContacts);
						 this.contacts.addAll(safeContacts);
					 }
					
					contactsApplied = true;
				}
				
				if(resolver.getConcurentCount() <= resolver.getConcurrents()) {
					
					return true;
				}
			}

			log.debug("Saved GeoResolver {} is not equal to received one {} for the same Zone ",r,resolver);
		}
		
		if(!resolver.chkUpdatConcurents()) {
			
			return false;
		}
		
		if(!contactsApplied && contacts != null && contacts.size()>0) {
			
			 List<Contact> safeContacts = resolver.checkApplyContacts(getId(), contacts);
			 
			 if(safeContacts != null) {
				 
				 contactsPerDomain.put(entry.getFullAName(), safeContacts);
				 this.contacts.addAll(safeContacts);
			 }
		}
			
		resolvers.put(resolver.getZone(),resolver);
		
		return true;
	}
	
	/*
	 * If countDwnResolvers was called twice for this node
	 * e.g. once in Resolver#updateMappers() and once in
	 * Monitor:catchStopedNode, then dey can't count down
	 * Resolver twice, because when its called, it clears
	 * this node's resolvers 
	 */
	public void countDwnResolvers() {
		
		synchronized (conCountMutext) {
		
			for(Iterator<Entry<String,GeoResolver>> it = resolvers.entrySet().iterator();it.hasNext();) {
				
				GeoResolver resolver = it.next().getValue();
				resolver.countDown();
				resolver.countDownContact(this.contacts);
			}
			resolvers.clear();
		}
	}
	/*
	 * we do not counting down here because this only get called on Monitor
	 * When resources get modified, so wee know it will get removed in every 
	 * containing nodes
	 */
	
	public void removeResolver(String zone) {
		
		resolvers.remove(zone);
	}
	
	public Map<String,EntryParam> getEntryParamsMapper() {
		
		return EntryParamsMapper;
	}
	
	public Map<Byte,NetParams> getNetParamsMapper() {
		
		return NetParamsMapper;
	}
	
	private String generateNodeKey() {
		
		return UUID.randomUUID().toString();
	}
	
	public Node(final String server,final long id,final short version) {
		
		super(id, version);
		this.server = server;
		firstReport = new Date();
		nodeKey = generateNodeKey();
	}
	
	public Node(final String server,final short interval,final long id,final short version) {
		
		super(id, version);
		
		this.server = server;
		this.interval = interval;
		this.firstReport = new Date();
		nodeKey = generateNodeKey();
		intervalMili = interval*1000;
	}
	
	private double getFactor(final String key,final Location geo,final EntryParam ep,final double srv_raise) {
		
		NodeParams nodeParam = ep.getNodeParams();
		
		boolean isFixed = ep.isFixed();
		
		if(!isFixed && (new Date().getTime()-ep.getLastReport().getTime()>getTimeout())) {
			
			log.error("Node {} infoz are stale. this node will be dumped out very soon",server);
			return DUMMY_LOAD;
		}
		
		GeoParams gp = getGeoParams(key);
		
		if(gp == null) {
			
			log.error("Could not get GeoParams out of key {}",key);
			return DUMMY_LOAD;
		}
		
		double factor = 0;
		double GEO_FACTOR = gp.GEO_FACTOR;
		double EDGE_FACTOR = 1-GEO_FACTOR;
		
		if(!isFixed) {
			
			factor = nodeParam.getLoads();
			
			if(factor < 0 && EDGE_FACTOR<GEO_FACTOR ) {
				
				EDGE_FACTOR = GEO_FACTOR;
				GEO_FACTOR = 1-EDGE_FACTOR;
			}
		}
		
		double round_factor = 1;
		
		if(isFixed && ep.isRoundRobined()) {
			
			long now = new Date().getTime();
			round_factor = ((double)now-getRound())/now;
		}
		else if(nodeParam.isRounroubined()) {
			
			long now = new Date().getTime();
			round_factor = ((double)now-getRound())/now;
		}
		
		List<String> geoz = gp.geoz;
		
		double ret = DUMMY_LOAD;
		
		if(geoz == null || geoz.size()==0) {
			
			if(gp.noGeoPolicy >= 0) {
				
				ret = (GEO_FACTOR*gp.noGeoPolicy)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(gp.noGeoPolicyPosition+1))+round_factor;	
				return ret+(ret*srv_raise);
			}
			ret = factor+round_factor;

			return ret+(ret*srv_raise);
		}
		
		if(log.isDebugEnabled()) {
		
			log.debug("CALCULATIONS FOR {} WITH CLIENT LOCATION OF {}", server, geo);
		}
		
		if(geo.isp!=Geo2.UNKNOWN && geoz.contains(geo.isp)) {
			
			ret = (GEO_FACTOR*ISP_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf(geo.isp)+1))+round_factor;

			if(log.isDebugEnabled()) {
				
				log.debug("(GEO_FACTOR:{}*ISP_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(geo.isp):{}+1)+round_factor:{}={}",
						GEO_FACTOR,ISP_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf(geo.isp),round_factor,ret);
			}
		}
		else if(geo.city!=Geo2.UNKNOWN && geoz.contains(geo.city)) {
			
			ret = (GEO_FACTOR*CITY_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf(geo.city)+1))+round_factor;
		
			if(log.isDebugEnabled()) {
				
				log.debug("(GEO_FACTOR:{}*CITY_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(geo.city):{}+1)+round_factor:{}={}",
						GEO_FACTOR,CITY_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf(geo.city),round_factor,ret);
			}
		}
		else if(geo.region!=Geo2.UNKNOWN && geoz.contains(geo.region)) {
			
			ret = (GEO_FACTOR*STATE_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf(geo.region)+1))+round_factor;
			
			if(log.isDebugEnabled()) {
				
				log.debug("(GEO_FACTOR:{}*STATE_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(geo.region):{}+1)+round_factor:{}={}",
						GEO_FACTOR,STATE_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf(geo.region),round_factor,ret);
			}
		}
		else if(geo.country!=Geo2.UNKNOWN && geoz.contains(geo.country)) {
			
			ret = (GEO_FACTOR*COUNTRY_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf(geo.country)+1))+round_factor;
			
			if(log.isDebugEnabled()) {
			
				log.debug("(GEO_FACTOR:{}*COUNTRY_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(geo.country):{}+1)+round_factor:{}={}",
						GEO_FACTOR,COUNTRY_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf(geo.country),round_factor,ret);
			}
		}
		
		else if(geo.continent!=Geo2.UNKNOWN && geoz.contains(geo.continent)) {
			
			ret = (GEO_FACTOR*CONTINENT_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf(geo.continent)+1))+round_factor;
			
			if(log.isDebugEnabled()) {
				
				log.debug("(GEO_FACTOR:{}*CONTINENT_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(geo.continent):{}+1)+round_factor:{}={}",
						GEO_FACTOR,CONTINENT_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf(geo.continent),round_factor,ret);
			}
		}	
		else if(geoz.contains("*")) {
			
			ret = (GEO_FACTOR*DEFULT_FACTOR)+(EDGE_FACTOR*factor)+(GEO_POS_ADJUSTER/(geoz.indexOf("*")+1))+round_factor;
			
			if(log.isDebugEnabled()) {
			
				log.debug("(GEO_FACTOR:{}*DEFAULT_FACTOR:{})+(EDGE_FACTOR:{}*factor:{})+(GEO_POS_ADJUSTER:{}/(geoz.indexOf(*):{}+1)+round_factor:{}={}",
						GEO_FACTOR,DEFULT_FACTOR,EDGE_FACTOR,factor,GEO_POS_ADJUSTER,geoz.indexOf("*"),round_factor,ret);
			}
		}
		
		if(ret < 0) {
			
			return ret;	
		}
		
		return ret+(ret*srv_raise);
	}
	
	@SuppressWarnings("unused")
	private double distance(double clientLat, double clientLon, double serverLat, double serverLon, byte unit) { 
	 
		double deg2darBase = Math.PI/180;
		double theta = clientLon - serverLon; 
		double dist = Math.sin(clientLat * deg2darBase) * Math.sin(serverLat * deg2darBase) +  Math.cos(clientLat * deg2darBase) * Math.cos(serverLat * deg2darBase) * Math.cos(theta * deg2darBase); 
		dist = Math.acos(dist); 
		dist = dist  * deg2darBase; 
		double miles = dist * 60 * 1.1515;
		 
		if (unit == 'K') {
			  
			return (miles * 1.609344); 
		    
		} else if (unit == 'N') {
		  
			return (miles * 0.8684);
		} else {
			
		    return miles;
		}
	}
	
	public boolean isQuestionedSrvAlive(final String key, final byte proto, final Short port) {
		
		return isQuestionedSrvAlive(getEntryParam(key), proto, port, key);
	}
	
	private boolean isQuestionedSrvAlive(EntryParam ep, final byte proto, final Short port, String key) {
		
		if(ep == null) {
			
			log.debug("Given EntryParam with key {} is null with node {}.", key, server);
			return false;
		}
		
		NodeParams nodeparams = ep.getNodeParams();
		
		if(proto == 0 || port == 0 || ep.isFixed()) {
			
			return true;
		}
		
		if(nodeparams.getSrvInfoz()==null
				||nodeparams.getSrvInfoz().size()==0) {
			
			return false;
		}
		
		ServiceInfo srv = new ServiceInfo(port, proto, (byte)0,0);
		
		if(nodeparams.getDownSrvz()!=null
				&&nodeparams.getDownSrvz().contains(srv)) {
			
			return false;
		}
		
		int zeroSrvPortIndex = nodeparams.getSrvInfoz().indexOf(new ServiceInfo((short)0,proto,(byte)0,0));
		
		if(zeroSrvPortIndex>-1) {
			
			ServiceInfo zSrv = nodeparams.getSrvInfoz().get(zeroSrvPortIndex);
			if(zSrv.value==ServiceInfo.UP)
				return true;
		}
		
		int srvIndex = nodeparams.getSrvInfoz().indexOf(srv); 
		
		if(srvIndex < 0) {

			return false;
		}
		
		return true;
	}
	
	public double getFactor(final String key,final Location geo,final byte proto,final Short port) {
		
		EntryParam ep = getEntryParamsMapper().get(key);
		
		if(ep == null) {
			
			log.debug("There is no entry for identifire {} with node {}.",key,server);
			return DUMMY_LOAD;
		}
		
		NodeParams nodeparams = ep.getNodeParams();
		
		if(proto == 0 || port == 0 || ep.isFixed()) {
			
			return getFactor(key,geo,ep,0);	
		}
		
		if(nodeparams.getSrvInfoz()==null
				||nodeparams.getSrvInfoz().size()==0) {
			
			log.debug("Node {} has not brought any service informations for identifire {}.",server,key);
			return DUMMY_LOAD;
		}
		
		ServiceInfo srv = new ServiceInfo(port, proto, (byte)0,0);
		//I guess this is better to check down ports before than checking if really that service is defined
		if(nodeparams.getDownSrvz()!=null
				&&nodeparams.getDownSrvz().contains(srv)) {
			
			log.debug("Requested service is down with node {}.",server);
			return DUMMY_LOAD;
		}
		
		/***
		 * Puted this here because I first want to check if there is a real service defined and if it is down or not
		 * and if it is down, then don't serve, if not, then before checking if that service is really defined or not,
		 * here, first check if there is a 0 port service for that protocol, if is, then serve it, if not, then
		 * check if it is really defined or return dummy
		 */
		
		int zeroSrvPortIndex = nodeparams.getSrvInfoz().indexOf(new ServiceInfo((short)0,proto,(byte)0,0));
		
		if(zeroSrvPortIndex>-1) {
			
			ServiceInfo zSrv = nodeparams.getSrvInfoz().get(zeroSrvPortIndex);
			if(zSrv.value==ServiceInfo.UP)
				return getFactor(key,geo,ep,zSrv.raise);
		}
		
		int srvIndex = nodeparams.getSrvInfoz().indexOf(srv); 
		
		if(srvIndex < 0) {
			
			log.debug("Requested service is not defined in node {} for identifire {}.",server,key);
			return DUMMY_LOAD;
		}
		
		ServiceInfo realSrv = nodeparams.getSrvInfoz().get(srvIndex);
		return getFactor(key,geo,ep,realSrv.raise);
	}
	
	public boolean isHealthy(final String key) {
		
		NodeParams np = getNodeParams(key);
		if(np==null)
			return true;
		if(np.getSrvInfoz()==null)
			return true;
		if(np.getSrvInfoz().size()==0)
			return true;
		if(np.getDownSrvz().size()>0)
			return false;
		return true;
	}
	
	public NodeParams getNodeParams(final String api) {
		
		EntryParam ep = getEntryParamsMapper().get(api);
		return ep!=null?ep.getNodeParams():null;
	}
	
	public GeoParams getGeoParams(final String api) {
		
		EntryParam ep = getEntryParamsMapper().get(api);
		return ep!=null?ep.getGeoParams():null;
	}
	
	public EntryParam getEntryParam(final String identifire) {
		
		return getEntryParamsMapper().get(identifire);
	}
	
	public List<ServiceInfo> getSrvInfoz(final String api) {
		
		try {
			
			return getEntryParamsMapper().get(api).getNodeParams().getSrvInfoz();
		}
		catch(NullPointerException e) {
			
			return null;
		}
	}
	
	public List<SystemInfo> getSysInfoz(final String api) {
		
		try {
			
			return getEntryParamsMapper().get(api).getNodeParams().getSysInfoz();
		}
		catch(NullPointerException e) {
			
			return null;
		}
	}
	
	public String getServer() {
		
		return server;
	}
	
	
	
	public short getInterval() {
		
		return interval;
	}
	
	public void setInterval(final short interval) {
		
		this.interval = interval;
	}
	
	public boolean isNeedBackends() {
		
		return needBackends;
	}
	
	public void setNeedBackends(final boolean needBackends) {
		
		this.needBackends = needBackends;
	}
	
	public String getNodeKey() {
		
		return nodeKey;
	}
	
	public void setNodeKey(String nodeKey) {
		
		this.nodeKey = nodeKey;
	}
	
	public boolean isNodeKeyRecvd() {
		
		return nodeKeyRecvd;
	}
	
	public void setNodeKeyRecvd(final boolean nodeKeyRecvd) {
		
		this.nodeKeyRecvd = nodeKeyRecvd;
	}
	
	public int getTimeout() {
		
		return getInterval()*STALE_FACTOR;
	}
	
	public List<Contact> getAllContacts() {
		
		return this.contacts;
	}
	
	@Override
	public int hashCode() {
		
		/*
		 * TODO should not consider
		 * id too?
		 */
		
		if(this.getServer()!=null)
			
			return this.getServer().hashCode();
		
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(this.getServer()!=null&&obj instanceof Node)
			
			return this.getServer().equals(((Node)obj).getServer());
		
		return false;
	}
	
	public boolean isFixed(String idOrDomain) throws NotFoundException {
		
		EntryParam ep = getEntryParamsMapper().get(idOrDomain);
		
		if(ep != null) {
			
			return ep.isFixed();
		}
		
		throw new NotFoundException(idOrDomain+" is not found for node "+server);
	}
	
	public boolean isRoundRobined(String idOrDomain) throws NotFoundException {
		
		EntryParam ep = getEntryParamsMapper().get(idOrDomain);
		
		if(ep != null) {
			
			return ep.isRoundRobined();
		}
		
		throw new NotFoundException(idOrDomain+" is not found for node "+server);
	}
	
	public void updateRound() {
		
		this.round = new Date().getTime();
	}
	
	public long getRound() {
		
		return round;
	}

	public long getLatestLastModifiedForCluster() {
		
		return latestLastModifiedForCluster;
	}

	public void setLatestLastModifiedForCluster(long latestLastModifiedForCluster) {
		
		this.latestLastModifiedForCluster = latestLastModifiedForCluster;
	}
	
	@Override
	public String toString() {

		return server;
	}

	public long getNodeLatestReport() {
		return nodeLatestReport;
	}
}
