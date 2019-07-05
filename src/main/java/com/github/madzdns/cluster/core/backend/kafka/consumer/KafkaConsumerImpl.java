package com.github.madzdns.cluster.core.backend.kafka.consumer;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import com.github.madzdns.cluster.core.backend.kafka.codec.IKafkaMessage;

public class KafkaConsumerImpl {

	private static KafkaConsumer<String, IKafkaMessage> consumer;
	private static IKafkaMessageHandler handler;
	
	public static void init(Properties props, List<String> topics, IKafkaMessageHandler handler) {
		
		consumer = new KafkaConsumer<String, IKafkaMessage>(props);
		consumer.subscribe(topics);
		KafkaConsumerImpl.handler = handler;
	}
	
	public static void consume() {
		
		if(consumer == null) {
			
			return;
		}
		
		ConsumerRecords<String, IKafkaMessage> records = consumer.poll(100);
		
        for ( Iterator<ConsumerRecord<String, IKafkaMessage>> it = records.iterator(); it.hasNext();) {
        	
        	ConsumerRecord<String, IKafkaMessage> record = it.next();
        	System.out.printf("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value());
        	
        	if(record.value() == null) {
        		
        		continue;
        	}
        	
        	handler.messageReceived(record.value(), record.key(), "", record.offset());
        }
	}
}
