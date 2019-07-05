package com.github.madzdns.cluster.rtp.handler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import com.github.madzdns.cluster.rtp.codec.RtpMinaDecoder;
import jxlibp.packetize.rtp.RTPpacketizer;
import jxlibp.packetize.rtp.RtpCodecBuffer;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.red5.io.object.UnsignedInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.rtp.ICTransport;
import com.github.madzdns.cluster.rtp.RTPPacket;
import com.github.madzdns.cluster.rtp.codec.RtpMinaEncoder;
import com.github.madzdns.cluster.rtsp.ITransportHandler;
import com.github.madzdns.cluster.core.api.net.NetProvider;
import com.github.madzdns.cluster.core.codec.ServerCodecFactory;
import com.github.madzdns.cluster.core.utils.AvailableOrderedPortFinder;
import com.github.madzdns.cluster.core.utils.AvailableOrderedPortFinder.ORDER;

public class RtpTransportHandler extends IoHandlerAdapter implements ITransportHandler {

	private Logger log = LoggerFactory.getLogger(RtpTransportHandler.class);
	
	private RTPpacketizer packetizer;
	
	private NioDatagramConnector udpConnector;
	
	private IoSession sessionTrack;
	
	private short sequence = 0;
	
	private UnsignedInt ssrc;
	
	private static Boolean noEvent = true;
	
	private static Object lock = new Object();
	
	private int rtpPayloadType = 0;
	
	public RtpTransportHandler(RTPpacketizer packetizer,
			ICTransport transport,
			InetAddress remote, InetAddress local) throws ConnectException {
		
		if(transport.getDstPorts()==null 
				|| transport.getDstPorts().length==0
				|| transport.getDstPorts()[0] <= 0)
			
			throw new ConnectException("Wrong client port parameters");
		
		this.packetizer = packetizer;
		
		this.udpConnector = new NioDatagramConnector();
		
		udpConnector.setHandler(this);
		
		ssrc = new UnsignedInt(transport.getUint32Ssrc());
		
		sequence = transport.getSequence();
		
		transport.setTimeStamp(packetizer.getBaseTimeStamp());
		
		rtpPayloadType = packetizer.getPayloadType();
		
		int[] ports = new int[2];
		
		if( AvailableOrderedPortFinder.findNextPort(0, ORDER.EVEN, ports) < 2 )
			
			throw new ConnectException("Internal error"); 
		
		ConnectFuture cf = udpConnector.connect(new InetSocketAddress(remote, transport.getDstPorts()[0]),
				new InetSocketAddress(local,ports[0]));
		
		try {
			
			cf.await();
			
			sessionTrack = cf.getSession();
			
			//ports = {((InetSocketAddress)sessionTrack.getLocalAddress()).getPort(),((InetSocketAddress)sessionTrack.getLocalAddress()).getPort()+1};
			
			transport.setLocalPorts(ports);
			
			sessionTrack.getFilterChain().addLast("com/github/madzdns/x/rtp",
					new ProtocolCodecFilter(new ServerCodecFactory(new RtpMinaDecoder(), new RtpMinaEncoder())));
			
			
		} catch (InterruptedException e) {
			
			throw new ConnectException("Session error");
		}
		
		if(sessionTrack == null) {
			
			throw new ConnectException();
		}
		
	}
	
	@Override
	public void play() {
		
		synchronized (lock) {

			new Thread(new Runnable() {
				
				@Override
				public void run() {
					
					try {
						
						myPlay(sessionTrack);
						
						
					} catch (ConnectException e) {
						e.printStackTrace();
					}
				}
				
			}).start();
			
			// Wait till success or exception of the session
			while(noEvent) {
				
				try {
					lock.wait();
					
				} catch (InterruptedException e) {}	
			}
		}
		
	}
	
	private void myPlay(IoSession session) throws ConnectException {
		
		if(session == null) {
			
			throw new ConnectException();
		}
		
		RtpCodecBuffer payload = null;
		
		int packetCounter = 0;
		
		while ((payload = packetizer.nextPayload(1300)) != null) {
			
			packetCounter ++;
			
			RTPPacket rtp = new RTPPacket();
			
			rtp.setVersion((byte)2);
			
			rtp.setExtension(false);
			
			rtp.setMarker(payload.isMarked());
			
			rtp.setPadding(false);
			
			rtp.setPayloadType(RTPPacket.unsignedByteFromInt(rtpPayloadType));
			
			rtp.setSsrc(ssrc);
			
			rtp.setTimestamp(RTPPacket.unsignedIntFromLong(payload.getTimestamp()));
	
			rtp.setSequence(RTPPacket.unsignedShortFromInt(sequence++));
			
			rtp.setPayload(payload.getData());
			
			session.write(rtp.toIoBuffer());
			
			if(packetCounter ==1) {
				
				synchronized (lock) {
					
					noEvent = false;
					lock.notify();
				}	
			}
		}
		
		if(packetCounter < 5) {
			
			synchronized (lock) {
				
				noEvent = false;
				lock.notify();
			}	
		}
		
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		
		sessionTrack = null;
		
		synchronized (lock) {
			
			noEvent = false;
			lock.notify();
		}
		
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
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
