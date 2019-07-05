package com.github.madzdns.cluster.core.backend.kafka;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.github.madzdns.cluster.core.IServer;
import com.github.madzdns.cluster.core.config.Bind;
import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.core.config.Socket;
import com.github.madzdns.cluster.core.utils.NetHelper;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.backend.kafka.consumer.ConsumerHandler;

public class KafkaServer implements IServer {
	
	private List<Socket> sockets = null;
	private Logger log;
	
	public KafkaServer(Bind bind) {
		
		log = LoggerFactory.getLogger(KafkaServer.class);
		this.sockets = bind.getSockets();
	}

	@Override
	public void start() throws IOException {
		
		StringBuilder kafkaListeners = new StringBuilder();
		int zkeeperPort = 0;
		
		String kafkaBestAddress = "";
		
		for(Socket s:sockets) {
			
			if(s.getIp().equals(Socket.ANY)) {
				
				for(InetAddress ia: NetHelper.getAllAddresses()) {
					
					if((s.getPort()%2) != 0) {
						
						zkeeperPort = s.getPort();	
					}
					else {
						
						kafkaBestAddress = new StringBuilder("127.0.0.1")
						.append(":").append(s.getPort()).toString();
						
						kafkaListeners.append("PLAINTEXT://")
						.append(ia.getHostAddress()).append(":")
						.append(s.getPort()).append(",");
					}
				}
			}
			else {
				
				if((s.getPort()%2) != 0) {
					
					zkeeperPort = s.getPort();	
				}
				else {
					
					if(s.getIp().equals("127.0.0.1")) {
						
						kafkaBestAddress = new StringBuilder(s.getIp())
						.append(":").append(s.getPort()).toString();
					}

					kafkaListeners.append("PLAINTEXT://")
					.append(s.getIp()).append(":")
					.append(s.getPort()).append(",");
				}
			}
		}
		
		if(zkeeperPort == 0) {

			throw new IOException("No zookeeper bindings found");
		}
		
		if(kafkaListeners.length() == 0) {
			
			throw new IOException("Could not find any kafka address");
		}
		
		Properties kafkaProperties = new Properties();
		Properties zkProperties = new Properties();
		final int THREADS =  Runtime.getRuntime().availableProcessors();
		String zookeeperConnect = new StringBuilder("127.0.0.1:")
		.append(zkeeperPort).toString();
		
		log.info(Config.KAFKA_CONFIG_PATH);
		kafkaProperties.load(new FileInputStream(Config.KAFKA_CONFIG_PATH));
		
		kafkaProperties.put("listeners", kafkaListeners.substring(0, kafkaListeners.length()-1).toString());
		kafkaProperties.put("broker.id", String.valueOf(Config.getApp().getId()));
		kafkaProperties.put("num.network.threads", THREADS);
		kafkaProperties.put("num.io.threads", THREADS);
		kafkaProperties.put("num.io.threads", THREADS);//TODO this either should be removed or change name (maybe it was network)
		kafkaProperties.put("zookeeper.connect", zookeeperConnect);
		
		String logDir = kafkaProperties.getProperty("log.dirs");
		
		if(logDir == null) {

			throw new IOException("log.dirs property has not been set");
		}
		
		logDir = logDir.replaceAll("\\$PWD", Config.KAFKA_CONFIG_DIR);
		kafkaProperties.put("log.dirs", logDir);
		
		kafkaProperties.load(new FileInputStream(Config.KAFKA_ZOOKEEPER_CONF));
		
		zkProperties.put("clientPort", zkeeperPort);
		zkProperties.put("clientPortAddress", "127.0.0.1");
		
		logDir = kafkaProperties.getProperty("dataDir");
		
		if(logDir == null) {

			throw new IOException("log.dirs property has not been set");
		}
		
		logDir = logDir.replaceAll("\\$PWD", Config.KAFKA_CONFIG_DIR);
		zkProperties.put("dataDir", logDir);
		
		new EmbedZookeeper(zkProperties);
		
		try {
			log.info("Waiting 2 seconds for zookeeper core");
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			
			throw new IOException(e);
		}
		
		KafkaConfig kafkaConfig = new KafkaConfig(kafkaProperties);
		KafkaServerStartable kafka = new KafkaServerStartable(kafkaConfig);
		kafka.startup();
		
		try {
			log.info("Waiting 2 seconds for kafka core");
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			
			throw new IOException(e);
		}
		
		log.info("Kafka started");
		
		Properties consumerProp = new Properties();
		consumerProp.put("bootstrap.servers", kafkaBestAddress);
		consumerProp.put("group.id", String.valueOf(Config.getApp().getId()));
		consumerProp.put("enable.auto.commit", true);
		consumerProp.put("auto.commit.interval.ms", "1000");
		consumerProp.put("session.timeout.ms", "30000");
		consumerProp.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		consumerProp.put("value.deserializer", "KafkaByteCoder");
		
		Properties producerProp = new Properties();
		producerProp.put("bootstrap.servers", kafkaBestAddress);
		producerProp.put("acks", "all");
		producerProp.put("retries", 0);
		producerProp.put("batch.size", 16384);
		producerProp.put("linger.ms", 1);
		producerProp.put("buffer.memory", 33554432);
		producerProp.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		producerProp.put("value.serializer", "KafkaByteCoder");
		
		new KafkaClientService(producerProp, consumerProp,
				Arrays.asList(KafkaTopics.RESOURCES.getValue(),
						KafkaTopics.CLUSTER.getValue()), new ConsumerHandler()).start();
		
		log.info("Kafka clients service inited");
	}
}
