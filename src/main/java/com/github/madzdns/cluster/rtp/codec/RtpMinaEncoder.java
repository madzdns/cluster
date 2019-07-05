package com.github.madzdns.cluster.rtp.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.rtp.RTPPacket;

public class RtpMinaEncoder implements ProtocolEncoder {

	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(RtpMinaEncoder.class);
	
	@Override
	public void dispose(IoSession session) throws Exception {
		
	}

	@Override
	public void encode(IoSession session, Object message, ProtocolEncoderOutput out)
			throws Exception {
		
		if(message instanceof RTPPacket) {
			
			RTPPacket msg = (RTPPacket) message;
			
			IoBuffer b = msg.toIoBuffer();
			
			out.write(b);
		}
	}

}
