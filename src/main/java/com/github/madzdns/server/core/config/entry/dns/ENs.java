package com.github.madzdns.server.core.config.entry.dns;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ens")
@XmlAccessorType(XmlAccessType.FIELD)
public class ENs {

	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="tt")
	private long ttl;
	
	@XmlAttribute(name="t")
	private String target;

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

	public String getTarget() {
		
		return target;
	}

	public void setTarget(String target) {
		
		this.target = target;
	}
}
