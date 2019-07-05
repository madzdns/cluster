package com.github.madzdns.cluster.core.config;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlAccessType; 
import javax.xml.bind.annotation.XmlAccessorType;

import com.github.madzdns.cluster.core.config.entry.dns.EntryDns;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@XmlType(name = "entry")
@XmlAccessorType(XmlAccessType.FIELD)
public class MyEntry {
	
	@XmlTransient
	private GeoResolver resolver;
	
	@XmlTransient
	private Object mapperMutext = new Object();
	
	@XmlTransient
	private String fullAName = null;
	
	@XmlTransient
	private ConcurrentMap<String, GeoMapper> geoMapperMap = null;
	
	@XmlAttribute(name="a")
	private String a;
	
	@XmlElement(name="ma")
	private List<GeoMapper> geoMapper;
	
	@XmlAttribute(name="k")
	private String key;
	
	@XmlAttribute(name="p")
	private String apikey;
	
	@XmlAttribute(name="m")
	private int resolveMode = EntryResolveModes.DNS_JUST_HA_RESOLVE.getValue();
	
	@XmlAttribute(name="tt")
	private long ttl = 40;
	
	@XmlAttribute(name="msg")
	private String failurMsg;
	
	@XmlAttribute(name="msgt")
	private String msgTheme;
	
	@XmlAttribute(name="r")
	private byte root;
	
	@XmlElement(name="edns")
	private EntryDns entryDns;
	
	@XmlElementWrapper(name="ssls")
	private Map<String, SSLCredentials> credentialsMap = null;

	public EntryDns getEntryDns() {
		
		return entryDns;
	}

	public void setEntryDns(EntryDns entryDns) {
		
		this.entryDns = entryDns;
	}

	public String getKey() {
		
		return key;
	}
	
	public void setKey(String key) {
		
		this.key = key;
	}
	
	public String getA() {
		
		return this.a;
	}
	
	public void setA(String a) {
		
		this.a = a;
	}
	
	public String getFullAName() {
		
		return fullAName;
	}

	public void setFullAName(String fullAName) {
		
		this.fullAName = fullAName;
	}

	public String getApikey() {
		
		return apikey;
	}
	
	public void setApikey(String apikey) {
		
		this.apikey = apikey;
	}
	
	public GeoResolver getResolver() {
		
		return resolver;
	}
	
	public void setResolver(GeoResolver resolver) {
		
		this.resolver = resolver;
	}
	
	public Object getMapperLock() {
		
		return mapperMutext;
	}
	
	public List<GeoMapper> getGeoMapper() {
		
		return geoMapper;
	}
	
	public void setGeoMapper(List<GeoMapper> geoMapper) {
		
		this.geoMapper = geoMapper;
	}
	
	public long getTtl() {
		
		return ttl;
	}
	
	public void setTtl(long ttl) {
		
		this.ttl = ttl;
	}
	
	public String getFailurMsg() {
		
		return failurMsg;
	}
	
	public void setFailurMsg(String failurMsg) {
		
		this.failurMsg = failurMsg;
	}
	
	public String getMsgTheme() {
		
		return msgTheme;
	}
	
	public void setMsgTheme(String msgTheme) {
		
		this.msgTheme = msgTheme;
	}
	
	public int getResolveMode() {
		
		return resolveMode;
	}
	
	public void setResolveMode(int resolveMode) {
		
		this.resolveMode = resolveMode;
	}

	public boolean hasFixedGeoMappers() {
		
		return this.geoMapper != null && this.geoMapper.size() > 0;
	}
	
	public boolean isDnsResolve() {
		
		return this.resolveMode == EntryResolveModes.DNS_RESOLVE.getValue();
	}
	
	public boolean isJustHaDnsMode() {
		
		return this.resolveMode == EntryResolveModes.DNS_JUST_HA_RESOLVE.getValue();
	}
	
	public boolean isRoot() {
		
		return this.root == 1? true: false;
	}
	
	public GeoMapper getGeomapper(String addr) {
		
		if(geoMapper == null || geoMapper.size() == 0) {
			
			return null;
		}
		
		if(geoMapperMap == null) {
			
			geoMapperMap = new ConcurrentHashMap<String, GeoMapper>();
			
			for(Iterator<GeoMapper> gIt = geoMapper.iterator(); gIt.hasNext(); ) {
				
				GeoMapper g = gIt.next();
				
				geoMapperMap.put(g.getAddr(), g);
			}
		}
		
		return geoMapperMap.get(addr);
	}

	public Map<String, SSLCredentials> getCredentialsMap() {
		
		return credentialsMap;
	}

	public void setCredentialsMap(Map<String, SSLCredentials> credentialsMap) {
		
		this.credentialsMap = credentialsMap;
	}
}
