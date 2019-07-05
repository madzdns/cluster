package com.github.madzdns.cluster.http.codec;

import com.github.madzdns.cluster.http.HTTPResponse;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class HttpMinaEncoder implements ProtocolEncoder {

	@Override
	public void dispose(IoSession arg0) throws Exception {
		
	}

	@Override
	public void encode(IoSession session, Object in, ProtocolEncoderOutput out)
			throws Exception {
		
		if(in instanceof HTTPResponse) {
			
			HTTPResponse msg = (HTTPResponse) in;
			out.write(msg.toByteBuffer());
		}
	}
}
