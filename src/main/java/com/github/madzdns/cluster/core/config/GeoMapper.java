package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ma")
@XmlAccessorType(XmlAccessType.FIELD)
public class GeoMapper {
	
	@XmlTransient
	public final static byte HEARTBEAT_NONE = 0;
	
	@XmlTransient
	public final static byte HEARTBEAT_FROM_CLOUD = 1;
	
	@XmlTransient
	public final static byte HEARTBEAT_FROM_HOST = 2;
	
	@XmlAttribute(name="adr")
	private String addr;
	
	@XmlAttribute(name="ndm")
	private String ndomain = null;
	
	@XmlAttribute(name="hb")
	private byte heartbeat = HEARTBEAT_NONE;
	
	@XmlAttribute(name="rr")
	private int roundRobin;
	
	@XmlElement(name="gz")
	private Geoz geoz;

	public String getAddr() {
		
		return addr;
	}

	public void setAddr(String addr) {
		
		this.addr = addr;
	}

	public String getNdomain() {
		
		return ndomain;
	}

	public void setNdomain(String ndomain) {
		
		this.ndomain = ndomain;
	}

	public Geoz getGeoz() {
		
		return geoz;
	}

	public void setGeoz(Geoz geoz) {
		
		this.geoz = geoz;
	}
	
	public byte getHeartbeatType() {
		
		return heartbeat;
	}
	
	public boolean isWithHeartbeat() {
		
		return this.heartbeat == HEARTBEAT_FROM_CLOUD 
				|| this.heartbeat == HEARTBEAT_FROM_HOST;
	}
	
	public boolean isWithHeartbeatFromCloud() {
		
		return this.heartbeat == HEARTBEAT_FROM_CLOUD;
	}
	
	public boolean isWithHeartbeatFromHost() {
		
		return this.heartbeat == HEARTBEAT_FROM_HOST;
	}
	
	public boolean isWithRoundRobin() {
		
		return this.roundRobin == 1;
	}
}
