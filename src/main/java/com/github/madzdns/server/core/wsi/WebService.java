package com.github.madzdns.server.core.wsi;

import com.github.madzdns.server.http.HttpServer;
import com.github.madzdns.server.http.web.WebController;
import com.github.madzdns.server.http.web.WebHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.github.madzdns.server.core.IServer;
import com.github.madzdns.server.core.config.Bind;
import com.github.madzdns.server.core.config.Socket;
import com.github.madzdns.server.core.utils.NetHelper;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebService implements IServer {

	private List<Socket> sockets = null;
	private Logger log;
	private List<Class<? extends WebController>> controllers = new ArrayList<Class<? extends WebController>>();
	
	public WebService(Bind bind) {
		
		log = LoggerFactory.getLogger(HttpServer.class);
		this.sockets = bind.getSockets();
	}
	
	public void addController(Class<? extends WebController> controller) {
		
		this.controllers.add(controller);
	}

	@Override
	public void start() throws IOException {

		NioSocketAcceptor socket = new NioSocketAcceptor();
		socket.setHandler(new WebHandler(controllers));
		socket.setReuseAddress(true);
		List<SocketAddress> addz = new ArrayList<SocketAddress>();
		
		for(Socket s:sockets) {
			
			if(s.getIp().equals(Socket.ANY)) {
				
				for(InetAddress ia: NetHelper.getAllAddresses()) {
					
					addz.add(new InetSocketAddress(ia, s.getPort()));
				}
			}
			else {
				
				addz.add(new InetSocketAddress(s.getIp(),s.getPort()));
			}
		}
		
		socket.bind(addz);
		
		log.info("WebService is listning on {} ",addz);
	}

}
