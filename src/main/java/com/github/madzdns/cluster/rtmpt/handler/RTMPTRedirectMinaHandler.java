package com.github.madzdns.cluster.rtmpt.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.session.IoSession;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.rtmp.handler.RTMPRedirectMinaHandler;
import com.github.madzdns.cluster.core.api.net.NetProvider;

public class RTMPTRedirectMinaHandler extends RTMPRedirectMinaHandler {

	private Logger log = LoggerFactory.getLogger(RTMPTRedirectMinaHandler.class);
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		log.debug("Session created");

		//add rtmpe filter
		session.getFilterChain().addLast("rtmpeFilter", new RTMPEIoFilter());
		//add protocol filter next

		//session.getFilterChain().addLast("protocolFilter", new ProtocolCodecFilter(codecFactory));

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
