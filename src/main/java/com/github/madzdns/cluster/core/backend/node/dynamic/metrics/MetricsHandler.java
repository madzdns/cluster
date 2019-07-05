package com.github.madzdns.cluster.core.backend.node.dynamic.metrics;

import java.net.InetSocketAddress;

import com.github.madzdns.cluster.core.backend.node.INode;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.api.Resolver;
import com.github.madzdns.cluster.core.api.Types;
import com.github.madzdns.cluster.core.backend.node.dynamic.SystemInfo;
import com.github.madzdns.cluster.core.backend.node.dynamic.impl.Node;
import com.github.madzdns.cluster.core.backend.node.dynamic.metrics.codec.MetricsMinaDecoder;
import com.github.madzdns.cluster.core.backend.node.dynamic.metrics.codec.MetricsMinaEcnoder;
import com.github.madzdns.cluster.core.codec.ServerCodecFactory;

public class MetricsHandler extends IoHandlerAdapter{
	
	private Logger log = LoggerFactory.getLogger(MetricsHandler.class);
	
	private static final String MESSAGE_STATE_KEY = MetricsHandler.class.getName() + ".STATE";
	private static final String MESSAGE_STATE_KEY_TMP = MetricsHandler.class.getName() + "-TMP.VALUE";
	private static final double EXTTERA_LOAD = 10;
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		session.getFilterChain().addLast("metrics_coder",
				new ProtocolCodecFilter(new ServerCodecFactory(new MetricsMinaDecoder(),new MetricsMinaEcnoder())));
	}

	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		
		if(message instanceof BWINFOMessage) {
			
			double bw = 0;
			boolean calced = false;
			BWINFOMessage msg = (BWINFOMessage) message;
			INode ie = Resolver.getMapperNode().get(((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress());
			
			Node e = null;
			
			if(ie!=null)
				 e = (Node) ie;
			
			if(!SystemInfo.isNetType(msg.getType())) {
				
				log.error("Message clientBackend {} is not a NET_TYPE FROM {}",msg.getType(), msg.getSrc());
				return;
			}
			
			if(e==null||e.getNodeKey()==null
					||!e.getNodeKey().equals(msg.getNodeKey())) {
				
				log.error("Wrong metric packet FROM {}. (could not find equivallent node)",msg.getSrc());
				return;
			}
			
			e.setNodeKeyRecvd(true);
			BWINFOMessage msg1 = (BWINFOMessage)session.getAttribute(MESSAGE_STATE_KEY);
			BWINFOMessage msg2 = (BWINFOMessage)session.getAttribute(MESSAGE_STATE_KEY);
			
			if(msg1==null&&msg2==null) {
				
				session.setAttribute(MESSAGE_STATE_KEY,msg);
				return;
			}
			
			if(msg1!=null) {
				
				if(msg1.getId()==msg.getId()) {
					bw = calcBwLoad(msg1, msg);
					session.removeAttribute(MESSAGE_STATE_KEY);
					calced = true;
				}
				else if(msg2==null) {
					
					log.debug("msg1.getId()!=msg.getId() and msg2==null. setting new packet as msg2");
					session.setAttribute(MESSAGE_STATE_KEY_TMP,msg);
					return;
				}
			}
			
			if(!calced&&msg2!=null) {
				
				if(msg2.getId()==msg.getId()) {
					
					bw = calcBwLoad(msg2, msg);
					session.removeAttribute(MESSAGE_STATE_KEY_TMP);
					calced = true;
				}
				else if(msg1==null) {
					
					log.warn("msg2.getId()!=msg.getId() and msg1==null. setting new packet as msg1");
					
					session.setAttribute(MESSAGE_STATE_KEY,msg);
					return;
				}
			}
			
			if(!calced) {
				
				if(msg1.getTimestamp()>=msg2.getTimestamp()) {

					session.setAttribute(MESSAGE_STATE_KEY_TMP,msg);
				}
				else {

					session.setAttribute(MESSAGE_STATE_KEY,msg);
				}
				
				log.warn("{} Bandwidth Issue, Non of which is null and received message is not equal to both of them, adding 20% to load", msg.getSrc());
				bw = e.getNetParam(msg.getType());
				bw = bw+EXTTERA_LOAD;
				bw = bw>100?100:bw;
			}
			e.updateNetParams(msg.getType(),(byte)bw);
		}
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		
		session.closeNow();
		log.error("",cause);
	}
	
	private double calcBwLoad(BWINFOMessage msg1,BWINFOMessage msg) {
		
		double bw = 0;
		long time_diff = Math.abs((msg.getTimestamp()-msg1.getTimestamp()));
		
		if(time_diff==0) {

			return bw = 0;
		}
		
		bw = (((msg.getLength()+Types.ShortBytes+40.0)*8000)/time_diff);
		
		if(bw==0) {
			
			log.debug("BANDWIDTH IS COMPLETLY LOADED FOR {}",msg1.getSrc());
			return bw = 100;
		}
		
		bw = 100.0/bw;
		bw = bw>100?100:bw;
		
		log.debug("BANDWIDTH LOAD IS {} FOR {}",bw, msg1.getSrc());
		return bw;
	}
}
