package com.github.madzdns.server.dns;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.utils.NetHelper;
import com.github.madzdns.server.core.config.Bind;
import com.github.madzdns.server.core.config.Socket;
import com.github.madzdns.server.core.codec.ServerCodecFactory;
import com.github.madzdns.server.dns.codec.DnsMinaDecoder;
import com.github.madzdns.server.dns.codec.DnsMinaEncoder;
import com.github.madzdns.server.dns.handler.DnsHandler;

public class DnsServer {
	
	
	public static Set<InetAddress> dnsBoundAddresses; 
	
	private List<Socket> sockets = null;
	private Logger log;
	
	public DnsServer(Bind bind) {
		
		log = LoggerFactory.getLogger(DnsServer.class);
		this.sockets = bind.getSockets();
	}
	
	public void start() throws IOException {
		
		dnsBoundAddresses = new HashSet<InetAddress>();
		
		NioDatagramAcceptor socket = new NioDatagramAcceptor();
		socket.getFilterChain().addLast("dns_coder",
				new ProtocolCodecFilter(
						new ServerCodecFactory(new DnsMinaDecoder(),new DnsMinaEncoder())));
		socket.setHandler(new DnsHandler());
		List<SocketAddress> addz = new ArrayList<SocketAddress>();
		
		for(Socket s:sockets) {
			
			if(s.getIp().equals(Socket.ANY)) {
				
				for(InetAddress ia:NetHelper.getAllAddresses()) {
					
					dnsBoundAddresses.add(ia);
					
					addz.add(new InetSocketAddress(ia, s.getPort()));
				}
			}
			else {
				
				InetAddress ia = InetAddress.getByName(s.getIp());
				dnsBoundAddresses.add(ia);
				
				addz.add(new InetSocketAddress(ia,s.getPort()));
			}
		}
		
		socket.bind(addz);
		log.info("DNS is listning on {} ",addz);
	}

}
