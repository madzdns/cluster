package server.backend.node.dynamic.metrics;

import java.util.Date;

import server.api.Types;
import server.backend.node.dynamic.INFOMessage;

public class BWINFOMessage extends INFOMessage{
	
	public static final byte EVEN_SEQ = 0;
	public static final byte ODD_SEQ = 1;
	
	private short id;
	private byte sequence;
	private long timestamp = 0;
	private byte type = 0;
	private byte fakeValue;

	public BWINFOMessage(short len)
	{
		super(len);
	}
	public BWINFOMessage()
	{
	}
	
	@Override
	public short getMessageLength() {
		//type
		short s = (short)Types.Bytes;
		//key
		if(getNodeKey()!=null)
			s+=(short)(getNodeKey().getBytes().length+Types.ShortBytes);
		else
			s += (short)(Types.ShortBytes);
		//id
		s+=Types.ShortBytes;
		//sequence
		s+=Types.Bytes;
		//fakevalue
		s+=Types.Bytes;
		return s;
	}
	public short getId() {
		return id;
	}
	public void setId(short id) {
		this.id = id;
	}
	public byte getSequence() {
		return sequence;
	}
	public void setSequence(byte sequence) {
		this.sequence = sequence;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		if(timestamp!=null)
			this.timestamp = timestamp.getTime();
	}
	public byte getType() {
		return type;
	}
	public void setType(byte type) {
		this.type = type;
	}
	public byte getFakeValue() {
		return fakeValue;
	}
	public void setFakeValue(byte fakeValue) {
		this.fakeValue = fakeValue;
	}
}