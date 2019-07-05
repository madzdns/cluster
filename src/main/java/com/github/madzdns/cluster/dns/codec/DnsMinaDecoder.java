package com.github.madzdns.cluster.dns.codec;

import com.github.madzdns.cluster.dns.DnsResolver;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsMinaDecoder extends CumulativeProtocolDecoder{
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(DnsMinaDecoder.class);

	@Override
	protected boolean doDecode(IoSession arg0, IoBuffer in,
			ProtocolDecoderOutput out) throws Exception {
		
		if(in.remaining()==0 ) {
			
			return false;
		}
		
		byte[] bytes = new byte[in.remaining()];
		in.get(bytes);
		DnsResolver dns = new DnsResolver(bytes);
		dns.processQuery();
		out.write(dns);
		return true;
	}
}
