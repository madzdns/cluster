package com.github.madzdns.cluster.core.config.entry.dns;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ena")
@XmlAccessorType(XmlAccessType.FIELD)
public class ENaPtr {

	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="tt")
	private long ttl;
	
	@XmlAttribute(name="p")
	private int preference;
	
	@XmlAttribute(name="f")
	private String flag;
	
	@XmlAttribute(name="s")
	private String service;
	
	@XmlAttribute(name="eg")
	private String regex = "";
	
	@XmlAttribute(name="ep")
	private String replacement = ".";
	
	@XmlAttribute(name="o")
	private int order;

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

	public int getPreference() {
		return preference;
	}

	public void setPreference(int preference) {
		this.preference = preference;
	}

	public String getFlag() {
		return flag;
	}

	public void setFlag(String flag) {
		this.flag = flag;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public String getRegex() {
		return regex;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	public String getReplacement() {
		return replacement;
	}

	public void setReplacement(String replacement) {
		this.replacement = replacement;
	}
	
	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}
}
