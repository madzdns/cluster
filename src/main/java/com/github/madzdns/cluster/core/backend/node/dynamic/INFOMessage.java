package com.github.madzdns.cluster.core.backend.node.dynamic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.github.madzdns.cluster.core.api.Types;
import com.github.madzdns.cluster.core.config.Contact;
import com.github.madzdns.cluster.core.config.ResolveHint;
import com.github.madzdns.cluster.core.config.ResolveHintField;

public class INFOMessage {
	
	private final String GEO_SPLITTER = "!";
	
	public static final byte ISAUTO = 1;
	public static final byte ISNOTAUTO = 0;
	
	public static final byte NEED_BACKENDS = 1;
	public static final byte DOES_NOT_NEED_BACKENDS = 0;
	
	private List<String> geomaps = null;
	private String dst = null;
	private String src = null;
	private short length = 0;
	private String key = null;
	private String domain = null;
	private short interval = 60;
	private List<SystemInfo> sysInfoz = null;
	private List<ServiceInfo> serviceInfoz = null;
	private boolean auto = false;
	private boolean needBackends;
	private String nodeKey;
	private List<Contact> contacts;
	private String ndomain = null;
	private byte noSys = SysNexTypes.WPOLICY;
	private String gfactor = null;
	private byte noGeo = GeoNexTypes.no_value;
	private short version;
	
	private List<ResolveHint> resolveHints;
	
	public short getVersion() {
		
		return version;
	}
	
	public void setVersion(short version) {
		
		this.version = version;
	}
	
	public String getGfactor() {
		
		return gfactor;
	}
	
	public void setGfactor(double gfactor) {
		
		this.gfactor = String.valueOf(gfactor);
	}
	
	public void setGfactor(String gfactor) {
		
		this.gfactor = gfactor;
	}
	
	public byte getNoSys() {
		
		return noSys;
	}
	
	public void setNoSys(byte noSys) {
		
		this.noSys = noSys;
	}
	
	public byte getNoGeo() {
		
		return noGeo;
	}
	
	public void setNoGeo(byte noGeo) {
		this.noGeo = noGeo;
	}
	
	public INFOMessage()
	{
		this((short)0);
	}
	
	public INFOMessage(short length) {
		
		this.length = length;
		geomaps = new ArrayList<String>();
	}
	
	public String getDst() {
		return dst;
	}
	
	public void setDst(String dst) {
		this.dst = dst;
	}
	
	public String getSrc() {
		return src;
	}
	
	public void setSrc(String src) {
		this.src = src;
	}
	
	public String getKey() {
		return key;
	}
	
	public void setKey(String key) {
		this.key = key;
	}
	
	public void addServiceInfoz(ServiceInfo service) {
		
		if(serviceInfoz==null)
			serviceInfoz = new ArrayList<ServiceInfo>();
		serviceInfoz.add(service);
	}
	
	public void clearSrvInfoz() {
		
		if(serviceInfoz!=null)
			serviceInfoz.clear();
	}
	
	public List<ServiceInfo> getSrvInfoz() {
		
		return serviceInfoz;
	}
	
	public void setSrvInfoz(List<ServiceInfo> serviceInfoz) {
		
		this.serviceInfoz = serviceInfoz;
	}
	
	public void addSysInfoz(SystemInfo sysinfo) {
		
		if(sysInfoz==null)
			sysInfoz = new ArrayList<SystemInfo>();
		sysInfoz.add(sysinfo);
	}
	
	public void clearSysInfoz() {
		
		if(sysInfoz!=null)
			sysInfoz.clear();
	}
	
	public List<SystemInfo> getSysInfoz() {
		
		return sysInfoz;
	}
	
	public void setSysInfoz(List<SystemInfo> sysInfoz) {
		
		this.sysInfoz = sysInfoz;
	}
	
	public void addGeomap(String value) {
		
		geomaps.add(value.toLowerCase());
	}
	
	public void setGeomaps(String splitted_value) {
		
		geomaps.clear();
		
		if(splitted_value==null) {
			
			return;	
		}
		
		String[] geoz = splitted_value.split(GEO_SPLITTER);
		
		for(int i=0; i< geoz.length; i++) {
			
			String g = geoz[i];
			
			if(g!= null && !g.equals("") && !g.contains("!")) {
				
				geomaps.add(g.toLowerCase());
			}
		}
	}
	
	public void setGeomaps(List<String> geoz) {
		
		geomaps.clear();
		
		if(geoz==null)
			
			return;
		
		for(int i=0; i< geoz.size(); i++) {
			
			String g = geoz.get(i);
			
			if(g!= null && !g.equals("") && !g.contains("!")) {
				
				geomaps.add(g.toLowerCase()+GEO_SPLITTER);
			}
		}
	}
	
	public List<String> getGeomaps() {
		
		return geomaps;
	}
	
	public short getLength() {
		
		if(length>0)
			return length;
		length = getMessageLength();
		return length;
	}
	
	public short getMessageLength() {
		
		//initialize version length
		short s = (short)Types.ShortBytes;
		//key
		if(key!=null)
			s+=(short)(key.getBytes().length+Types.ShortBytes);
		else
			s += (short)(Types.ShortBytes);
		//services
		s += Types.ShortBytes;
		if(serviceInfoz!=null) {
			
			for(ServiceInfo sr:serviceInfoz) {
				
				String raise = String.valueOf(sr.raise);
				s += (Types.ShortBytes+Types.Bytes+Types.Bytes+Types.ShortBytes+
						raise.getBytes().length);
			}
		}
		//statistics
		s += Types.ShortBytes;
		if(sysInfoz!=null)
			s += (sysInfoz.size()*(Types.Bytes+Types.Bytes+Types.Bytes+Types.Bytes+Types.Bytes));
		//contacts
		s += Types.ShortBytes;
		if(getContacts()!=null)
			for(Contact c:getContacts())
				s += (Types.Bytes+ c.getUsername().getBytes().length+
						Types.Bytes+ c.getPassword().getBytes().length+
						Types.Bytes+c.getType().getBytes().length+
						Types.Bytes+c.getValue().getBytes().length);
		//domain
		s +=Types.ShortBytes;
		if(domain!=null)
			s += (short)(domain.getBytes().length);
		//ndomain
		s +=Types.ShortBytes;
		if(getNdomain()!=null)
			s += (short)(getNdomain().getBytes().length);
		//interval
		s += (short)(Types.ShortBytes);
		//Being auto
		s += (Types.Bytes);
		//NeedBackends
		s += (Types.Bytes);
		//NoSysPolicy
		s += (Types.Bytes);
		//NoGeoPolicy
		s += (Types.Bytes);
		
		//number of hints
		s += Types.Bytes;
		
		if(getResolveHints()!=null) {
			
			for(Iterator<ResolveHint> rIt = getResolveHints().iterator(); rIt.hasNext();) {
				
				ResolveHint r = rIt.next();
				
				//fallback
				s += Types.Bytes;
				
				//clientBackend
				s += Types.Bytes;
				
				//number of fields
				s += Types.Bytes;
				
				if(r.getByteType() == ResolveHintType.HTTP_SET_PROXY.getValue()) {
				
					continue;
				}
				
				if(r.getFields() != null) {
					//typeValue == ResolveHintType.HTTP_SINGLE_DOMAIN.getValue()
					for(Iterator<ResolveHintField> rfIt = r.getFields().iterator(); rfIt.hasNext();) {
						
						ResolveHintField rhf = rfIt.next();
						
						// field clientBackend
						s += Types.Bytes;
						
						// value length
						s += Types.Bytes;
						
						if(rhf.getValue() != null) {
							
							s += rhf.getValue().getBytes().length;
						}
					}
				}
			}
		}
		
		if(geomaps==null)
			
			return s;
		
		//gfactor length
		s += Types.Bytes;
		
		if(getGfactor()!=null) {
			
			s += getGfactor().getBytes().length;
		}	
		
		for(Iterator<String> geoIt = geomaps.iterator(); geoIt.hasNext();) {
			
			s += geoIt.next().getBytes().length;
		}
		
		return s;
	}
	
	public String getDomain() {
		return domain;
	}
	
	public void setDomain(String domain) {
		
		this.domain = domain;
	}
	
	public short getInterval() {
		
		return interval;
	}
	
	public void setInterval(short interval) {
		
		this.interval = interval;
	}
	
	public boolean isAuto() {
		
		return auto;
	}
	
	public void setAuto(boolean auto) {
		
		this.auto = auto;
	}
	
	public boolean isNeedBackends() {
		
		return needBackends;
	}
	
	public void setNeedBackends(boolean needBackends) {
		
		this.needBackends = needBackends;
	}
	
	public String getNodeKey() {
		
		return nodeKey;
	}
	
	public void setNodeKey(String nodeKey) {
		
		this.nodeKey = nodeKey;
	}
	
	public List<Contact> getContacts() {
		
		return contacts;
	}
	
	public void setContacts(List<Contact> contacts) {
		
		this.contacts = contacts;
	}
	
	public String getNdomain() {
		
		return ndomain;
	}
	
	public void setNdomain(String ndomain) {
		
		this.ndomain = ndomain;
	}
	
	public void clearGeoMaps() {
		
		this.geomaps.clear();
	}
	
	public List<ResolveHint> getResolveHints() {
		
		return resolveHints;
	}

	public void setResolveHints(List<ResolveHint> resolveHints) {
		
		this.resolveHints = resolveHints;
	}
}