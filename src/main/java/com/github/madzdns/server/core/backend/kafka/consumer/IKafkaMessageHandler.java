package com.github.madzdns.server.core.backend.kafka.consumer;

public interface IKafkaMessageHandler {

	public void messageReceived(Object message, Object key, Object topic, long offset);
}
