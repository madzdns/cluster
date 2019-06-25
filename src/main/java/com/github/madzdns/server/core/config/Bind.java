package com.github.madzdns.server.core.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "bind")
@XmlAccessorType(XmlAccessType.FIELD)
public class Bind {

	@XmlTransient
	public static final String RTMPSERVICE = "com/github/madzdns/x/rtmp";
	@XmlTransient
	public static final String RTSPSERVICE = "com/github/madzdns/x/rtsp";
	@XmlTransient
	public static final String DNSSERVICE = "com/github/madzdns/x/dns";
	@XmlTransient
	public static final String HTTPSERVICE = "com/github/madzdns/x/http";
	@XmlTransient
	public static final String BACKENDSERVICE = "backend";
	@XmlTransient
	public static final String SYNCHSERVICE = "synch";
	@XmlTransient
	public static final String WEBSERVICE = "webservice";
	
	@XmlAttribute(name="service")
	private String service = null;
	
	@XmlElement(name="socket")
	private List<Socket> sockets = null;
	
	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public List<Socket> getSockets() {
		return sockets;
	}

	public void setSockets(List<Socket> sockets) {
		this.sockets = sockets;
	}
}
