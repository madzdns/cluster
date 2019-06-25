package server.backend.frsynch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.backend.MetaData;
import server.backend.ZoneSynch;

import com.frfra.frsynch.ISession;
import com.frfra.frsynch.ISynchCallbak;
import com.frfra.frsynch.ISynchProtocolOutput;
import com.frfra.frsynch.SynchFeature;
import com.frfra.frsynch.codec.IMessage;

public class ZoneSynchCallback implements ISynchCallbak {

	private static Logger log = LoggerFactory.getLogger(ZoneSynchCallback.class);
	
	@Override
	public boolean callBack(ISession session, IMessage message,
			Set<Short> withNodes, ISynchProtocolOutput out) {
		
		boolean result = false;
		ZoneSynchMessage outMsg = null;
		
		ZoneSynchMessage e = (ZoneSynchMessage) message;
		log.debug("RCVD command {} for message {}", e.getCommand(), e.getKey());
		
		if("ask_replication".equals(e.getKey())) {
			
			List<MetaData> resources = ZoneSynch.getMetasListSynch();
			
			log.debug("RCVD Msg typ was ask_replication, Replicating {}", resources);
			
			if(resources != null) {
				
				List<IMessage> responses = new ArrayList<IMessage>();
				
				for(Iterator<MetaData> it = resources.iterator(); it.hasNext(); ) {
					
					MetaData m = it.next();
					
					responses.add(new ZoneSynchMessage(m.getData(), m.getName(), m.getModified(),
							m.isDeleted()?ZoneSynchMessage.COMMAND_DEL_THis:ZoneSynchMessage.COMMAND_TAKE_THis, m.getSchedule()));
				}
				
				if(responses.size() > 0) {
					
					out.write(responses);
				}
			}
			
			return false;
		}
		
		MetaData meta = ZoneSynch.getMetaDataSynch(e.getKey());
		log.debug("existing meta for key {}, is {}", e.getKey(), meta);
		if(meta == null) {
			
			if(e.getCommand()==ZoneSynchMessage.COMMAND_DEL_THis) {
				
				try {
					
					ZoneSynch.updateZoneSynch(e.getZoneName(), e.getLastModified(),
							null, MetaData.STATE_DEL);
					
				} catch (Exception e1) {
					
					log.error("",e1);
					
					return result;
				}
				
				outMsg = new ZoneSynchMessage(null, e.getZoneName(),
						e.getLastModified(), ZoneSynchMessage.COMMAND_OK, MetaData.NOT_SYNCHED);
				result = true;
			}
			else if(e.getCommand()==ZoneSynchMessage.COMMAND_GIVE_THis
					||e.getCommand()==ZoneSynchMessage.COMMAND_OK
					||e.getCommand()==ZoneSynchMessage.COMMAND_RCPT_THis) {
				
				/*
				 * I checked every possibilities and this situation can't happen.
				 * Anyway for the sake of completeness, I put a continue here
				 */
				return result;
			}
			//TAKE command
			else if(e.getContent() != null) {
				
				try {

					ZoneSynch.updateZoneSynch(e.getZoneName(), e.getLastModified(),
							e.getContent(),MetaData.STATE_VLD);
					
				} catch (Exception e1) {
					
					log.error("",e1);
					
					return result;
				}
				
				outMsg = new ZoneSynchMessage(null, e.getZoneName(), e.getLastModified(),
						ZoneSynchMessage.COMMAND_OK, MetaData.NOT_SYNCHED);
				
				result = true;
			}
			else {

				outMsg = new ZoneSynchMessage(null, e.getZoneName(), e.getLastModified(),
						ZoneSynchMessage.COMMAND_GIVE_THis, MetaData.NOT_SYNCHED);
				
				result = false;
			}
		}
		else {
			
			/*
			 * 
			 */
			meta.setStartup(false);
			
			if(e.getCommand()==ZoneSynchMessage.COMMAND_GIVE_THis) {
				
				/*
				 * Look I don't think someone asks something I don't have,
				 * But I try to be able to answer everything 
				 */
				byte cmd;
				
				byte[] data = null;
				
				if(meta.isDeleted()) {
					
					cmd = ZoneSynchMessage.COMMAND_DEL_THis;
				}
				else {
					
					cmd = ZoneSynchMessage.COMMAND_TAKE_THis;
					
					data = meta.getData();
				}
				
				outMsg = new ZoneSynchMessage(data,meta.getName(),
						meta.getModified(),cmd, MetaData.NOT_SYNCHED);
				
				result = false;
			}
			else if(e.getCommand()==ZoneSynchMessage.COMMAND_DEL_THis) {
				
				if(meta.isValid()) {
					
					if(meta.getModified() < e.getLastModified()) {
						
						try {
							
							ZoneSynch.updateZoneSynch(e.getZoneName(), e.getLastModified(),
									null, MetaData.STATE_DEL);
							
						} catch (Exception e1) {
							
							log.error("",e1);
							return result;
						}
						
						outMsg = new ZoneSynchMessage(null, e.getZoneName(),
								e.getLastModified(), ZoneSynchMessage.COMMAND_OK, MetaData.NOT_SYNCHED);
						
						result = true;
					}
					else if(meta.getModified() > e.getLastModified()) {
						
						outMsg = new ZoneSynchMessage(meta.getData(), e.getZoneName(), meta.getModified(),
								ZoneSynchMessage.COMMAND_TAKE_THis, MetaData.NOT_SYNCHED);
						result = false;
					}	
					/*
					 * else I assume it is impossible for the same last modified one is deleted but mine is not
					 */
				}
				else {
					
					/*
					 * This means we are both have this deleted. So just send OK
					 * I don't think its matter to check modification here. But be careful, 
					 * if use send back OK here using your own lastModified, it leads to a loop.
					 * Thats why I've used e.getLastModified() here
					 */
					
					outMsg = new ZoneSynchMessage(null, e.getZoneName(), e.getLastModified(),
							ZoneSynchMessage.COMMAND_OK, meta.getSchedule());
					
					result = true;
				}
			}
			else/*He sent TAKE or OK or RCPT*/{
				
				log.debug("existing resource version {} versus msg version {} for key {}",
						meta.getModified(), e.getLastModified(), e.getKey());
				
				if(meta.getModified()==e.getLastModified()) {
					
					if(e.getCommand()==ZoneSynchMessage.COMMAND_TAKE_THis) {
						
						outMsg = new ZoneSynchMessage(null, meta.getName(),meta.getModified(),
								ZoneSynchMessage.COMMAND_OK,meta.getSchedule());
						
						result = true;
						
						/*
						 * If we got a take message that we already
						 * have that one, there might be coming from 
						 * a restarted edge. So for a temporary fix,
						 * we make that zone as not scheduled
						 */
						meta.setScheduled(false);
					}
					else {
						
						if( e.getCommand() == ZoneSynchMessage.COMMAND_RCPT_THis) {

							/*
							 * He is asking for RCPT
							 */
							
							outMsg = new ZoneSynchMessage(null, meta.getName(),meta.getModified(),
									ZoneSynchMessage.COMMAND_OK,meta.getSchedule());
							
							result = true;
						}
						else { //this is an OK
							
							//if(e.getSchedule() == ZoneSynchMessage.SCHEDULED) {
								
								/*
								 * Here is the only where we set a zone data as scheduled
								 */
								meta.setScheduled(true);
								result = true;
							//}
						}
						
					}
				}
				else if(meta.getModified() < e.getLastModified()) {
					
					/*
					 * Here checking e.getContent() is equivalent of checking COMMAND_OK. in both case they are
					 * null 
					 */
					if( e.getContent() != null ) {
						
						try {
							
							ZoneSynch.updateZoneSynch(e.getZoneName(), e.getLastModified(),
									e.getContent(),MetaData.STATE_VLD);
							
						} catch (Exception e1) {
							
							log.error("",e1);
							return result;
						}
						
						outMsg = new ZoneSynchMessage(null, e.getZoneName(),
								e.getLastModified(), ZoneSynchMessage.COMMAND_OK, MetaData.NOT_SYNCHED);
						
						result = true;
					}
					else {
						
						outMsg = new ZoneSynchMessage(null, e.getZoneName(), e.getLastModified(),
								ZoneSynchMessage.COMMAND_GIVE_THis, MetaData.NOT_SYNCHED);
						
						result = false;
					}
				}
				else /* if(meta.getModified()>e.getLastModified()) */ {
					
					outMsg = new ZoneSynchMessage(meta.getData(),e.getZoneName(),meta.getModified(),
							 meta.isDeleted()?ZoneSynchMessage.COMMAND_DEL_THis
									:ZoneSynchMessage.COMMAND_TAKE_THis, MetaData.NOT_SYNCHED);
					
					result = false;
				}
			}
		}

		out.write(outMsg);
		return result;
	}

	@Override
	public void result(SynchFeature synchFeature) {
		
		
	}

}
