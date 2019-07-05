package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "mxfield")
@XmlAccessorType(XmlAccessType.FIELD)
public class MxField {

	/*
	 * EXCHANGER
	 */
	@XmlElement(name="t")
	private String name;
	
	@XmlElement(name="ttl")
	private long ttl;
	
	@XmlElement(name="p")
	private short periority;
	
	@XmlElement(name="n")
	private String domain = null;

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
	
	public short getPeriority() {
		
		return periority;
	}

	public void setPeriority(short periority) {
		
		this.periority = periority;
	}

	public String getDomain() {
		
		return domain;
	}

	public void setDomain(String domain) {
		
		this.domain = domain;
	}

	@Override
	public String toString() {
		
		 return new StringBuilder()
		.append("name=")
		.append(name)
		.append(",ttl=")
		.append(ttl)
		.append(periority)
		.toString();
	}
	
	public String toXmlString() {
		
		 return new StringBuilder()
		.append("<name>")
		.append(name)
		.append("</name>")
		.append("<domain>")
		.append(domain)
		.append("</domain>")
		.append("<ttl>")
		.append(ttl)
		.append("</ttl>")
		.append("<periority>")
		.append(periority)
		.append("</periority>")
		.toString();
	}
}
