package com.github.madzdns.cluster.rtmp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.github.madzdns.cluster.core.IServer;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.github.madzdns.cluster.core.config.Bind;
import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.core.config.Socket;
import com.github.madzdns.cluster.rtmp.handler.RTMPRedirectMinaHandler;
import com.github.madzdns.cluster.core.utils.NetHelper;

public class RtmpServer implements IServer {
	
	List<Socket> sockets = null;
	
	private static Logger log = LoggerFactory.getLogger(RtmpServer.class);
	
	public static FileSystemXmlApplicationContext CTX;
	
	static {
		
		URL[] urls = new URL[]{Config.getSpringConf()};
		ClassLoader CL = new URLClassLoader(urls);
		Thread.currentThread().setContextClassLoader(CL);
		
		System.setProperty("red5.root", Config.APPHOME);
		System.setProperty("red5.config_root", Config.SPRING_CONF);
		
		CTX = new FileSystemXmlApplicationContext(new String[]{"classpath:/red5.xml"},false);
		
		if(CTX == null) {
			
			log.error("Could not load FileSystemXmlApplicationContext");
			//XXX exit point
			System.exit(1);
		}
		
		CTX.setClassLoader(CL);
		CTX.refresh();
	}
	
	public RtmpServer(Bind bind) {
		
		this.sockets = bind.getSockets();
	}
	
	@Override
	public void start() throws IOException {
		
		NioSocketAcceptor socket = new NioSocketAcceptor();

		
		socket.setHandler(RtmpServer.CTX.getBean("red5.core", ApplicationContext.class).getBean("rtmpMinaIoHandler",RTMPRedirectMinaHandler.class));
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
		
		log.info("RTMP is listning on {} ",addz);
	}

}
