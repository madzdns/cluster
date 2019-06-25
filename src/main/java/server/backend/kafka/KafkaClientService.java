package server.backend.kafka;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import server.IServer;
import server.backend.kafka.consumer.IKafkaMessageHandler;
import server.backend.kafka.consumer.KafkaConsumerImpl;
import server.backend.kafka.producer.KafkaProducerImpl;

public class KafkaClientService implements IServer{
	
	
	
	public KafkaClientService(Properties producerProp, Properties consumerProp,
			List<String> topics, IKafkaMessageHandler consumerHandler) {
		
		KafkaConsumerImpl.init(consumerProp, topics, consumerHandler);
		KafkaProducerImpl.init(producerProp);
	}

	@Override
	public void start() throws IOException {
		
		
	}

}
