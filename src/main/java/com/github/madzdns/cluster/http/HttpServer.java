package com.github.madzdns.cluster.http;

import com.github.madzdns.cluster.http.handler.HttpHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.config.Bind;
import com.github.madzdns.cluster.core.config.Socket;

import com.github.madzdns.cluster.core.IServer;
import com.github.madzdns.cluster.core.utils.NetHelper;

public class HttpServer implements IServer {

	private List<Socket> sockets = null;
	private Logger log;
	public HttpServer(Bind bind) {
		
		log = LoggerFactory.getLogger(HttpServer.class);
		this.sockets = bind.getSockets();
	}
	
	@Override
	public void start() throws IOException {
		
		NioSocketAcceptor socket = new NioSocketAcceptor();
		socket.setHandler(new HttpHandler());
		socket.setReuseAddress(true);
		List<SocketAddress> addz = new ArrayList<SocketAddress>();
		
		for(Socket s:sockets) {
			
			if(s.getIp().equals(Socket.ANY)) {
				
				for(InetAddress ia:NetHelper.getAllAddresses()) {
					
					addz.add(new InetSocketAddress(ia, s.getPort()));
				}
			}
			else {
				
				addz.add(new InetSocketAddress(s.getIp(),s.getPort()));
			}
		}
		
		socket.bind(addz);
		log.info("HTTP is listning on {} ",addz);
	}
}
