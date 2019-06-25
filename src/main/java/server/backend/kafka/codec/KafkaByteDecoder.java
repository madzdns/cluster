package server.backend.kafka.codec;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import kafka.serializer.Decoder;
//import org.apache.kafka.common.serialization.Deserializer;

public class KafkaByteDecoder implements Decoder<IKafkaMessage> {

	@Override
	public IKafkaMessage fromBytes(byte[] data) {
		
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

}
