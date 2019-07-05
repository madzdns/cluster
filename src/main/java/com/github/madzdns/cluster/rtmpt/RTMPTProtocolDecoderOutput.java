package com.github.madzdns.cluster.rtmpt;

import com.github.madzdns.cluster.http.HTTPMessage;

import org.apache.mina.core.filterchain.IoFilter;
import org.apache.mina.core.filterchain.IoFilter.NextFilter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPTProtocolDecoderOutput implements ProtocolDecoderOutput {

	Object out = null;
	
	IoFilter currentFilter = null;
	
	private Logger log = LoggerFactory.getLogger(RTMPTProtocolDecoderOutput.class);
	
	RTMPTProtocolDecoderOutput(IoFilter currentFilter) {
		
		this.currentFilter = currentFilter;
	}
	
	@Override
	public void write(Object out) {
		
		this.out = out;
	}
	
	@Override
	public void flush(NextFilter next, IoSession session) {
		
		if(out == null) {
			
			return;
		}
		
		try {
			
			currentFilter.messageReceived(next,session, out);
			
		} catch (Exception e) {
			
			log.error("",e);
		}
	}
	
	public HTTPMessage getMessage() {
		
		return (HTTPMessage) this.out;
	}

}
