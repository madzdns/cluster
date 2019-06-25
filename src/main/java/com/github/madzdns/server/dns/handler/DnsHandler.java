package com.github.madzdns.server.dns.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.github.madzdns.server.dns.DnsResolver;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.api.net.NetProvider;

public class DnsHandler extends IoHandlerAdapter{

	Logger log = LoggerFactory.getLogger(DnsHandler.class);
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		if(message instanceof DnsResolver) {
			
			DnsResolver dns = (DnsResolver) message;
			session.write(dns.setupResponse
					(((InetSocketAddress)session.getLocalAddress()).getAddress().getHostAddress(),
							((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress()));	
		}
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
	@Override
	public void messageSent(IoSession session, Object message) throws Exception {
		
	}
}
