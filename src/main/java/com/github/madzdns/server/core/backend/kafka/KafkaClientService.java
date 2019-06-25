package com.github.madzdns.server.core.backend.kafka;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.github.madzdns.server.core.IServer;
import com.github.madzdns.server.core.backend.kafka.producer.KafkaProducerImpl;
import com.github.madzdns.server.core.backend.kafka.consumer.IKafkaMessageHandler;
import com.github.madzdns.server.core.backend.kafka.consumer.KafkaConsumerImpl;

public class KafkaClientService implements IServer {
	
	
	
	public KafkaClientService(Properties producerProp, Properties consumerProp,
			List<String> topics, IKafkaMessageHandler consumerHandler) {
		
		KafkaConsumerImpl.init(consumerProp, topics, consumerHandler);
		KafkaProducerImpl.init(producerProp);
	}

	@Override
	public void start() throws IOException {
		
		
	}

}
