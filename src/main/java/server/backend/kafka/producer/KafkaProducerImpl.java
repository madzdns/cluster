package server.backend.kafka.producer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import server.backend.kafka.KafkaTopics;
import server.backend.kafka.codec.IKafkaMessage;

public class KafkaProducerImpl {

	private static KafkaProducer<String, IKafkaMessage> producer;
	
	public static void init(Properties props) {
		
		producer = new KafkaProducer<String, IKafkaMessage>(props);
	}
	
	public static void send(KafkaTopics topic, String key, IKafkaMessage message) 
			throws InterruptedException, ExecutionException {
		
		producer.send(new ProducerRecord<String, IKafkaMessage>(topic.getValue(), key, message)).get();
	}
}
