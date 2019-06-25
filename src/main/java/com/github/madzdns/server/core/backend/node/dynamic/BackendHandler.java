package com.github.madzdns.server.core.backend.node.dynamic;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLContext;

import com.github.madzdns.server.core.backend.node.INode;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.ClusterSnapshot;
import com.frfra.frsynch.ClusterNode;

import com.github.madzdns.server.core.backend.BackendNode;
import com.github.madzdns.server.core.api.ProtoInfo;
import com.github.madzdns.server.core.api.Resolver;
import com.github.madzdns.server.core.api.net.NetProvider;
import com.github.madzdns.server.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.server.core.backend.frsynch.Frsynch;
import com.github.madzdns.server.core.backend.node.dynamic.codec.BackendMinaDecoder;
import com.github.madzdns.server.core.backend.node.dynamic.codec.BackendMinaEncoder;
import com.github.madzdns.server.core.backend.node.dynamic.impl.Node;
import com.github.madzdns.server.core.codec.ServerCodecFactory;

public class BackendHandler extends IoHandlerAdapter{
	
	private Logger log = LoggerFactory.getLogger(BackendHandler.class);
	
	private final static String CLOSE_LISTNER_STATE = new StringBuffer()
		.append(BackendHandler.class.getName()).append("STAT.key").toString() ;
	
	public final static String SESSION_CLUSTER_LAST_MODIFIED = new StringBuffer()
		.append(BackendHandler.class.getName()).append("clusterPerSess").toString();
	
	public final static String MY_EDGE_INFO = new StringBuffer()
	.append(BackendHandler.class.getName()).append("myEdje").toString();
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		SSLContext ssl = NetProvider.getServerTLSContext(true);
		
		if(ssl!=null) {
			
			//we use incomming port as the default port number
			final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
			MinaSslFilter sslFilter = new MinaSslFilter(ssl, session, new ProtoInfo(ServiceInfo.TCP, "BACKEND", DEFAULT_PORT_NUM),false);
			
			session.getFilterChain().addLast(MinaSslFilter.NAME, sslFilter);
		}
		
		session.getFilterChain().addLast("backend_coder",
				new ProtocolCodecFilter(
						new ServerCodecFactory(new BackendMinaDecoder()
						,new BackendMinaEncoder())));
		
		session.setAttribute(SESSION_CLUSTER_LAST_MODIFIED, 0L);
		
	}
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		
		if(message instanceof INFOMessage) {
			
			INFOMessage info = (INFOMessage) message;
			
			if(Resolver.checkBlackList(info.getSrc())) {
				
				log.warn("Rejecting node {} due to blacklist",info.getSrc());
				
				NetProvider.closeMinaSession(session, true);
				return;
			}
			
			INode node = Resolver.updateMappers(
					info.getSrc()
					,info.getKey()
					,info.getGeomaps()
					,info.getSysInfoz()
					,info.getSrvInfoz()
					,info.getContacts()
					,info.getDomain()
					,info.getNdomain()
					,info.getInterval()
					,info.isAuto()
					,info.isNeedBackends()
					,info.getNoSys()
					,session.getId()
					,info.getGfactor()
					,false
					,null
					,false
					,info.getNoGeo()
					,info.getVersion()
					,info.getResolveHints()
					,null);
			
			Node e = (Node) node;
			
			INFOResponse response = createResponse(e,info.getKey(),info.getDomain(),info.isAuto(),session);
			
			if(response==null) {
				
				log.warn("Rejecting node {} due to wrong handshake or impropper resource",info.getSrc());
				
				NetProvider.closeMinaSession(session, true);
				
				return;
			}
			//TODO what if current edge info changed? like report interval? 
			ClusterNode me = (ClusterNode)session.getAttribute(MY_EDGE_INFO);
			
			if( me == null ) {
				
				//TODO if this is a kafka cluster this'll produce null pointer
				me = Frsynch.getContext().getMyInfo();
				session.setAttribute(MY_EDGE_INFO, me);
			}
					
			
			/*
			 *  Checking report interval here is because
			 *  if report interval is not zero, then
			 *  purging nodes is done in report specific
			 *  codes
			 */
			if(response.getType()!=INFOResponse.RSP_TYPE_ERR
					&& response.getType()!= INFOResponse.RSP_TYPE_ERR_NOFATTAL
					&&session.getAttribute(CLOSE_LISTNER_STATE)==null
					&&(me == null || me.getReportInterval()==0)) {
				
				int idletimeout = (e.getTimeout()/1000) + 1;
				
				log.debug("{} seconds idle timeout registered for node {}",idletimeout,info.getSrc());
				
				session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE,idletimeout);
				
				session.getCloseFuture().addListener(new MinaCloseCallBak(info.getSrc()));
				
				session.setAttribute(CLOSE_LISTNER_STATE);
			}
			
			session.write(response);
			
			if(response.getType()==INFOResponse.RSP_TYPE_ERR) {
				
				log.warn("Rejecting node {} due to error status",info.getSrc());
				
				NetProvider.closeMinaSession(session,true);
			}
		}
	}
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		
		if(cause instanceof IOException) {

			log.error("{} by {}",cause.getMessage(), ((InetSocketAddress)session.getRemoteAddress())
					.getAddress().getHostAddress());
			return;
		}
		
		log.error("",cause);
		
		NetProvider.closeMinaSession(session,true);
	}
	@Override
	public void sessionIdle(IoSession session, IdleStatus status)
			throws Exception {
		
		log.debug("Session {}:{} was idle for max idle time. Closing gets in process ...",
				session.getId(),
				((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress());
		
		/*
		 * We close here and let closeHandler do the next job
		 */
		NetProvider.closeMinaSession(session,true);
	}
	
	private INFOResponse createResponse(Node e,String key,String domain,boolean is_auto, IoSession session) {
		
		if(e == null) {

			return null;
		}
		
		INFOResponse response = new INFOResponse();
		
		response.setKey(key);
		
		response.setType(e.getStatus());
		
		if(e.getStatus()==INFOResponse.RSP_TYPE_ERR || e.getStatus() == INFOResponse.RSP_TYPE_ERR_NOFATTAL) {

			response.setErrBody(e.getErrBody());
			return response;
		}
		
		response.setDomain(domain);
		
		if(!e.isNodeKeyRecvd())
			
			response.setNodeKey(e.getNodeKey());
		
		Node.GeoParams egparams = e.getGeoParams(domain);
		
		if(egparams!=null
				&&((egparams.getGeoz()!=null&&egparams.getGeoz().size()>0)
						||egparams.getNoGeoPolicy()>=0)) {
			
			response.setNeedGeoz(false);
			
			if(is_auto)
			
				response.setGeomaps(egparams.getGeoz());
		}
		else {
			
			log.error("ERROR geoz entry is empty. Requesting to send geoz");
			
			response.setNeedGeoz(true);
		}
		
		//TODO if it is not Frsynch cluster, a null pointer exception
		long last_modified = Frsynch.getContext().getClusterLastModified();
		
		long clusterLastModifiedForSession = (long) session.getAttribute(SESSION_CLUSTER_LAST_MODIFIED);
		
		if(e.isNeedBackends() || last_modified > clusterLastModifiedForSession) {
			
			session.setAttribute(SESSION_CLUSTER_LAST_MODIFIED,last_modified);
			
			ClusterSnapshot snapshot = Frsynch.getContext().getSnapshot();

			List<ClusterNode> backends = snapshot.getCluster();
			
			List<BackendNode> backendNodes = null;
			
			if(backends != null && backends.size()>0) {
				
				backendNodes = new ArrayList<BackendNode>();
				
				for(Iterator<ClusterNode> it = backends.iterator(); it.hasNext(); ) {
					
					ClusterNode n = it.next();
					backendNodes.add(new BackendNode(n.getId(), n.getName(), n.getBackendAddresses(), n.isValid(), n.getLastModified()));
				}
				
				response.setEdges(backendNodes);
			}
			
			backends = null;
		}
		return response;
	}
}
