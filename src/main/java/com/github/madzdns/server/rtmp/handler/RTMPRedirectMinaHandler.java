package com.github.madzdns.server.rtmp.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.IoSession;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.RTMPMinaIoHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.rtmpt.RTMPTConnectionMinaFilter;
import com.github.madzdns.server.core.api.ProtoInfo;
import com.github.madzdns.server.core.api.net.NetProvider;
import com.github.madzdns.server.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.server.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.server.core.config.Config;

public class RTMPRedirectMinaHandler extends RTMPMinaIoHandler {
	
	private Logger log = LoggerFactory.getLogger(RTMPRedirectMinaHandler.class);

	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		//Boolean isRtmptHasSetBefore = (Boolean)session.getAttribute(RTMPTMinaFilter.RTMPT_FLAG);
		//if(isRtmptHasSetBefore == null || !isRtmptHasSetBefore) {
		
		session.getFilterChain().addFirst(RTMPTConnectionMinaFilter.RTMPT_MINAFILTER, new RTMPTConnectionMinaFilter());
		
		if(Config.getApp().isServSSLEnabled()) {
			
			SSLContext ssl = NetProvider.getServerTLSContext(false);
			
			if(ssl!=null) {
				
				//we use incomming port as the default port number
				final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
				MinaSslFilter sslFilter = new MinaSslFilter(ssl, session, new ProtoInfo(ServiceInfo.TCP, ProtoInfo.RTMP_SRV, DEFAULT_PORT_NUM),false, true);
				session.getFilterChain().addFirst(MinaSslFilter.NAME, sslFilter);
			}
		}
	}
	
	public void afterSessionCreated(IoSession session) {
		
		// create a connection
		RTMPMinaConnection conn = createRTMPMinaConnection();

		// add session to the connection
		conn.setIoSession(session);
		// add the handler
		conn.setHandler(handler);
		// add the connections session id for look up using the connection manager
		session.setAttribute(RTMPConnection.RTMP_SESSION_ID, conn.getSessionId());
		// add the in-bound handshake
		session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, new InboundHandshake());
	}
	
	@Override
	public void sessionOpened(IoSession session) throws Exception {
		
	}
	
	public void baseSessionOpend(IoSession session) throws Exception {

		super.sessionOpened(session);
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