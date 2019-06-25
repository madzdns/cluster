package com.github.madzdns.server.core.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "geoz")
@XmlAccessorType(XmlAccessType.FIELD)
public class Geoz {

	@XmlElement(name="g")
	private List<String> geoz;
	
	@XmlAttribute(name="a")
	private boolean auto = false;
	
	@XmlAttribute(name="gf")
	private double gfactor = 0.5;
	
	public double getGfactor() {
		
		return gfactor;
	}
	
	public void setGfactor(double gfactor) {
		
		this.gfactor = gfactor;
	}
	
	public List<String> getGeoz() {
		
		return geoz;
	}

	public void setGeoz(List<String> geoz) {
		
		this.geoz = geoz;
	}

	public boolean isAuto() {
		
		return auto;
	}

	public void setAuto(boolean auto) {
		
		this.auto = auto;
	}
}
