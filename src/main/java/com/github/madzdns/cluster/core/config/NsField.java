package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlType(name = "nsfield")
@XmlAccessorType(XmlAccessType.FIELD)
public class NsField {
	
	
	@XmlElement(name="name")
	private String name;
	
	@XmlElement(name="ttl")
	private long ttl;
	
	@XmlElement(name="ip")
	private String ip;

	public String getName() {
		
		return name;
	}

	public void setName(String name) {
		
		this.name = name;
	}

	public long getTtl() {
		
		return ttl;
	}

	public void setTtl(long ttl) {
		
		this.ttl = ttl;
	}
	
	
	public String getIp() {
		
		return ip;
	}

	public void setIp(String ip) {
		
		this.ip = ip;
	}

	@Override
	public String toString() {
		
		 return new StringBuilder()
		.append("name=")
		.append(name)
		.append(",ip=")
		.append(ip)
		.append(",ttl=")
		.append(ttl).toString();
	}
	
	public String toXmlString() {
		
		 return new StringBuilder()
		.append("<name>")
		.append(name)
		.append("</name>")
		.append("<ip>")
		.append(ip)
		.append("</ip>")
		.append("<ttl>")
		.append(ttl)
		.append("</ttl>").toString();
	}
	
	
}
