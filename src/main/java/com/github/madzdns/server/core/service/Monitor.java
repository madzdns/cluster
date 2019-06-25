package com.github.madzdns.server.core.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.Date;
import java.util.Set;

import com.github.madzdns.server.core.backend.node.INode;
import com.github.madzdns.server.core.backend.node.dynamic.impl.Node;
import com.github.madzdns.server.core.backend.node.statics.HeartbeatWorker;
import com.github.madzdns.server.core.config.Config;
import com.github.madzdns.server.core.config.GeoResolver;
import com.github.madzdns.server.core.config.MyEntry;
import com.github.madzdns.server.core.config.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.ClusterNode;

import com.github.madzdns.server.dns.ZoneManager;
import com.github.madzdns.server.core.api.Resolver;
import com.github.madzdns.server.core.backend.MetaData;
import com.github.madzdns.server.core.backend.ZoneSynch;
import com.github.madzdns.server.core.backend.frsynch.Frsynch;
import com.github.madzdns.server.core.backend.kafka.KafkaTopics;
import com.github.madzdns.server.core.backend.kafka.codec.ResourceMessage;
import com.github.madzdns.server.core.backend.kafka.consumer.KafkaConsumerImpl;
import com.github.madzdns.server.core.backend.kafka.producer.KafkaProducerImpl;
import com.github.madzdns.server.core.utils.Dictionary;

public class Monitor {
	
	public final static class XmlFileFilter implements FilenameFilter {
		/**
		 * Check whether file matches filter rules
		 * 
		 * @param dir Directory
		 * @param name File name
		 * @return true If file does match filter rules, false otherwise
		 */
		public boolean accept(File dir, String name) {
			
			return name.endsWith(".xml");
		}
	}
	
	private static Logger log = LoggerFactory.getLogger(Monitor.class);
	
	private static Object s_mutex = new Object();
	
	private static long clusterLastModified = 0;
	
	public static void doMonitor() {
		
		ClusterNode mine = Frsynch.getContext().getMyInfo();
		
		if(Config.getMonitor().getInterval() != mine.getMonitorInterval()) {
			
			Config.getMonitor().cancle();
			
			Config.setMonitor(new ClockWork(mine.getMonitorDelay(), mine.getMonitorInterval(), new TimerTask() {
				
				@Override
				public void run() {
					
					Monitor.doMonitor();
				}
			}, "monitor"));
		}

		if(Config.getReport().getInterval() != mine.getReportInterval()) {
			
			Config.getReport().cancle();
			
			Config.setReport(new ClockWork(mine.getReportDelay(), mine.getReportInterval(), new TimerTask() {
				
				@Override
				public void run() {
					
					Monitor.healthCheck();
				}
			}, "report"));
		}
		
		if(!mine.isValid()) {
			
			log.error("Reporting from after life!");
			
			return; 	
		}
		
		if(Config.getApp().isKafkaCluster()) {
			KafkaConsumerImpl.consume();
		}
		
		XmlFileFilter xmlFileFilter = new XmlFileFilter();
		
		String resPath = Config.RESPATH;
		
		File resDir = new File(resPath);
		
		File[] resFiles = resDir.listFiles(xmlFileFilter);
		
		String zname = null;

		if(resFiles != null) {
			
			for(File res:resFiles) {
				
				zname = res.getName().substring(0, res.getName().length()-4);
				
				MetaData meta = ZoneSynch.getMetaDataSynch(zname);
				
				if( meta != null && res.lastModified() < meta.getModified() ) {
					/*
					 * This means we already have a newer version of this.
					 * Rename invalid resource here
					 */
					res.renameTo(new File(res.getAbsolutePath()+".stale.err"));
					continue;
				}
				
				FileInputStream fis = null;
				
				try {
					
					fis = new FileInputStream(res);
					
				} catch (FileNotFoundException e1) {
					
					
					log.error("",e1);
					
					/*
					 * Some error occurred. So rename this either.
					 */
					//res.renameTo(new File(res.getAbsolutePath()+".err"));
					
					continue;
				}

				try {
					
					log.debug("Loading {} from resources", zname);
					
					if(Config.getApp().isKafkaCluster()) {
						
						ResourceMessage message = new ResourceMessage(fis, zname, res.lastModified(),
								Config.getApp().getId(), MetaData.STATE_VLD);
						
						KafkaProducerImpl.send(KafkaTopics.RESOURCES, zname, message);
						
						fis.close();
						
						fis = new FileInputStream(res);
					}
					
					
					ZoneSynch.updateZoneSynch(zname,res.lastModified(),MetaData.STATE_VLD,fis);
					
				} catch (Exception e) {
					
					log.error("",e);
					
					/*
					 * Some error occurred. So rename this too.
					 */
					res.renameTo(new File(res.getAbsolutePath()+".err"));
					
					continue;
				}
				finally {
					
					try {
						
						fis.close();
					} catch (IOException e) {}
				}
				
				/*
				 * Rename consumed valid resource here
				 */
				res.renameTo(new File(res.getAbsolutePath()+".ok"));
			}	
		}
		
		//Get last modified meta data map
		Collection<MetaData> lastModifieds = ZoneSynch.getMetaLastModifiedsSynch();
		
		if(log.isDebugEnabled()) {
			
			log.debug("last modified resources {}", lastModifieds);
		}
		
		if(lastModifieds != null
				&& lastModifieds.size() > 0 ) {
			
			/*
			 * Here we try to detect all meta data that were in the resource_zone
			 * so we could unload them to take effects
			 */
			//List<GeoResolver> modified_resourses = new ArrayList<GeoResolver>();
			List<Resource> modified_resourses = new ArrayList<Resource>();
			
			List<MetaData> all_new_resourses = new ArrayList<MetaData>();
			
			/*
			 * Since lastModifieds are not empty, some meta data are modified. 
			 * Note all meta data are considered as modified after each restarts
			 */
			for(Iterator<MetaData> it = lastModifieds.iterator();
					it.hasNext();) {
				
				MetaData meta = it.next();
				
				/*GeoResolver gr = Config.getGeoResolver(meta.getName());
				
				if(gr != null) {
					
					GeoResolver metaResolver = meta.getResolver();
					
					if(metaResolver != null && metaResolver.getRequestCount()!= null &&
							gr.getRequestCount() != null &&
							!metaResolver.isNewPlanZone() && 
							metaResolver.getRequestCount().getCount() > -1
							&& gr.getRequestCount().getTotalUsedValue() > 
							metaResolver.getRequestCount().getTotalUsedValue()) {

						meta.getResolver().getRequestCount().setTotalUsedValueIfGreater(gr.getRequestCount().getTotalUsedValue());
					}
					
					modified_resourses.add(gr);
				}*/
				
				if(!meta.isDeleted() && meta.getResolver() != null && meta.getResolver().getGeoResolvers() != null) {
				
					for(Iterator<GeoResolver> gIt = meta.getResolver().getGeoResolvers().iterator(); gIt.hasNext();) {
						
						GeoResolver metaGr = gIt.next();
						GeoResolver currentgr = Config.getGeoResolver(metaGr.getZone());
						
						if(currentgr != null) {
							
							if(metaGr != null && metaGr.getRequestCount()!= null &&
									currentgr.getRequestCount() != null &&
									!metaGr.isNewPlanZone() && 
									metaGr.getRequestCount().getCount() > -1
									&& currentgr.getRequestCount().getTotalUsedValue() > 
									metaGr.getRequestCount().getTotalUsedValue()) {

								metaGr.getRequestCount().setTotalUsedValueIfGreater(currentgr.getRequestCount().getTotalUsedValue());
							}
						}
					}
				}
				
				Resource currentRes = Config.getResource(meta.getName()); 
				
				if(currentRes != null) {
				
					modified_resourses.add(currentRes);
				}
				
				/*
				 * Changes the modified state to not modified
				 * TODO why?
				 */
				meta.clearModified();
				
				/*
				 * Whether its modified or not, all zone data here
				 * are considered new
				 */
				all_new_resourses.add(meta);
				
			}
			
			log.debug("Updating mappers");
			/*
			 * Here because I first reload resources and then attempt to purge relevant
			 * data structures, there is no possibility of screwing up between the three
			 * resource maps in Configure class i.e (resourses_zone,resourses_key)
			 * Besides that, there is no screwing up for edge's structures
			 */

			Config.getZoneResz(all_new_resourses);
			
			all_new_resourses = null;
			
			ZoneManager.setupZones();

			if(Resolver.getMapperNode() != null
					&& Resolver.getMapperNode().size()>0
					&& modified_resourses.size()>0) {
				
				for(int k=0; k< modified_resourses.size(); k++) {
					
					Resource rr = modified_resourses.get(k);
					
					List<GeoResolver> modifiedGeoMappersList = rr.getGeoResolvers();
					
					if(modifiedGeoMappersList == null) {
						
						continue;
					}
					
					for(int i=0; i < modifiedGeoMappersList.size();i++) {
						
						GeoResolver gr = modifiedGeoMappersList.get(i);
						
						List<MyEntry> myEtList = gr.getEntries();
						
						if(myEtList==null)
							
							continue;
						
						for(int j=0;j<myEtList.size();j++) {
							
							MyEntry me = myEtList.get(j);

							Dictionary<Set<INode>, MyEntry> dict = Resolver.getApiMapper().get(me.getFullAName());
							
							if(dict == null)
								
								continue;
							
							Set<INode> nodes = dict.getValue1();
							
							if(nodes == null)
								
								continue;
							
							List<INode> inodeList = new ArrayList<INode>(nodes);
							
							nodes = null;
							
							if(me.getFullAName() == null) {
								
								String fullAName = new StringBuilder(me.getA()).
										append(".").append(gr.getZone()).append(".")
										.append(Config.MAIN_DOMAIN_STR).toString();
								
								me.setFullAName(fullAName);
							}
							
							HeartbeatWorker.removeJobsForDomain(me.getFullAName());
							
							for(int c = 0; c < inodeList.size(); c++) {
								
								Node node = (Node) inodeList.get(c);
								
								node.getEntryParamsMapper().remove(me.getApikey());
								
								node.getEntryParamsMapper().remove(me.getFullAName());
								
								node.removeResolver(gr.getZone());
								
								if(node.getEntryParamsMapper().size()==0) {
									
									Resolver.purgeNodeMapper(node);
								}
								
								node = null;
							}
							
							inodeList = null;
							
							Resolver.getApiMapper().remove(me.getApikey());
							
							Resolver.getApiMapper().remove(me.getFullAName());
							
							me = null;
						}
					}	
				}
				//After Removing modified resources, Let the dynamic nodes connect again and catch them.
			}
			
			Config.reloadFixeds();
			
			modified_resourses = null;
		}
		
		lastModifieds = null;
		
		xmlFileFilter = null;
		
		resPath = null;
		
		resDir = null;
		
		resFiles = null;
		
		zname = null;
	}
	
	/*
	 * If cluster synch process failed, this way we make Monitor
	 * to start it over
	 */
	public static void downModifiedOnFaliure(long lastModified) {
		
		synchronized (s_mutex) {
			
			/*
			 * This if prevents altering changed clusterLastModified
			 */
			if(Monitor.clusterLastModified == lastModified) {
				
				Monitor.clusterLastModified = 0;
			}
		}
	}
	
	public static void catchStopedNode(String node,long id) {
		
		INode iedge = Resolver.getMapperNode().get(node);
		
		if( iedge == null ) {
			
			log.error("Could not get any node with address",node);
			return;
		}
		
		Node edge = (Node) iedge;
		
		//TODO call for Report
		
		/*
		 * count downing all resolvers.
		 * If a node was buggy, we don't set
		 * close Handler for it and also don't
		 * count up resolver to it. So don't worry here.
		 */
		edge.countDwnResolvers();
		
		if( id != 0 && edge.getId()!=id ) {
			
			log.error("Processing to clean up stop node {} stoped. Because it has id {} which is different than {}.",node,edge.getId(),id);
			return;
		}
		
		log.info("Node {} has stoped reporting",node);
		
		for(Iterator<Entry<String, Node.EntryParam>> it = edge.getEntryParamsMapper().entrySet().iterator()
			; it.hasNext();) {
			
			Entry<String, Node.EntryParam> see = it.next();
			
			log.info("Removing Node {} from api {}",node,see.getKey());
			
			Resolver.removeNodeFromApizSafe(see.getKey(),edge);
		}
		
		Resolver.purgeNodeMapper(edge);
	}
	
	public static void healthCheck() {
		
		long now = new Date().getTime();
		
		for(Iterator<Entry<String,INode>> it=Resolver.getMapperNode().entrySet().iterator();it.hasNext();) {
			
			Entry<String,INode> ientry = it.next();
			
			Node e = (Node)ientry.getValue();
			
			if(now-e.getNodeLatestReport() > e.getTimeout()) {
				
				log.info("Node {} has stoped reporting",e.getServer());
				
				for(Iterator<Entry<String, Node.EntryParam>> sit = e.getEntryParamsMapper().entrySet().iterator()
					; sit.hasNext();) {
					
					Entry<String, Node.EntryParam> see = sit.next();
					
					if(!see.getValue().isFixed()) {

						log.info("Removing Node {} from api {}",e.getServer(),see.getKey());
						
						Resolver.removeNodeFromApizSafe(see.getKey(),e);	
					}
				}
				
				log.info("Removing Node[core={}]",e.getServer());
				
				//TODO call for Report
				
				e.countDwnResolvers();
				
				Resolver.sessNodeCleanUp(e.getServer(), e.getId());
				
				it.remove();
			}
		}
	}
}
