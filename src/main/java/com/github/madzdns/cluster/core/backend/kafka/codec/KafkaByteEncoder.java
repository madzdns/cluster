package com.github.madzdns.cluster.core.backend.kafka.codec;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import kafka.serializer.Encoder;

public class KafkaByteEncoder implements Encoder<IKafkaMessage> {
	
	
	@Override
	public byte[] toBytes(IKafkaMessage m) {
		
		if(m == null) {
			
			return null;
		}
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try {
			
			m.serialize(out);
			return stream.toByteArray();
			
		} catch (IOException e) {
			
			return null;
		}
	}

}
