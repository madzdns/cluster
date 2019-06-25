package com.github.madzdns.server.core.backend.kafka.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface IKafkaMessage {

	public void serialize(DataOutputStream out) throws IOException;
	
	public void deserialize(DataInputStream in) throws IOException;
}
