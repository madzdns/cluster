package com.github.madzdns.server.rtsp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.config.Bind;
import com.github.madzdns.server.core.config.Socket;

import com.github.madzdns.server.rtsp.handler.RtspHandler;
import com.github.madzdns.server.core.IServer;
import com.github.madzdns.server.core.utils.NetHelper;

public class RtspServer implements IServer {

	private List<Socket> sockets = null;
	private Logger log;
	public RtspServer(Bind bind)
	{
		log = LoggerFactory.getLogger(RtspServer.class);
		this.sockets = bind.getSockets();
	}
	@Override
	public void start() throws IOException {
		
		NioSocketAcceptor socket = new NioSocketAcceptor();
		socket.setHandler(new RtspHandler());
		socket.setReuseAddress(true);
		List<SocketAddress> addz = new ArrayList<SocketAddress>();
		for(Socket s:sockets)
		{
			if(s.getIp().equals(Socket.ANY))
			{
				for(InetAddress ia:NetHelper.getAllAddresses())
				{
					addz.add(new InetSocketAddress(ia, s.getPort()));
				}
			}
			else
			{
				addz.add(new InetSocketAddress(s.getIp(),s.getPort()));
			}
		}
		socket.bind(addz);
		log.info("RTSP is listning on {} ",addz);
	}
}
