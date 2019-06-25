package server.backend.kafka.codec;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ResourceMessage extends KafkaMessageBase {

	private byte[] content;
	
	private String zonename;
	
	private long lastModified;

	private byte command;
	
	public ResourceMessage() {
		
		setType(ZoneSynchMessage.MODE_SYNCH_ZONE);
	}
	
	public ResourceMessage(byte[] content, String zonename,
			long lastModified, short sourceId, byte command) {

		this();
		setSourceId(sourceId);
		this.content = content;
		this.zonename = zonename;
		this.lastModified = lastModified;
		this.command = command;
	}
	
	public ResourceMessage(InputStream is, String zonename,
			long lastModified, short sourceId, byte command) throws Exception {
		
		if(is==null) {
			
			throw new NullPointerException();
		}
		
		setSourceId(sourceId);
		
		try {
			
			int nRead;
			
			byte[] data = null;
			
			data = new byte[16384];
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				
			  buffer.write(data, 0, nRead);
			  buffer.flush();
			}
			
			data = buffer.toByteArray();
			
			this.content = data;
			this.zonename = zonename;
			this.lastModified = lastModified;
			this.command = command;
			
		}
		catch (Exception e) {

			throw e;
		}
	}

	public byte[] getContent() {
		
		return content;
	}

	public void setContent(byte[] content) {
		
		this.content = content;
	}

	public String getZonename() {
		
		return zonename;
	}

	public void setZonename(String zonename) {
		
		this.zonename = zonename;
	}

	public long getLastModified() {
		
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		
		this.lastModified = lastModified;
	}

	public byte getCommand() {
		
		return command;
	}

	public void setCommand(byte command) {
		
		this.command = command;
	}
	
	public void serialize(DataOutputStream out) throws IOException {
		
		super.serialize(out);
		
		if(content == null) {
			
			out.writeShort(0);
		}
		else {
		
			out.writeInt(content.length);
			out.write(content);
		}
		
		out.writeUTF(zonename);
		out.writeLong(lastModified);
		out.writeShort(getSourceId());
		out.writeByte(command);
	}
	
	public void deserialize(DataInputStream in) throws IOException {
		
		int len = in.readInt();
		
		if(len > 0) {
			
			content = new byte[len];
			in.read(content, 0, len);
		}
		
		zonename = in.readUTF();
		lastModified = in.readLong();
		setSourceId(in.readShort());
		command = in.readByte();
	}

}
