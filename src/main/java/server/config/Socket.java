package server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name = "socket")
@XmlAccessorType(XmlAccessType.FIELD)
public class Socket {

	@XmlTransient
	public static final String ANY = "*";
	@XmlTransient
	private String ip = null;
	
	@XmlTransient
	private int port = 0;
	
	@XmlValue
	private String value=null;
	
	public String getIp()
	{
		if(ip!=null)
			return ip;
		if(value==null||value.split(":").length!=2)
			return null;
		return ip = value.split(":")[0];
	}
	public int getPort()
	{
		if(port!=0)
			return port;
		if(value==null||value.split(":").length!=2)
			return 0;
		return port = Integer.parseInt(value.split(":")[1]);
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
}
