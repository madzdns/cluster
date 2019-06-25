package com.github.madzdns.server.core.backend;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.github.madzdns.server.core.config.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetaData implements IMeta,Externalizable {
	
	
	public final static byte STATE_DEL = 0;
	
	public final static byte STATE_VLD = 1;//valid
	
	public final static byte SYNCHED = 0;
	
	public final static byte NOT_SYNCHED = -1;
	
	private static Logger log = LoggerFactory.getLogger(MetaData.class);
	
	
	private byte version = 0;

	/***
	 * stores last modified date of GeoResolver
	 */
	private long modified;
	
	/***
	 * Stores data of GeoResolvers as byte. These data gets converted to xml
	 */
	//private GeoResolver resolver;
	
	private Resource resource;
	
	/***
	 * Whether this GeoResolver is deleted or valid or errorneous
	 */
	private byte state;
	
	/***
	 * This just determines if this meta data is changed or not
	 */
	private long lastModified = 0;

	/***
	 * name of this GeoResolver e.g. 123.frfra.com
	 */
	private String name = null;
	
	/***
	 * This determines if a res has been scheduled or not.
	 * -1 means not scheduled.
	 * 0 means scheduled
	 * any number greater than 0 means this is sent to that id
	 * but has not got receipt as scheduled
	 */
	private short schedule = NOT_SYNCHED;
	
	private boolean startup = true;
	
	public MetaData(){}
	
	/*protected MetaData(final String name,
			final long modified,
			GeoResolver resolver,
			final byte state) {*/
	
	protected MetaData(final String name,
			final long modified,
			Resource resource,
			final byte state) {
		
		//if(state == STATE_VLD && resolver.getContent() == null)
		if(state == STATE_VLD && (resource == null || resource.getContent() == null))
			throw new IllegalStateException();
		
		this.modified = modified;
		
		this.resource = resource;
		
		this.state = state;
		
		/*
		 * We always set the lastModified 0 in constructor to make changes visible to monitor.
		 * It was better to change names of modified and lastModified ;)
		 */
		this.lastModified = 0;

		this.name = name;
		
		/*
		 * Once a meta data is changed, this means its not scheduled with others.
		 * If a received res has every one else in his ids, means we are the last
		 * edge in the chain receiving this. So this should be changed to true in monitor
		 */
		this.schedule = NOT_SYNCHED;
		
		this.startup = false;
	}
	
	public long getModified() {
		
		return modified;
	}

	/*public GeoResolver getResolver() {
		
		return resolver;
	}*/
	
	public Resource getResolver() {
	
		return resource;
	}
	
	public byte[] getData() {
		
		//return resolver==null?null:resolver.getContent();
		
		return resource==null?null:resource.getContent();
	}

	public byte getState() {
		
		return state;
	}
	
	public void setState(byte state) {
		
		this.state = state;
	}
	
	public boolean isDeleted() {
		
		return state == STATE_DEL;
	}
	
	public boolean isValid() {
		
		return state == STATE_VLD;
	}
	
	/***
	 * This is only used in monitor to determine if this is changed
	 * so it can load it to resourses_zone, if it is updated, or remove it from resourses_zone
	 * if it is removed
	 * @return boolean
	 */
	public boolean isModified() {
		
		return modified>lastModified;
	}

	/***
	 * we should call this as soon as loaded this meta data to resourses_zone to prevent 
	 * it to be loaded next time
	 */
	public void clearModified() {
		
		this.lastModified = modified;
	}

	public String getName() {
		
		return name;
	}

	/**
	 * 
	 * @return true if the zone is sent to some other peer and
	 * also scheduled in the received peer
	 */
	public boolean isFullyScheduled() {
		
		return schedule == SYNCHED;
	}
	
	/**
	 * 
	 * @return true if the zone is sent to some other peer
	 */
	public boolean isScheduled() {
		
		return schedule > NOT_SYNCHED;
	}

	public void setScheduled(boolean scheduled) {
		
		this.schedule = (short)(scheduled?SYNCHED:NOT_SYNCHED);
	}

	public short getSchedule() {
		
		return schedule;
	}

	public void setSchedule(short schedule) {
		
		this.schedule = schedule;
	}

	public boolean isStartup() {
		
		return startup;
	}

	public void setStartup(boolean startup) {
		
		this.startup = startup;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {

		out.writeByte(version);
		
		out.writeObject(name);
		
		out.writeLong(modified);
		out.write(state);
		
		/*if(resolver!=null) {

			out.writeObject(resolver.getContent());
		}*/
		if(resource!=null) {

			out.writeObject(resource.getContent());
		}
		else
			out.writeObject(null);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		
		//this prevent sending contents at startup
		this.startup = true;
		
		//in startup nothing scheduled yet
		this.schedule = NOT_SYNCHED;
		
		this.version = in.readByte();
		
		name = (String) in.readObject();
		
		this.lastModified = 0;
		
		this.modified = in.readLong();
		
		this.state = in.readByte();

		byte[] data = (byte[]) in.readObject();
		
		if(data == null) {
			
			if( state != STATE_DEL )
				
				throw new ClassNotFoundException();
			
			//resolver = null;
			resource = null;
		}
		else {
			
			try {
				
				//this.resolver = GeoResolver.fromBytes(data);
				this.resource = Resource.fromBytes(data);
				
			} catch (Exception e) {
				
				log.error("",e);
				
				throw new ClassNotFoundException();
			}
		}
	}

	@Override
	public byte getVersion() {
		
		return this.version;
	}

	@Override
	public void setVersion(byte version) {
		
		this.version = version;
	}
	
	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("Zone=")
		.append(this.name)
		.append(" ,Status=")
		.append(isValid())
		.append(" ,Schedule=");
		
		return sb.toString();
	}
	
	/*public String toXmlString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<zone><name>")
		.append(this.name)
		.append("</name><status>")
		.append(isValid())
		.append("</status><schedule>")
		.append(isFullyScheduled())
		.append("</schedule><content>");
		
		if(resolver!=null 
				&& resolver.getContent()
				!=null) {
			
			sb.append("<![CDATA[")
			.append(new String(resolver.getContent()))
			.append("]]>");
		}
		
		sb.append("</content>")
		.append("</zone>");
		
		return sb.toString();
	}*/
	
	public String toXmlString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("<resource><name>")
		.append(this.name)
		.append("</name><status>")
		.append(isValid())
		.append("</status><schedule>")
		.append(isFullyScheduled())
		.append("</schedule><content>");
		
		if(resource!=null 
				&& resource.getContent()
				!=null) {
			
			sb.append("<![CDATA[")
			.append(new String(resource.getContent()))
			.append("]]>");
		}
		
		sb.append("</content>")
		.append("</resource>");
		
		return sb.toString();
	}
	
	@Override
	public int hashCode() {

		return String.valueOf(name).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if (obj instanceof MetaData) {
			
			MetaData o = (MetaData) obj;
			
			if (String.valueOf(o.getName()).equals(name)) {
				
				return true;
			}
		}
		return false;
	}

}
