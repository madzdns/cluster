package server.config;

/*import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;*/
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*import javax.xml.bind.JAXBContext;*/
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAccessType; 
import javax.xml.bind.annotation.XmlAccessorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlType(name = "GeoResolver" /*,propOrder = {"contacts","minInterval","concurrents","entries"}*/)
@XmlAccessorType(XmlAccessType.FIELD)
public class GeoResolver  {
	
	@XmlTransient
	private static Logger log = LoggerFactory.getLogger(GeoResolver.class);
	
/*	@XmlTransient
	private static JAXBContext jc = null;
	
	static {
		
		try{
			
			jc = JAXBContext.newInstance(GeoResolver.class);
		}catch(Exception e){
			
			log.error("",e);
			//XXX exit point
			System.exit(1);
		}	
	}
	
	public static GeoResolver fromBytes(final byte[] data) throws Exception {
		
		if(data==null) {
			
			throw new NullPointerException();
		}
		try {

			GeoResolver tmpGeoR = (GeoResolver) jc.createUnmarshaller().unmarshal(
					new ByteArrayInputStream(data));
			
			tmpGeoR.setContent(data);
			
			return tmpGeoR;
		}
		catch (Exception e) {
			
			log.error("",e);
			
			throw e;
		}
	}
	
	public static GeoResolver fromInputStream(final InputStream is) throws Exception {
		
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
			
			GeoResolver tmpGeoR = (GeoResolver) jc.createUnmarshaller().unmarshal(new ByteArrayInputStream(data));
			
			tmpGeoR.setContent(data);
			return tmpGeoR;
		}
		catch (Exception e) {
			
			log.error("",e);
			throw e;
		}
	}*/
	
	@XmlTransient
	private Object concMutex = new Object();
	
	@XmlTransient
	private Object cnctMutex = new Object();
	
	@XmlTransient
	private Resource resource = null;
	
	/*@XmlTransient
	private byte[] content = null;*/
	
	/*@XmlTransient
	private Long lastmodified = null;*/
	
	@XmlTransient
	private int concurentCount = 0;
	
	@XmlTransient
	private Map<Long, List<Contact>> resolverContactsPerNode = new HashMap<Long,List<Contact>>();
	
	@XmlAttribute(name="r")
	private boolean root = false;
	
	@XmlAttribute(name="z")
	private String zone;
	
	@XmlAttribute(name="mi")
	private int minInterval = 40;
	
	@XmlAttribute(name="c")
	private int concurrents = 0;
	
	@XmlAttribute(name="npz")
	private int newPlanZone = 0;
	
	@XmlAttribute(name="pzid")
	private long planZoneId = 0;
	
	@XmlElement(name="ctct")
	private List<Contact> contacts;
	
	@XmlElement(name="rc")
	private RequestCount requestCount = null;
	
	@XmlElement(name="ent")
	private List<MyEntry> entries;
	
	public List<MyEntry> getEntries() {
		
		return entries;
	}
	
	public void setEntries(List<MyEntry> entries) {
		
		this.entries = entries;
	}
	
	public String getZone() {
		
		return zone;
	}
	
	public void setZone(String zone) {
		
		this.zone = zone;
	}
	
	/*public long getLastmodified() {
		
		return lastmodified;
	}
	
	public void setLastmodified(long lastmodified) {
		
		this.lastmodified = lastmodified;
	}*/
	
	public int getMinInterval() {
		
		return minInterval;
	}
	
	public void setMinInterval(int minInterval) {
		
		this.minInterval = minInterval;
	}
	
	public int getConcurrents() {
		
		return concurrents;
	}
	
	public void setConcurrents(int concurrents) {
		
		this.concurrents = concurrents;
	}
	
	public boolean chkUpdatConcurents() {
		
		synchronized (concMutex) {
			
			if(concurentCount>=concurrents)
				return false;
			
			concurentCount++;
			//dirty fix up
			concurentCount = concurentCount<=concurrents?concurentCount:concurrents;
			return true;
		}
	}
	
	public void countDown() {
		
		synchronized (concMutex) {
			
			concurentCount--;
			//dirty fix up
			concurentCount = concurentCount>=0?concurentCount:0;
		}
	}
	
	public int getConcurentCount() {
		
		return concurentCount;
	}
	
	public List<Contact> checkApplyContacts(Long nodeId, List<Contact> applyContacts) {
		
		if(contacts == null) {
			
			return null;
		}
		
		synchronized (cnctMutex) {
					
			List<Contact> safeContacts = new ArrayList<Contact>();
			
			for(int i=0;i<contacts.size();i++) {
				
				Contact c = contacts.get(i);
				
				if(c.getTakedCount() == c.getCount()) {
					
					continue;
				}
				
				for(int j=0; j< applyContacts.size(); j++) {
					
					Contact ac = applyContacts.get(j);
					
					if(!ac.getType().equals(c.getType())) {
						
						continue;
					}
					
					safeContacts.add(ac);
					c.incTakedCount();
					
					if(c.getTakedCount() == c.getCount()) {
						
						break;
					}
				}
			}
			
			if(safeContacts.size() > 0) {
				
				this.resolverContactsPerNode.put(nodeId, safeContacts);
				return safeContacts;
			}
			
			return null;
		}
	}
	
	public void countDownContact(List<Contact> appliedContacts) {
		
		if(this.contacts == null) {
			
			return;
		}
		
		synchronized (cnctMutex) {
					
			if(appliedContacts == null) {
				
				return;
			}
		
			for(int i=0;i<contacts.size();i++) {
				
				Contact c = contacts.get(i);
				
				if(c.getTakedCount() == 0) {
					
					continue;
				}
				
				for(int j=0; j< appliedContacts.size(); j++) {
					
					Contact ac = appliedContacts.get(j);
					
					if(!ac.getType().equals(c.getType())) {
						
						continue;
					}
					
					c.decTakedCount();
					
					if(c.getTakedCount() == 0) {
						
						break;
					}
				}
			}
		}
	}
	
	public List<Contact> getNodesContacts(Long nodeId) {
		
		synchronized (cnctMutex) {
		
			return this.resolverContactsPerNode.get(nodeId);
		}
	}
	
	@Override
	public String toString() {
		
		return new StringBuilder(getZone())
		.append("<concurrents=")
		.append(concurrents)
		.append(", current_count=")
		.append(concurentCount)
		.toString();
	}
	
	public boolean isRoot() {
		
		return root;
	}

	public void setRoot(boolean root) {
		
		this.root = root;
	}

	/*public byte[] getContent() {
		
		return content;
	}

	public void setContent(byte[] content) {
		
		this.content = content;
	}*/

	public boolean isNewPlanZone() {
		
		return newPlanZone == 1;
	}

	public void setNewPlanZone(int newPlanZone) {
		
		this.newPlanZone = newPlanZone;
	}

	public RequestCount getRequestCount() {
		
		return requestCount;
	}

	public void setRequestCount(RequestCount requestCount) {
		
		this.requestCount = requestCount;
	}

	public long getPlanZoneId() {
		
		return planZoneId;
	}

	public void setPlanZoneId(long planZoneId) {
		
		this.planZoneId = planZoneId;
	}

	public Resource getResource() {
		
		return resource;
	}

	public void setResource(Resource resource) {
		
		this.resource = resource;
	}
}
