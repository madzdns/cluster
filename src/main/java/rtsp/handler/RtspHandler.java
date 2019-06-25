package rtsp.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.api.ProtoInfo;
import server.api.net.NetProvider;
import server.api.net.ssl.server.filter.MinaSslFilter;
import server.backend.node.dynamic.ServiceInfo;
import server.codec.ServerCodecFactory;
import server.config.Config;
import rtsp.RTSPNegotiation;
import rtsp.RTSPRequest;
import rtsp.codec.RTSPMinaDecoder;
import rtsp.codec.RTSPMinaEncoder;

public class RtspHandler extends IoHandlerAdapter{
	
	private RTSPNegotiation responder = null;
	
	private Logger log = LoggerFactory.getLogger(RtspHandler.class);
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		responder = new RTSPNegotiation();
		
		if(Config.getApp().isServSSLEnabled()) {
			
			SSLContext ssl = NetProvider.getServerTLSContext(false);
			
			if(ssl != null) {
				
				//we use incomming port as the default port number
				final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
				MinaSslFilter sslFilter = new MinaSslFilter(ssl, session, new ProtoInfo(ServiceInfo.TCP, ProtoInfo.RTSP_SRV, DEFAULT_PORT_NUM),false, true);
				session.getFilterChain().addLast(MinaSslFilter.NAME, sslFilter);
			}
		}
		
		session.getFilterChain().addLast("rtsp_coder",new ProtocolCodecFilter(new ServerCodecFactory(new RTSPMinaDecoder(),new RTSPMinaEncoder())));
	}
	
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		
		if(message instanceof RTSPRequest) {
			
			session.write(responder.make(session,(RTSPRequest)message).toByteBuffer());
		}
	}
	
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		
		if(cause instanceof IOException) {

			InetSocketAddress p = ((InetSocketAddress)session.getRemoteAddress());
			log.error("{} by {}:{}",cause.getMessage(), 
					p.getAddress().getHostAddress(), p.getPort());
			return;
		}
		
		log.error("",cause);
		
		NetProvider.closeMinaSession(session,true);
	}
}
