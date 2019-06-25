package server.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


@XmlType(name = "geo")
@XmlAccessorType(XmlAccessType.FIELD)
public class Geo {
	
	@XmlAttribute(name="map")
	private String map;
	
	@XmlElement(name="server")
	private List<Server> server;
	
	public void setServer(List<Server> server) {
		this.server = server;
	}
	public List<Server> getServer() {
		return server;
	}
	public void setMap(String map) {
		this.map = map;
	}
	public String getMap()
	{
		return map;
	}

}
