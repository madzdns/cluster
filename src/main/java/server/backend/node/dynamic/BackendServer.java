package server.backend.node.dynamic;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.utils.NetHelper;
import server.config.Bind;
import server.config.Config;
import server.config.Socket;

public class BackendServer implements server.IServer {
	
	private Logger log = LoggerFactory.getLogger(BackendServer.class);
	private List<Socket> sockets = null;
	
	public BackendServer(Bind bind) {
		
		this.sockets = bind.getSockets();
		
		if(!new File(Config.getApp().getGeo()).exists()) {
			
			log.error("Could not find GEO datable file {}", Config.getApp().getGeo());
			System.exit(1);
		}
	}

	@Override
	public void start() throws IOException {
		
		NioSocketAcceptor socket = new NioSocketAcceptor();
		
		socket.setHandler(new BackendHandler());
		
		socket.setReuseAddress(true);
		
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
		
		log.info("BACKEND is listning on {} ",addz);
	}
}
