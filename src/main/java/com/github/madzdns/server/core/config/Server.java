package com.github.madzdns.server.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


@XmlType(name = "com/github/madzdns/x/core")
@XmlAccessorType(XmlAccessType.FIELD)
public class Server {

	@XmlAttribute(name="threshold")
	private String threshold;
	@XmlValue
	private String value;
	@XmlAttribute(name="ttl")
	private String ttl;
	
	public void setThreshold(String threshold)
	{
		this.threshold = threshold;
	}
	public String getThreshold() {
		return this.threshold;
	}
	public String getValue()
	{
		return this.value;
	}
	public void setValue(String value)
	{
		this.value = value;
	}
	public String getTTl()
	{
		return this.ttl;
	}
	public void setTTl(String ttl)
	{
		this.ttl = ttl;
	}
}
