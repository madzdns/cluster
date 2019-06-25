package server.config;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.config.entry.dns.EntryDns;

@XmlRootElement(name="R")
@XmlType(name = "Resource")
@XmlAccessorType(XmlAccessType.FIELD)
public class Resource {

	
	@XmlTransient
	private static Logger log = LoggerFactory.getLogger(Resource.class);
	
	@XmlTransient
	private static JAXBContext jc = null;
	
	static {
		
		try{
			
			jc = JAXBContext.newInstance(Resource.class);
		}catch(Exception e){
			
			log.error("",e);
			//XXX exit point
			System.exit(1);
		}	
	}
	
	public static Resource fromBytes(final byte[] data) throws Exception {
		
		if(data==null) {
			
			throw new NullPointerException();
		}
		try {

			Resource tmpR = (Resource) jc.createUnmarshaller().unmarshal(
					new ByteArrayInputStream(data));
			
			tmpR.setContent(data);
			
			return tmpR;
		}
		catch (Exception e) {
			
			log.error("",e);
			
			throw e;
		}
	}
	
	public static Resource fromInputStream(final InputStream is) throws Exception {
		
		if(is == null) {
			
			throw new NullPointerException();
		}
		try {
			
			int nRead;
			
			byte[] data = null;
			
			data = new byte[16384];
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			
			while ((nRead = is.read(data, 0, data.length)) != -1) {
				
			  buffer.write(data, 0, nRead);
			  buffer.flush();
			}
			
			data = buffer.toByteArray();
			
			Resource tmpR = (Resource) jc.createUnmarshaller().unmarshal(new ByteArrayInputStream(data));
			
			tmpR.setContent(data);
			return tmpR;
		}
		catch (Exception e) {
			
			log.error("",e);
			throw e;
		}
	}
	
	@XmlTransient
	private byte[] content = null;

	@XmlTransient
	private Long lastmodified = null;
	
	@XmlAttribute(name="id")
	private String id;
	
	@XmlAttribute(name="a")
	private byte active = 0;
	
	@XmlElement(name="gr")
	private List<GeoResolver> geoResolvers;
	
	@XmlElement(name="dns")
	private List<EntryDns> dnsList;
	
	public String getId() {
		
		return id;
	}

	public void setId(String id) {
		
		this.id = id;
	}

	public byte[] getContent() {
		
		return content;
	}

	public void setContent(byte[] content) {
		
		this.content = content;
	}
	
	public long getLastmodified() {
		
		return lastmodified;
	}
	
	public void setLastmodified(long lastmodified) {
		
		this.lastmodified = lastmodified;
	}

	public List<GeoResolver> getGeoResolvers() {
		return geoResolvers;
	}

	public void setGeoResolvers(List<GeoResolver> geoResolvers) {
		this.geoResolvers = geoResolvers;
	}

	public List<EntryDns> getDnsList() {
		return dnsList;
	}

	public void setDnsList(List<EntryDns> dnsList) {
		this.dnsList = dnsList;
	}
	
	public boolean isActive() {
		
		return active == 1? true:false;
	}
	
	public void setActive(boolean active) {
		
		this.active = (byte)(active?1:0);
	}

}
