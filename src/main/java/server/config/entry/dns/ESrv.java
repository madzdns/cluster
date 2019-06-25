package server.config.entry.dns;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "esrv")
@XmlAccessorType(XmlAccessType.FIELD)
public class ESrv {

	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="t")
	private String target;
	
	@XmlAttribute(name="p")
	private int periority;
	
	@XmlAttribute(name="w")
	private int wight;
	
	@XmlAttribute(name="po")
	private int port;
	
	@XmlAttribute(name="tt")
	private long ttl;

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public int getPeriority() {
		return periority;
	}

	public void setPeriority(int periority) {
		this.periority = periority;
	}

	public int getWight() {
		return wight;
	}

	public void setWight(int wight) {
		this.wight = wight;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

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
