package server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "soa")
@XmlAccessorType(XmlAccessType.FIELD)
public class Soa {

	@XmlAttribute(name="ns")
	private String ns;
	
	@XmlAttribute(name="m")
	private String mail;
	
	@XmlAttribute(name="s")
	private long serial;
	
	@XmlAttribute(name="ref")
	private long refresh;
	
	@XmlAttribute(name="ret")
	private long retry;
	
	@XmlAttribute(name="e")
	private long expire;
	
	@XmlAttribute(name="mtt")
	private long minttl;
	
	@XmlAttribute(name="tt")
	private long ttl;
	
	/*@XmlAttribute(name="n")
	private String name;
	
	public String getName() {
		
		return name;
	}

	public void setName(String name) {
		
		this.name = name;
	}*/

	public String getNs() {
		
		return ns;
	}
	
	public void setNs(String ns) {
		
		this.ns = ns;
	}
	
	public String getMail() {
		
		return mail;
	}
	
	public void setMail(String mail) {
		
		this.mail = mail;
	}
	
	public long getSerial() {
		
		return serial;
	}
	
	public void setSerial(long serial) {
		
		this.serial = serial;
	}
	
	public long getRefresh() {
		
		return refresh;
	}
	
	public void setRefresh(long refresh) {
		
		this.refresh = refresh;
	}
	
	public long getRetry() {
		
		return retry;
	}
	
	public void setRetry(long retry) {
		
		this.retry = retry;
	}
	
	public long getExpire() {
		
		return expire;
	}
	
	public void setExpire(long expire) {
		
		this.expire = expire;
	}
	
	public long getMinttl() {
		
		return minttl;
	}
	
	public void setMinttl(long minttl) {
		
		this.minttl = minttl;
	}
	
	public long getTtl() {
		
		return ttl;
	}

	public void setTtl(long ttl) {
		
		this.ttl = ttl;
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder()
		.append("ns=")
		.append(ns)
		.append(",mail=")
		.append(mail)
		.append(",serial=")
		.append(serial)
		.append(",refresh=")
		.append(refresh)
		.append(",minttl=")
		.append(minttl)
		.append(",expire=")
		.append(expire)
		.append(",retry=")
		.append(retry);
		
		return sb.toString();
		
	}
	
	
	public String toXmlString() {
		
		StringBuilder sb = new StringBuilder()
		.append("<soa>")
		.append("<ns>")
		.append(ns)
		.append("</ns>")
		.append("<mail>")
		.append(mail)
		.append("</mail>")
		.append("<serial>")
		.append(serial)
		.append("</serial>")
		.append("<refresh>")
		.append(refresh)
		.append("</refresh>")
		.append("<minttl>")
		.append(minttl)
		.append("</minttl>")
		.append("<expire>")
		.append(expire)
		.append("</expire>")
		.append("<retry>")
		.append(retry)
		.append("</retry>")
		.append("</soa>");
		
		return sb.toString();
		
	}
	
}
