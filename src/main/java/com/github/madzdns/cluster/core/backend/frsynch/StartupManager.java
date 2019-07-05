package com.github.madzdns.cluster.core.backend.frsynch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.github.madzdns.cluster.core.backend.MetaData;
import com.github.madzdns.cluster.core.backend.ZoneSynch;
import com.github.madzdns.cluster.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.SynchContext;
import com.frfra.frsynch.SynchFeature;
import com.frfra.frsynch.SynchResult;
import com.frfra.frsynch.SynchType;
import com.frfra.frsynch.codec.IMessage;

public class StartupManager {
	
	private static Logger log = LoggerFactory.getLogger(StartupManager.class);

	public boolean startSynchingResourcesWithCluster(SynchContext ctx, short myId) {
		
		
		log.info("Starting to synch resources with the cluster");
		
		ZoneSynchMessage message = new ZoneSynchMessage(null, "ask_replication", -1, 
				(byte) 0, (byte)0);
		
		SynchFeature sf = Frsynch.getContext()
				.make(SynchType.UNICAST_ONE_OF)
				.withoutCluster(Config.getApp().getId())
				.withCallBack(new ZoneSynchCallback())
				.withEncoder(ZoneSynchMessage.class)
				.synch(message).get();
		
		if(sf != null && sf.get("ask_replication").isSuccessful()) {
			
			return true;
		}
		
		return false;
	}
	
	public void startSynchingResourcesToCluster(SynchContext ctx, short rcivingNodeId) {

		log.info("Starting to synch resources to node {}", rcivingNodeId);
		
		List<MetaData> resources = ZoneSynch.getMetasListSynch();
		
		if(resources != null) {
			
			List<IMessage> messages = new ArrayList<IMessage>();
			
			for(Iterator<MetaData> it = resources.iterator(); it.hasNext(); ) {
				
				MetaData m = it.next();
				
				messages.add(new ZoneSynchMessage(m.getData(), m.getName(), m.getModified(),
						m.isDeleted()?ZoneSynchMessage.COMMAND_DEL_THis:ZoneSynchMessage.COMMAND_TAKE_THis, m.getSchedule()));
			}
			
			if(messages.size() > 0) {
				
				SynchFeature sf = Frsynch.getContext()
						.make(SynchType.UNICAST)
						.withCluster(rcivingNodeId)
						.withCallBack(new ZoneSynchCallback())
						.withEncoder(ZoneSynchMessage.class)
						.synch(messages).get();
				
				if(sf != null) {
					
					for(Iterator<Entry<String, SynchResult>> it = sf.entrySet().iterator(); it.hasNext();) {
						
						Entry<String, SynchResult> eIt = it.next();
							
						log.info("Synching {} with node was {}", eIt.getKey(), eIt.getValue().isSuccessful());
					}
				}
			}
		}
	}
}
