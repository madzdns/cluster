package server.config.entry.dns;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "edy")
@XmlAccessorType(XmlAccessType.FIELD)
public class EDy {

	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="tt")
	private long ttl;

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
}
