package server.backend.kafka.codec;

import java.util.List;

import server.api.Types;

public abstract class SynchMessage {

	public final static byte TYPE_BAD_KEY = 0;
	public final static byte TYPE_BAD_SEQ = 1;
	public final static byte TYPE_BAD_ID = 2;
	public final static byte TYPE_OK = 3;
	public final static byte TYPE_FULL_CHECK = 4;
	public final static byte TYPE_CHECK = 5;
	public final static byte TYPE_NOT_VALID_EDGE = 6;
	public final static byte TYPE_BOTH_STARTUP = 7;
	
	
	public final static byte COMMAND_TAKE_THis = 0;
	public final static byte COMMAND_GIVE_THis = 1;
	public final static byte COMMAND_DEL_THis = 2;
	public final static byte COMMAND_OK = 3;
	public final static byte COMMAND_RCPT_THis = 4;
	
	public final static byte SCHEDULED = 1;
	public final static byte NOT_SCHEDULED = 0;
	
	public final static byte IN_STARTUP = 1;
	public final static byte NOT_IN_STARTUP = 0;
	
	/*
	 * I think maximum sequence of 4 is sufficient
	 */
	public final static byte SEQ_MAX = 4;
	
	private List<String> keyChain = null;
	
	private int length = 0;
	
	private short id = 0;
	
	private byte type = 0;
	
	private byte sequence = 0;
	
	private byte mode;
	
	private boolean inStartup = false;
	
	public SynchMessage(){}
	
	public SynchMessage(final short len){ this.length = len;}
	

	public short getMessageLength() {
		
		//starting with id
		short len = (short)Types.ShortBytes;
		
		//mode
		len += Types.Bytes;
		
		//sequence number
		len += Types.CharBytes;
		
		//keys len
		len += Types.Bytes;
		List<String> keys = getKeyChain();
		
		if(keys!=null) {
			
			String key = null;
			for(int i=0;i<keys.size();i++) {
				
				key = keys.get(i);
				len += Types.Bytes;
				if(key!=null)
					len +=key.getBytes().length;
			}
			
			key = null;
			keys = null;
		}
		
		//in startup
		len += Types.Bytes;
		
		return len;
	}
	
	public byte getType() {
		
		return type;
	}
	
	public void setType(byte type) {
		
		this.type = type;
	}
	
	public List<String> getKeyChain() {
		
		return keyChain;
	}

	public void setKeyChain(List<String> keyChain) {
		
		this.keyChain =keyChain;
	}
	
	public int getLength() {
		
		if(length>0)
			
			return length;
		
		length = getMessageLength();
		return length;
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
	
	public byte getMode() {
		
		return this.mode;
	}
	
	public void setMode(final byte mode) {
		
		this.mode = mode;
	}

	public boolean isInStartup() {
		
		return inStartup;
	}

	public void setInStartup(boolean inStartup) {
		
		this.inStartup = inStartup;
	}
}
