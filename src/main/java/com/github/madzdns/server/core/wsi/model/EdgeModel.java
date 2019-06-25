package com.github.madzdns.server.core.wsi.model;

import java.util.Date;
import java.util.List;

import com.github.madzdns.server.core.config.Dns;

public class EdgeModel {
	
	public static class AddressPort {
		
		private String address;
		
		private int port;
		
		public String getAddress() {
			
			return address;
		}
		
		public void setAddress(String address) {
			
			this.address = address;
		}
		
		public int getPort() {
			
			return port;
		}
		
		public void setPort(int port) {
			
			this.port = port;
		}
		
		@Override
		public String toString() {

			return address+":"+port;
		}
		
		@Override
		public int hashCode() {
			
			return String.valueOf(address).hashCode() * 31 +Integer.valueOf(port).hashCode();
		}
	}

	private short id = 0;
	
	private boolean useSsl = true;
	
	private boolean authByKey = true;
	
	private long lastModified = new Date().getTime();
	
	private String key;
	
	private List<AddressPort> clusterAddrz;
	
	private List<AddressPort> backendAddrz;
	
	private boolean state = true;
	
	private Dns dns = null;
	
	private short monitorInterval = 620;
	
	private short monitorDelay = 15;
	
	private short reportInterval = 620;
	
	private short reportDelay = 15;

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

	public long getLastModified() {
		
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		
		this.lastModified = lastModified;
	}

	public String getKey() {
		
		return key;
	}

	public void setKey(String key) {
		
		this.key = key;
	}

	public List<AddressPort> getClusterAddrz() {
		
		return clusterAddrz;
	}

	public void setClusterAddrz(List<AddressPort> clusterAddrz) {
		
		this.clusterAddrz = clusterAddrz;
	}

	public List<AddressPort> getBackendAddrz() {
		
		return backendAddrz;
	}

	public void setBackendAddrz(List<AddressPort> backendAddrz) {
		
		this.backendAddrz = backendAddrz;
	}

	public boolean getState() {
		
		return state;
	}

	public void setState(boolean state) {
		
		this.state = state;
	}
	
	public Dns getDns() {
		
		return dns;
	}

	public void setDns(Dns dns) {
		
		this.dns = dns;
	}
	
	public short getMonitorInterval() {
		return monitorInterval;
	}

	public void setMonitorInterval(short monitorInterval) {
		this.monitorInterval = monitorInterval;
	}

	public short getReportInterval() {
		return reportInterval;
	}

	public void setReportInterval(short reportInterval) {
		this.reportInterval = reportInterval;
	}

	public short getMonitorDelay() {
		return monitorDelay;
	}

	public void setMonitorDelay(short monitorDelay) {
		this.monitorDelay = monitorDelay;
	}

	public short getReportDelay() {
		return reportDelay;
	}

	public void setReportDelay(short reportDelay) {
		this.reportDelay = reportDelay;
	}

	@Override
	public String toString() {
		
		StringBuffer buf = new StringBuffer();
		buf.append("id=")
		.append(this.id)
		.append(", state: ")
		.append(state)
		.append(", key=")
		.append(key)
		.append(", use SSL=")
		.append(useSsl)
		.append(", synch addrs:")
		.append(String.valueOf(clusterAddrz))
		.append(", backend addrs:")
		.append(String.valueOf(backendAddrz))
		.append(", dns=[")
		.append(dns)
		.append("]")
		.append(", monitor delay=")
		.append(monitorDelay)
		.append(", monitor interval")
		.append(monitorInterval)
		.append(", report delay=")
		.append(reportDelay)
		.append(", report interval")
		.append(reportInterval);
		
		return buf.toString();
	}
}
