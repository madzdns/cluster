package server.backend.kafka.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.backend.ZoneSynch;
import server.backend.kafka.codec.ClusterMessage;
import server.backend.kafka.codec.ResourceMessage;
import server.config.Config;

public class ConsumerHandler implements IKafkaMessageHandler {

	private static Logger log = LoggerFactory.getLogger(ConsumerHandler.class);
	
	@Override
	public void messageReceived(Object message, Object key, Object topic,
			long offset) {
		
		if(message instanceof ResourceMessage) {
			
			ResourceMessage m = (ResourceMessage) message;
			
			if(m.getSourceId() == Config.getApp().getId()) {
				
				//If the source is me, I already have called ZoneSynch.updateZoneSynch in wsi
				return;
			}
			
			try {
				
				ZoneSynch.updateZoneSynch(m.getZonename(), m.getLastModified(),m.getContent(), m.getCommand());
			} catch (Exception e) {
				
				log.error("",e);
			}
		}
		else if(message instanceof ClusterMessage) {
			
			ClusterMessage m = (ClusterMessage) message;
			
			if(m.getSourceId() == Config.getApp().getId()) {
				
				//If the source is me, I already have called ZoneSynch.updateZoneSynch in wsi
				return;
			}
			
			try {
				
				//here save the node
				
			} catch (Exception e) {
				
				log.error("",e);
			}
		}
	}

}
