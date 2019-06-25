package com.github.madzdns.server.core.backend.node.dynamic.metrics;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.github.madzdns.server.core.IServer;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.config.Bind;
import com.github.madzdns.server.core.config.Socket;
import com.github.madzdns.server.core.utils.NetHelper;

public class BackendMetricServer implements IServer {

	private Logger log = LoggerFactory.getLogger(BackendMetricServer.class);
	private List<Socket> sockets = null;
	
	public BackendMetricServer(Bind bind)
	{
		this.sockets = bind.getSockets();
	}
	@Override
	public void start() throws IOException {
		
		NioDatagramAcceptor socket = new NioDatagramAcceptor();
		socket.setHandler(new MetricsHandler());
		List<SocketAddress> addz = new ArrayList<SocketAddress>();
		
		for(Socket s:sockets) {
			
			if((s.getPort()%10) == 0x8) {
				
				continue;
			}
			
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
		log.info("Metrics calculator is listning on {} ",addz);
	}

	
}
