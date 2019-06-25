package server.backend.node.statics;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.ClusterNode;
import com.frfra.frsynch.ClusterSnapshot;

import server.api.FRfra;
import server.api.Resolver;
import server.backend.BackendAddress;
import server.backend.BackendNode;
import server.backend.frsynch.Frsynch;
import server.backend.node.BaseNodeV1;
import server.backend.node.INode;
import server.backend.node.dynamic.INFOResponse;
import server.backend.node.dynamic.impl.Node;
import server.backend.node.statics.impl.HostNode;
import http.web.WebController;
import http.web.annotation.ContentType;
import http.web.annotation.RequestParam;
import http.web.annotation.RouteMap;

@RouteMap("/backend")
public class RequestController extends WebController {
	
	private static Logger log = LoggerFactory.getLogger(RequestController.class);

	@ContentType("text/html")
	@RouteMap({"/",""})
	public String greeting() {
		
		return "Hi "+FRfra.getServerVersion();
	}
	
	@ContentType("text/xml")
	@RouteMap("/heartbeat")
	public String postHeartBeat(@RequestParam(name="domain") String domain,
			@RequestParam(name="key") String key,
			@RequestParam(name="ip", isNullable=true) String ip) {
		
		if(ip == null || ip.equals("")) {
			
			ip = this.getRemote().getAddress().getHostAddress();
		}
		
		StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		
		if(Resolver.checkBlackList(ip)) {
			
			log.warn("Rejecting node {} due to blacklist for domain {}",ip, domain);
			return sb.toString();
		}
		
		INode node = Resolver.updateMappers(ip, key, null, null, null, null, domain, null, 
				(short)-1, false, false, (byte)0, -1L, null, true, null,
				false, (byte)0, (short)1, null, null);
		
		BaseNodeV1 n = (BaseNodeV1) node;
		
		if(n == null || (n.getStatus() == INFOResponse.RSP_TYPE_ERR ||
				n.getStatus() == INFOResponse.RSP_TYPE_ERR_NOFATTAL)) {

			sb.append("<responce state=\"error\">");
			sb.append("<error>");
			if(n != null) sb.append(n.getErrBody());
			sb.append("</error>");
			sb.append("</responce>");
			return sb.toString();
		}
		
		HostNode e = (HostNode) node;
		
		sb.append("<response state=\"valid\">");
		
		long last_modified = Frsynch.getContext().getClusterLastModified();
		
		Node underlyingNode = ((Node)e.getUnderliyingNode());
		
		if( e.isNeedBackends() || (underlyingNode != null && last_modified > underlyingNode.getLatestLastModifiedForCluster())) {
			
			if(underlyingNode != null) {
			
				underlyingNode.setLatestLastModifiedForCluster(last_modified);
			}
			
			ClusterSnapshot snapshot = Frsynch.getContext().getSnapshot();
			
			List<ClusterNode> edges = snapshot.getCluster();
			
			if(edges != null && edges.size()>0) {
			
				sb.append("<backend-set>");
				
				for(Iterator<ClusterNode> eit = edges.iterator();eit.hasNext();) {
					
					final ClusterNode cn = eit.next();
					
					BackendNode b = new BackendNode(cn.getId(), cn.getName(), cn.getBackendAddresses(), cn.isValid(), cn.getLastModified());
					
					final Set<BackendAddress> addrz = b.getBackendAddrzForStaticNodes();
					
					if(addrz!=null && addrz.size() > 0 ) {
						
						sb.append("<backend valid=\"");
						sb.append(b.isValid()?"true":"false");
						sb.append("\" name=\"");
						sb.append(b.getName());
						sb.append("\" modified=\"");
						sb.append(b.getLastModified());
						sb.append("\" >");
						
						for(Iterator<BackendAddress> it = addrz.iterator();it.hasNext();) {
							
							final BackendAddress clusterAddr = it.next();
							
							sb.append("<connection>");
							sb.append("<ip>");
							sb.append(clusterAddr.getAddress().getHostAddress());
							sb.append("</ip>");
							sb.append("<port>");
							sb.append((short)clusterAddr.getPort());
							sb.append("</port>");
							sb.append("</connection>");
						}
						
						sb.append("</backend>");
					}
				}
				
				sb.append("</backend-set>");
			}
		}
		
		sb.append("</response>");

		return sb.toString();
	}
}
