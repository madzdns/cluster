package com.github.madzdns.server.core.backend.kafka.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class KafkaMessageBase implements IKafkaMessage {
	
	private byte type;
	private short sourceId = 0;

	public byte getType() {
		
		return type;
	}

	public void setType(byte type) {
		
		this.type = type;
	}
	
	public short getSourceId() {
		
		return sourceId;
	}

	public void setSourceId(short sourceId) {
		
		this.sourceId = sourceId;
	}

	@Override
	public void serialize(DataOutputStream out) throws IOException {
		
		out.writeByte(getType());
		out.writeShort(getSourceId());
	}

	@Override
	public void deserialize(DataInputStream in) throws IOException {
		
		type = in.readByte();
		setSourceId(in.readShort());
	}

}
