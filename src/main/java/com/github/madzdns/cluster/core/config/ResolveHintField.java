package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name = "field")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResolveHintField {

	@XmlTransient
	private byte byteType;
	
	@XmlAttribute(name="clientBackend")
	private String type;
	
	@XmlValue
	private String value;

	public String getType() {
		
		return type;
	}

	public void setType(String type) {
		
		this.type = type;
	}

	public String getValue() {
		
		return value;
	}

	public void setValue(String value) {
		
		this.value = value;
	}
	
	public byte getByteType() {
		
		return byteType;
	}

	public void setByteType(byte byteType) {
		
		this.byteType = byteType;
	}
	
	@Override
	public String toString() {
		
		return new StringBuilder().append("clientBackend=")
				.append(byteType)
				.append(", value=")
				.append(value)
				.toString();
	}
}
