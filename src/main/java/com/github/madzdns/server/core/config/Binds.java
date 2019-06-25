package com.github.madzdns.server.core.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "binds")
@XmlAccessorType(XmlAccessType.FIELD)
public class Binds {

	@XmlElement(name="bind")
	private List<Bind> binds = null;

	public List<Bind> getBinds() {
		return binds;
	}
	public void setBinds(List<Bind> binds) {
		this.binds = binds;
	}
}
