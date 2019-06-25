package com.github.madzdns.server.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ssl")
@XmlAccessorType(XmlAccessType.FIELD)
public class SSLCredentials {

	@XmlElement(name="c")
	private String certificate;
	
	@XmlElement(name="k")
	private String key;
	
	public SSLCredentials() {
		
	}
	
	public SSLCredentials(String certificate, String key) {

		this.certificate = certificate;
		this.key = key;
	}

	public String getCertificate() {
		return certificate;
	}
	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
}
