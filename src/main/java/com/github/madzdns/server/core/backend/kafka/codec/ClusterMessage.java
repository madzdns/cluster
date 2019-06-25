package com.github.madzdns.server.core.backend.kafka.codec;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.frfra.frsynch.ClusterNode.ClusterAddress;

public class ClusterMessage extends KafkaMessageBase {

	private short id;
	
	private boolean useSsl = true;
	
	private boolean authByKey = true;
	
	private String key;
	
	private long lastModified = 0;
	
	private Set<ClusterAddress> edgeAddrz;
	
	private Set<ClusterAddress> backendAddrz;
	
	private byte command;
	
	private short monitorInterval = 600;
	
	private short monitorDelay = 20;
	
	private short reportInterval = 600;
	
	private short reportDelay = 20;
	
	public ClusterMessage() {
		
		setType(ClusterSynchMessage.MODE_SYNCH_CLUSTER);
	}
	
	public ClusterMessage(final short id,
			final boolean useSsl,
			final boolean authByKey,
			final String key,
			long modified,
			final Set<ClusterAddress> edgeAddrz,
			final Set<ClusterAddress> backendAddrz,
			final byte command,
			final short monitorInterval,
			final short monitorDelay,
			final short reportInterval,
			final short reportDelay,
			final short sourceId) {
		
		this.id=id;
		this.useSsl=useSsl;
		this.authByKey=authByKey;
		this.key = key;
		this.lastModified = modified;
		this.edgeAddrz = edgeAddrz;
		this.backendAddrz = backendAddrz;
		this.command = command;
		
		this.monitorDelay = monitorDelay;
		this.monitorInterval = monitorInterval;
		this.reportDelay = reportDelay;
		this.reportInterval = reportInterval;
		
		setSourceId(sourceId);
		
	}

	public short getId() {
		return id;
	}

	public void setId(short id) {
		this.id = id;
	}

	public boolean isUseSsl() {
		return useSsl;
	}

	public void setUseSsl(boolean useSsl) {
		this.useSsl = useSsl;
	}

	public boolean isAuthByKey() {
		return authByKey;
	}

	public void setAuthByKey(boolean authByKey) {
		this.authByKey = authByKey;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public Set<ClusterAddress> getEdgeAddrz() {
		return edgeAddrz;
	}

	public void setEdgeAddrz(Set<ClusterAddress> edgeAddrz) {
		this.edgeAddrz = edgeAddrz;
	}

	public Set<ClusterAddress> getBackendAddrz() {
		return backendAddrz;
	}

	public void setBackendAddrz(Set<ClusterAddress> backendAddrz) {
		this.backendAddrz = backendAddrz;
	}

	public byte getCommand() {
		return command;
	}

	public void setCommand(byte command) {
		this.command = command;
	}

	public short getMonitorInterval() {
		return monitorInterval;
	}

	public void setMonitorInterval(short monitorInterval) {
		this.monitorInterval = monitorInterval;
	}

	public short getMonitorDelay() {
		return monitorDelay;
	}

	public void setMonitorDelay(short monitorDelay) {
		this.monitorDelay = monitorDelay;
	}

	public short getReportInterval() {
		return reportInterval;
	}

	public void setReportInterval(short reportInterval) {
		this.reportInterval = reportInterval;
	}

	public short getReportDelay() {
		return reportDelay;
	}

	public void setReportDelay(short reportDelay) {
		this.reportDelay = reportDelay;
	}

	@Override
	public void serialize(DataOutputStream out) throws IOException {
		
		super.serialize(out);
		out.writeShort(id);
		out.writeBoolean(useSsl);
		out.writeBoolean(authByKey);
		out.writeUTF(key);
		out.writeLong(lastModified);
		
		if(edgeAddrz != null) {
			
			out.writeByte(edgeAddrz.size());
			
			for(Iterator<ClusterAddress> it = edgeAddrz.iterator(); it.hasNext();) {
				
				ClusterAddress addr = it.next();
				
				if(addr.getAddress() == null) {
					
					out.writeByte(0);
					continue;
				}
				out.writeByte(addr.getAddress().getAddress().length);
				out.write(addr.getAddress().getAddress());
				out.writeShort(addr.getPort());
			}
		}
		else {
			
			out.writeByte(0);
		}
		
		if(backendAddrz != null) {
			
			out.writeByte(backendAddrz.size());
			
			for(Iterator<ClusterAddress> it = backendAddrz.iterator(); it.hasNext();) {
				
				ClusterAddress addr = it.next();
				out.write(addr.getAddress().getAddress());
				out.writeShort(addr.getPort());
			}
		}
		else {
			
			out.writeByte(0);
		}
		
		out.writeByte(command);
		out.writeShort(monitorInterval);
		out.writeShort(monitorDelay);
		out.writeShort(reportInterval);
		out.writeShort(reportDelay);
	}
	
	@Override
	public void deserialize(DataInputStream in) throws IOException {
		
		id = in.readShort();
		useSsl = in.readBoolean();
		authByKey = in.readBoolean();
		key = in.readUTF();
		lastModified = in.readLong();
		
		byte edgeAddrzSize = in.readByte();
		
		if(edgeAddrzSize > 0) {
			
			edgeAddrz = new HashSet<ClusterAddress>();
			
			int addrLen = 0;
			byte[] ip;
			short port = 0;
			ClusterAddress addr = null;
			
			for(int i=0; i< edgeAddrzSize; i++) {
			
				addrLen = in.readByte();
				
				if(addrLen == 0) {
					
					continue;
				}
				
				ip = new byte[addrLen];
				in.read(ip, 0, addrLen);
				port = in.readShort();
				
				addr = new ClusterAddress(new String(ip), port);
				edgeAddrz.add(addr);
			}
		}
		
		//means backends size
		edgeAddrzSize = in.readByte();
		
		if(edgeAddrzSize > 0) {
			
			backendAddrz = new HashSet<ClusterAddress>();
			
			int addrLen = 0;
			byte[] ip;
			short port = 0;
			ClusterAddress addr = null;
			
			for(int i=0; i< edgeAddrzSize; i++) {
			
				addrLen = in.readByte();
				
				if(addrLen == 0) {
					
					continue;
				}
				
				ip = new byte[addrLen];
				in.read(ip, 0, addrLen);
				port = in.readShort();
				
				addr = new ClusterAddress(new String(ip), port);
				backendAddrz.add(addr);
			}
		}
		
		command = in.readByte();
		
		monitorInterval = in.readShort();
		monitorDelay = in.readShort();
		reportInterval = in.readShort();;
		reportDelay = in.readShort();
	}
}
