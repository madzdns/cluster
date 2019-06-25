package server.wsi;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.ClusterSnapshot;
import com.frfra.frsynch.ClusterNode;
import com.frfra.frsynch.SynchFeature;
import com.frfra.frsynch.SynchType;
import com.frfra.frsynch.ClusterNode.ClusterAddress;

import server.api.FRfra;
import server.api.Resolver;
import server.backend.MetaData;
import server.backend.ResourceCtrl;
import server.backend.frsynch.Frsynch;
import server.backend.frsynch.StartupManager;
import server.backend.frsynch.ZoneSynchCallback;
import server.backend.frsynch.ZoneSynchMessage;
import server.backend.ZoneSynch;
import server.backend.kafka.KafkaTopics;
import server.backend.kafka.codec.ClusterMessage;
import server.backend.kafka.codec.ResourceMessage;
import server.backend.kafka.producer.KafkaProducerImpl;
import server.backend.node.INode;
import server.config.Config;
import server.config.GeoResolver;
import server.config.Resource;
import server.wsi.model.EdgeModel;
import server.wsi.model.EdgeModel.AddressPort;
import http.web.WebController;
import http.web.annotation.ContentType;
import http.web.annotation.RequestParam;
import http.web.annotation.RouteMap;

@RouteMap("/wsi")
public class Admin extends WebController{
	
	private Logger log = LoggerFactory.getLogger(Admin.class);
	
	private boolean checkAthorization(short id,String pass, String methodName) {
		
		log.debug("Connection info	Remote={}, Local={}, Asked by={}",getRemote(), getLocal(), methodName);
		
		try {
			
			short ID = id;
			
			if(ID != Config.getApp().getId()) {
				
				return false;
			}
			
			ClusterNode me = Frsynch.getContext().getMyInfo();
			
			if(me == null)
				
				return true;
			
			if(me.getKey().equals(pass))
				
				return true;
			
			log.error("My key was [{}] Rcvd key was [{}]", me.getKey(), pass);
			
			return false;
		}
		catch(Exception e) {
			
			log.error("",e);
			return false;
		}
	}
	
	@ContentType("text/html")
	@RouteMap({"/",""})
	public String greeting() {
		
		return "Hi "+FRfra.getServerVersion();
	}
	
	@ContentType("text/html")
	@RouteMap("/post/single/resource")
	public String postResource(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password,
			@RequestParam(name="res_id", isNullable=true) String resId,
			@RequestParam(name="content", isNullable=true) String res,
			@RequestParam(name="state") boolean _state) {
		
		if(!checkAthorization(id,password, "postResource"))
			
			return "Wrong authentication";
		
		log.debug("rcvd contents for {} are: {}",resId,  res);
		
		if(resId == null || res == null)
			
			return "Parameters should not be empty";
			
		try {
			
			byte state = _state?ResourceCtrl.STATE_VLD:ResourceCtrl.STATE_DEL;
			
			if(Config.getApp().isKafkaCluster()) {

				ResourceMessage message = new ResourceMessage(res.getBytes(), resId, new Date().getTime(),
						Config.getApp().getId(), state);
				
				KafkaProducerImpl.send(KafkaTopics.RESOURCES, resId, message);
				
				return "OK";
			}
			
			log.info("Received synch request to {} recource {} from cluster", _state?"update":"delete", resId);
			
			long version = new Date().getTime();
			
			ZoneSynch.updateZoneSynch(resId, version, res.getBytes(), state);
			
			ZoneSynchMessage message = new ZoneSynchMessage(res.getBytes(), resId, version, 
					_state?ZoneSynchMessage.COMMAND_TAKE_THis:ZoneSynchMessage.COMMAND_DEL_THis, (byte)0);
			
			SynchFeature feature = Frsynch.getContext().make(SynchType.RING)
			.withoutCluster(Config.getApp().getId())
			.withCallBack(new ZoneSynchCallback())
			.withEncoder(ZoneSynchMessage.class)
			.synch(message)
			.get();
			
			if(feature != null && 
					feature.get(resId).isSuccessful()) {
				
				return "OK";
			}
				
			log.info("feature[{}] = {}", resId, feature == null? "null":feature.get(resId).isSuccessful());
			
			return "Synch_Failed";
			
		} catch (Exception e) {
			
			log.error("",e);
			
			return "Failed";
		}
	}
	
	@ContentType("application/xml")
	@RouteMap("/get/all/resource")
	public String getResources(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password) {
		
		if(!checkAthorization(id, password, "getResources"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><zones auth=\"wrong\"></zones>";
		
		StringBuilder buffer = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
		.append("<zones auth=\"ok\">");
		
		
		List<MetaData> zones = ZoneSynch.getMetasListSynch();
		
		if(zones!=null) {
			
			for(int i=0;i<zones.size();i++) {
				
				buffer.append(zones.get(i).toXmlString());
			}
		}
		
		buffer.append("</zones>");
		
		return buffer.toString();
	}
	
	@ContentType("application/xml")
	@RouteMap("/get/single/resource")
	public String getResource(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password,
			@RequestParam(name="res_id", isNullable = true) String resIs) {
		
		if(!checkAthorization(id,password, "getResource"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><zone auth=\"wrong\"></zone>";
		
		if(resIs == null)
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><zone auth=\"ok\">Parameters should not be empty</zone>";
		
		MetaData _zone = ZoneSynch.getMetaDataSynch(resIs);
		
		if(_zone == null)
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><zone auth=\"ok\"></zone>";
		
		return new String(_zone.getResolver().getContent());
	}
	
	@ContentType("application/xml")
	@RouteMap("/get/all/cluster")
	public String getCluserInfo(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password) {
		
		if(!checkAthorization(id,password, "getCluserInfo"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><edges auth=\"wrong\"></edges>";
		
		ClusterSnapshot snapshot = Frsynch.getContext().getSnapshot();
		
		if(snapshot == null)
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><edges auth=\"ok\"></edges>";
		
		List<ClusterNode> cluster = snapshot.getCluster();
		
		if(cluster == null) {
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><edges auth=\"ok\"></edges>";
		}
		
		StringBuilder buf = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
		.append("<edges auth=\"ok\">");
		
		for(int i=0; i<cluster.size();i++) {
			
			buf.append(cluster.get(i).toXmlString());
		}
		
		return buf.append("</edges>").toString();
	}
	
	@ContentType("application/xml")
	@RouteMap("/get/single/cluster")
	public String getCluser2Info(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password,
			@RequestParam(name="cid") short cid) {
		
		if(!checkAthorization(id, password, "getCluser2Info"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><edge auth=\"wrong\"></edge>";
		
		StringBuilder buf = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
		.append("<edge auth=\"ok\">");
		
		ClusterNode e = Frsynch.getContext().getFrNodeById(cid);
		
		String edge = "";
		
		if(e != null)
			
			edge = e.toXmlString();
		
		buf.append(edge);
		
		return buf.append("</edge>").toString();
	}
	
	@ContentType("text/html")
	@RouteMap("/reset/keychain")
	public String secureCluserKey() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<form action=\"/wsi/post/single/key\" method=\"post\" >")
		.append("ID <input type=\"text\" name=\"id\" /><br />")
		.append("Pass <input type=\"password\" name=\"pass\" autocomplete=\"off\" /><br />")
		.append("new pass <input type=\"password\" name=\"new_pass\" autocomplete=\"off\" /><br />")
		.append("<input type=\"submit\" value=\"send\" /></form>");
		
		return sb.toString();
	}
	
	@ContentType("text/html")
	@RouteMap("/post/single/key")
	public String postCluserKey(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password,
			@RequestParam(name="new_pass") String newPass) {
		
		if(!checkAthorization(id,password, "postCluserKey"))
			
			return "Wrong_Authentication";
		
		try {

			Frsynch.getContext().resetFrNodeKeyById(id, newPass);
			
			return "OK";
		}
		catch(Exception e) {
			
			return e.getMessage();
		}
		
	}
	
	@ContentType("text/html")
	@RouteMap("/post/single/cluster")
	public String postCluserInfo(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password,
			@RequestParam(name="edges") List<EdgeModel> edges) {
		
		if(!checkAthorization(id,password, "postCluserInfo"))
			
			return "Wrong authentication";
		
		if(edges == null || edges.size() == 0)
			
			return "FAIL";
		
		Set<Short> ids = new HashSet<Short>();
		
		for(int i=0; i<edges.size(); i++) {
			
			EdgeModel e = edges.get(i);
			
			if(ids.contains(e.getId()))
				
				continue;
			
			Set<ClusterAddress> clusterAddrzSet = null;
			
			Set<ClusterAddress> backendAddrzSet = null;
			
			if(e.getClusterAddrz() != null) {
				
				clusterAddrzSet = new HashSet<ClusterAddress>();
				
				for(Iterator<AddressPort> it= e.getClusterAddrz().iterator();it.hasNext();) {
					AddressPort ap = it.next();
					
					clusterAddrzSet.add(new ClusterAddress(ap.getAddress(), ap.getPort()));
				}
			}
			
			if(e.getBackendAddrz()!=null) {
				
				backendAddrzSet = new HashSet<ClusterAddress>();
				
				for(Iterator<AddressPort> it= e.getBackendAddrz().iterator();it.hasNext();) {
					
					AddressPort ap = it.next();
					
					backendAddrzSet.add(new ClusterAddress(ap.getAddress(), ap.getPort()));
				}
			}
			
			String key = "";
			
			if(e.getKey()!=null)
				
				key = e.getKey();
			
			ClusterNode edge = new ClusterNode(e.getId(), 
					clusterAddrzSet, backendAddrzSet,
					e.isUseSsl(), e.isAuthByKey(), 
					key, new Date().getTime(),
					null, e.getState()==true?ClusterNode.STATE_VLD:ClusterNode.STATE_DEL,
							e.getMonitorDelay(),
							e.getMonitorInterval(),e.getReportDelay(),
							e.getReportInterval());
			
			log.info("Received Node:{}", edge);
			
			try {
				
				if(Config.getApp().isKafkaCluster()) {
					
					ClusterMessage message = new ClusterMessage(edge.getId(), edge.isUseSsl(), edge.isAuthByKey(),
							edge.getKey(), edge.getLastModified(), edge.getSynchAddresses(),
							edge.getBackendAddresses(), edge.getState(),
									edge.getMonitorInterval(), edge.getMonitorDelay(), edge.getReportInterval(),
									edge.getReportDelay(), Config.getApp().getId());
					
					KafkaProducerImpl.send(KafkaTopics.CLUSTER
							, String.valueOf(edge.getId()), message);
					
					return "OK";
				}
				
				if(Frsynch.getContext().synchCluster(edge, SynchType.RING)) {
					
					new StartupManager().startSynchingResourcesToCluster(Frsynch.getContext(), edge.getId());
					
					return "OK";
				}
				
				return "Synch_Failed";
				
			} catch (Exception e2) {
				
				log.error("",e2);
			}
		}

		return "Failed";
	}
	
	
	@ContentType("application/xml")
	@RouteMap("/get/all/loaded/resource")
	public String getLoadedResources(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password) {
		
		if(!checkAthorization(id,password, "getLoadedResources"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><zones auth=\"wrong\"></zones>";
		
		Map<String, Resource> resZ = Config.getZoneResz(null);
		
		StringBuilder b  = new StringBuilder()
						.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
						.append("<resources auth=\"ok\">");
		
		if(resZ == null) {
			
			return b.append("</resources>").toString();
		}
		
		for(Iterator<Resource> rIt = resZ.values().iterator(); rIt.hasNext();) {
			
			Resource r = rIt.next();
			
			b.append("<resource id=\"").append(r.getId()).append("\"");
			
			for(Iterator<GeoResolver> sIt = r.getGeoResolvers().iterator(); sIt.hasNext();) {
				
				GeoResolver zone = sIt.next();
				
				b.append("<zone>")
				.append(zone.getZone())
				.append("</zone>");
			}
			
			b.append("</resource>");
		}
		
		
		
		return b.append("</resources>").toString();
	}
	
	
	@ContentType("application/xml")
	@RouteMap("/get/all/connected/node")
	public String getConnectedNodes(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password) {
		
		
		if(!checkAthorization(id,password, "getConnectedNodes"))
			
			return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><nodes auth=\"wrong\"></nodes>";

		Map<String, INode> resZ = Resolver.getMapperNode();
		
		StringBuilder b = new StringBuilder()
						.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
						.append("<nodes auth=\"ok\">");
		
		if(resZ == null) {
			
			return b.append("</nodes>").toString();
		}
		
		for(Iterator<String> sIt = resZ.keySet().iterator(); sIt.hasNext();) {
			
			String nodeIp = sIt.next();
			
			b.append("<node>")
			.append(nodeIp)
			.append("</node>");
		}
		
		return b.append("</nodes>").toString();
	}
	
	@ContentType("text/html")
	@RouteMap("/get/geo")
	public String getGeoForIP(@RequestParam(name="ip") String ip) {
		
		
		if(ip == null) {
			
			return "NOT FOUND";
		}
		
		return Resolver.testGeo(ip).toString();
	}
	
	@ContentType("text/html")
	@RouteMap("/get/replica")
	public String askForReplication(@RequestParam(name="id") short id,
			@RequestParam(name="pass") String password) {
		
		if(!checkAthorization(id,password, "askForReplication"))
			
			return "wrong";
		
		ZoneSynchMessage message = new ZoneSynchMessage(null, "ask_replication", -1, 
				(byte) 0, (byte)0);
		
		SynchFeature sf = Frsynch.getContext()
				.make(SynchType.UNICAST_ONE_OF)
				.withoutCluster(Config.getApp().getId())
				.withCallBack(new ZoneSynchCallback())
				.withEncoder(ZoneSynchMessage.class)
				.synch(message).get();
		
		if(sf != null && sf.get("ask_replication").isSuccessful()) {
			
			return "ok";
		}
		
		return "false";
	}
}
