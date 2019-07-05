package com.github.madzdns.cluster.core.backend.node.dynamic.metrics.codec;

import java.net.InetSocketAddress;
import java.util.Date;

import com.github.madzdns.cluster.core.backend.node.dynamic.metrics.BWINFOMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.api.Types;

public class MetricsMinaDecoder implements ProtocolDecoder{

	Logger log = LoggerFactory.getLogger(MetricsMinaDecoder.class);
	@Override
	public void decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {

		final String srcIp = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
		
		if(in.remaining()<Types.ShortBytes) {
			
			log.warn("Corrupted metric packet(remaining<Types.ShortBytes)");
			return;
		}
		
		BWINFOMessage message = new BWINFOMessage(in.getShort());
		message.setSrc(srcIp);
		
		int mlen = message.getLength();
		if(in.remaining()!=mlen
				||mlen<Types.CharBytes) {
			
			log.warn("Corrupted metric packet(remaining!={} or {}<Types.CharBytes) from {}",mlen,mlen,srcIp);
			return;
		}
		
		message.setType(in.get());
		message.setTimestamp(new Date());
		final int keysize = in.getShort();
		
		if(keysize>0) {
			
			final byte[] key = new byte[keysize];
			in.get(key);
			message.setNodeKey(new String(key));	
		}
		else {
			
			log.warn("Corrupted metric packet(key==null) from {}", srcIp);
			return;
		}
		message.setId(in.getShort());
		message.setSequence(in.get());
		message.setFakeValue(in.get());
		out.write(message);
		return;
	}
	
	@Override
	public void dispose(IoSession arg0) throws Exception {
		
	}
	@Override
	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
			throws Exception {
		
	}

}
