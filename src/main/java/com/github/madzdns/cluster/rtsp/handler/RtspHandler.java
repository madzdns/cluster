package com.github.madzdns.cluster.rtsp.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import com.github.madzdns.cluster.rtsp.RTSPNegotiation;
import com.github.madzdns.cluster.rtsp.RTSPRequest;
import com.github.madzdns.cluster.rtsp.codec.RTSPMinaDecoder;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.api.ProtoInfo;
import com.github.madzdns.cluster.core.api.net.NetProvider;
import com.github.madzdns.cluster.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.cluster.core.codec.ServerCodecFactory;
import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.rtsp.codec.RTSPMinaEncoder;

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
