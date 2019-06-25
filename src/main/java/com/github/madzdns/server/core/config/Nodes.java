package com.github.madzdns.server.core.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "nodes")
@XmlAccessorType(XmlAccessType.FIELD)
public class Nodes {

	@XmlElement(name="mininterval")
	private int minInterval = 20;

	public int getMinInterval() {
		return minInterval;
	}

	public void setMinInterval(int minInterval) {
		this.minInterval = minInterval;
	}
}
