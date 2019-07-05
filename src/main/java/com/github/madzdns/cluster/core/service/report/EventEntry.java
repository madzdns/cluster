package com.github.madzdns.cluster.core.service.report;

import java.util.Date;
import java.util.List;

import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.cluster.core.backend.node.dynamic.SystemInfo;
import com.github.madzdns.cluster.core.config.Contact;

public class EventEntry {

	public static final String SRV_EVENT = "Service";
	public static final String SYS_EVENT = "System";
	private String event;
	private String type;
	private String value;
	private List<Contact> contacts;
	private Date eventDate;
	private Date reportDate;
	
	public EventEntry(List<Contact> contacts,String event,byte type,short value)
	{
		this.contacts = contacts;
		this.event=event;
		this.type= String.valueOf(type);
		this.value= String.valueOf(value);
		this.eventDate = new Date();
	}
	
	public void update(List<Contact> contacts,Integer value)
	{
		this.eventDate = new Date();
		if(value!=null)
			this.value = value.toString();
		this.contacts = contacts;
	}
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof ServiceInfo)
		{
			ServiceInfo srv = (ServiceInfo) obj;
			if(this.type.equals(srv.type+"")
					&&this.value.equals(srv.value+""))
				return true;
			return false;	
		}
		if(obj instanceof SystemInfo)
		{
			SystemInfo sys = (SystemInfo) obj;
			if(this.type.equals(sys.type+""))
				return true;
			return false;	
		}
		return false;
	}
	public String getEvent() {
		return event;
	}
	public String getType() {
		return type;
	}
	public String getValue() {
		return value;
	}
	public List<Contact> getContacts()
	{
		return contacts;
	}
	public Date getReportDate() {
		return reportDate;
	}
	public void setEventDate(Date eventDate) {
		this.eventDate = eventDate;
	}
	public Date getEventDate() {
		return eventDate;
	}
}
