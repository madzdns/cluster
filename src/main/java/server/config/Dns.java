package server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "dns")
@XmlAccessorType(XmlAccessType.FIELD)
public class Dns {
	
	
	@XmlElement(name="soa")
	private Soa soa;
	
	@XmlElement(name="ns")
	private Ns ns;
	
	@XmlElement(name="mx")
	private Mx mx;

	public Soa getSoa() {
		
		return soa;
	}
	
	public void setSoa(Soa soa) {
		
		this.soa = soa;
	}
	
	public Ns getNs() {
		
		return ns;
	}
	
	public void setNs(Ns ns) {
		
		this.ns = ns;
	}
	
	public Mx getMx() {
		
		return mx;
	}

	public void setMx(Mx mx) {
		
		this.mx = mx;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder()
		.append("Soa=[")
		.append(soa)
		.append("],Ns=[")
		.append(ns)
		.append("],Mx=[")
		.append(mx)
		.append("]");
		return sb.toString();
	}
	
	public String toXmlString() {
		
		StringBuilder sb = new StringBuilder()
		.append("<dns>")
		.append(soa!=null?soa.toXmlString():"")
		.append(ns!=null?ns.toXmlString():"")
		.append(mx!=null?mx.toXmlString():"")
		.append("</dns>");
		return sb.toString();
	}
}
