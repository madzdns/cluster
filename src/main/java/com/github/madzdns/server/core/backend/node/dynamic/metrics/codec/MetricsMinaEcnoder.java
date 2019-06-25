package com.github.madzdns.server.core.backend.node.dynamic.metrics.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.github.madzdns.server.core.api.Types;
import com.github.madzdns.server.core.backend.node.dynamic.INFOMessage;

public class MetricsMinaEcnoder implements ProtocolEncoder {

	@Override
	public void dispose(IoSession session) throws Exception {
		
	}

	@Override
	public void encode(IoSession session, Object in, ProtocolEncoderOutput out)
			throws Exception {
		
		if(!(in instanceof INFOMessage))
			return;
		
		INFOMessage message = (INFOMessage) in;
		short mlen = message.getMessageLength();
		IoBuffer bb = IoBuffer.allocate(mlen+Types.ShortBytes);
		bb.putShort(mlen);
		
		if(message.getKey()==null) {
			
			bb.putShort((short)0);
		}
		else {
			
			bb.putShort((short) message.getKey().getBytes().length);
			bb.put(message.getKey().getBytes());
		}
		bb.flip();
		out.write(bb);
	}
}
