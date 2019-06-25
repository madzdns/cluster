package com.github.madzdns.server.rtp.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

import com.github.madzdns.server.rtp.RTPPacket;

public class RtpMinaDecoder extends CumulativeProtocolDecoder{

	@Override
	protected boolean doDecode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		
		if(in.remaining()>0) {
			
			RTPPacket message = new RTPPacket(in);
			out.write(message);
			return true;
		}
		return false;
	}

	
}
