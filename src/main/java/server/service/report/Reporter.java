package server.service.report;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.backend.node.dynamic.ServiceInfo;
import server.backend.node.dynamic.SystemInfo;
import server.config.Contact;

public class Reporter {
	
	private static Logger log = LoggerFactory.getLogger(Reporter.class);

	private static ConcurrentHashMap<String, ConcurrentHashMap<String,EventEntry>> eventsMap = 
			new ConcurrentHashMap<String,ConcurrentHashMap<String,EventEntry>>();
	
	public static void updateEvents(String node,List<Contact> contacts,int timeout,List<ServiceInfo> srvz,
			List<SystemInfo> sysz) {
		
		updateEvents(node,contacts,timeout,srvz,sysz,false);
	}
	
	public static void updateEvents(String node,List<Contact> contacts,int timeout,List<ServiceInfo> srvz,
			List<SystemInfo> sysz,boolean justad) {
		
		if(sysz==null||sysz.size()==0)
			sysz=null;
		
		if(srvz==null||srvz.size()==0)
			srvz=null;
		
		if((sysz==null&&srvz==null)
				||node==null)
			return;
		
		EventEntry eEntry = null;
		
		log.debug("");
		
		ConcurrentHashMap<String,EventEntry> nodeEvents = eventsMap.get(node);
		
		if(nodeEvents==null)
			nodeEvents = new ConcurrentHashMap<String, EventEntry>();
		
		if(srvz!=null)
		{
			for(ServiceInfo srv:srvz)
			{
				eEntry = nodeEvents.get(EventEntry.SRV_EVENT+srv.type+srv.port);
				if(eEntry!=null)
				{
					if(!justad)
						eEntry.update(contacts, null);
				}
				else
					nodeEvents.put(node,new EventEntry(contacts, EventEntry.SRV_EVENT, srv.type, srv.port));
			}
		}
		else if(sysz!=null)
		{
			for(SystemInfo sys:sysz)
			{
				eEntry = nodeEvents.get(EventEntry.SYS_EVENT+sys.type+sys.value);
				if(eEntry!=null)
				{
					if(!justad)
						eEntry.update(contacts,null);
				}
				else
					nodeEvents.put(node,new EventEntry(contacts, EventEntry.SYS_EVENT, sys.type, (short)sys.value));
			}
		}
		if(eEntry==null)
			return;
		long now = new Date().getTime();
		for(Iterator<Entry<String,EventEntry>> it=nodeEvents.entrySet().iterator();it.hasNext();)
		{
			Entry<String,EventEntry> entry = it.next();
			if(now-entry.getValue().getReportDate().getTime()>=timeout)
				it.remove();
		}
	}
	public static void updateEvents(String node,List<Contact> contacts,int timeout,SystemInfo sys) {
		
		updateEvents(node,contacts,timeout,sys);
	}
	
	public static void updateEvents(String node,List<Contact> contacts,int timeout,SystemInfo sys,boolean justad) {
		
		if(sys==null||node==null
				||contacts==null
				||contacts.size()==0)
			return;
		
		EventEntry eEntry = null;
		
		ConcurrentHashMap<String,EventEntry> nodeEvents = eventsMap.get(node);
		
		if(nodeEvents!=null) {
			
			eEntry = nodeEvents.get(EventEntry.SRV_EVENT+sys.type+sys.value);
			if(eEntry!=null) {
				
				if(!justad)
					
					eEntry.update(contacts,null);
			}
			else
				nodeEvents.put(node,new EventEntry(contacts, EventEntry.SYS_EVENT, sys.type, (short)sys.value));
		}
	}
}
