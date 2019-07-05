package com.github.madzdns.cluster.core.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "ns")
@XmlAccessorType(XmlAccessType.FIELD)
public class Ns {
	
	@XmlElement(name="record")
	private List<NsField> fileds;
	
	public List<NsField> getFileds() {
		
		return fileds;
	}
	
	public void setFileds(List<NsField> fileds) {
		
		this.fileds = fileds;
	}
	
	
	@Override
	public String toString() {
		
		return String.valueOf(fileds);
	}
	
	public String toXmlString() {
		
		if(fileds == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		for(int i=0;i<fileds.size();i++) {
			
			sb.append("<ns>")
			.append(fileds.get(i).toXmlString())
			.append("</ns>");
		}
		return sb.toString();
	}
}
