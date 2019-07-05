package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "NsC")
@XmlAccessorType(XmlAccessType.FIELD)
public class NameserverConfig {
	
	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="tt")
	private long ttl;
	
	@XmlAttribute(name="c")
	private int count;

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

	public int getCount() {
		
		return count;
	}

	public void setCount(int count) {
		
		this.count = count;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return String.format("%s, %d, %d", name, ttl, count);
	}
}
