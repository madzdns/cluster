package server.backend.kafka.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class KafkaByteCoder implements Deserializer<IKafkaMessage>, Serializer<IKafkaMessage> {

	@Override
	public void close() {

	}

	@Override
	public void configure(Map<String, ?> config, boolean isKey) {
		
		
	}

	@Override
	public IKafkaMessage deserialize(String topic, byte[] data) {

		KafkaMessageBase m = new KafkaMessageBase();
		
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		
		try {
			
			m.deserialize(in);
			
			if(m.getType() == ZoneSynchMessage.MODE_SYNCH_ZONE) {
				
				m = new ResourceMessage();
				m.deserialize(in);
			}
			else if(m.getType() == ClusterSynchMessage.MODE_SYNCH_CLUSTER) {
				
				m = new ClusterMessage();
				m.deserialize(in);
			}
			
		} catch (IOException e) {
			
			return null;
		}
		 
		return m;
	}

	@Override
	public byte[] serialize(String topic, IKafkaMessage message) {
		
		if(message == null) {
			
			return null;
		}
		
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(stream);
		
		try {
			
			message.serialize(out);
			return stream.toByteArray();
			
		} catch (IOException e) {
			
			return null;
		}
	}

}
