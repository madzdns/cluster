package com.github.madzdns.server.core.backend.kafka.codec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.madzdns.server.core.api.Types;
import com.github.madzdns.server.core.backend.MetaData;

public class ZoneSynchMessage extends SynchMessage {
	
	public final static byte MODE_SYNCH_ZONE = 0;
	
	public class MetaEntry
	{
		private short content_len;
		
		private byte[] content;
		
		private String zonename;
		
		private long lastModified;
		
		private Set<Short> ids;
		
		private byte command;
		
		private byte schedule;
		
		public MetaEntry(final short content_len, final byte[] content,final String filename,
				final long modified,final Set<Short> ids,final byte command, final byte schedule)
		{
			
			this.content_len=content_len;
			this.content=content;
			this.zonename=filename;
			this.lastModified = modified;
			this.ids = ids;
			this.command = command;
			this.schedule = schedule;
		}

		public short getContentLen() {
			
			return content_len;
		}

		public byte[] getContent() {
			
			return content;
		}

		public String getZoneName() {
			
			return zonename;
		}

		public long getLastModified() {
			
			return lastModified;
		}

		public Set<Short> getIds() {
			
			return ids;
		}

		public byte getCommand() {
			
			return command;
		}
		
		public byte getSchedule() {
			
			return schedule;
		}

		@Override
		public String toString() {
			
			StringBuilder sb = new StringBuilder();
			sb.append("Zone=")
			.append(zonename);
			
			return sb.toString();
		}
	}
	
	public class PartailEdgeEntry
	{
		private short id;
		
		private long lastModified = 0;
		
		public PartailEdgeEntry(final short id, final long lastModified) {
			
			this.id = id;
			this.lastModified = lastModified;
		}
		
		public short getId() {
			
			return id;
		}
		
		public void setId(short id) {
			
			this.id = id;
		}
		
		public long getLastModified() {
			
			return lastModified;
		}
		
		public void setLastModified(long lastModified) {
			
			this.lastModified = lastModified;
		}
	}

	private List<MetaEntry> metaList = new ArrayList<MetaEntry>();
	
	private List<PartailEdgeEntry> partialEdgeList = new ArrayList<PartailEdgeEntry>();
	
	
	public ZoneSynchMessage() {
		
		setMode(ZoneSynchMessage.MODE_SYNCH_ZONE);
	}
	
	public ZoneSynchMessage(short len) {
		
		super(len);
		setMode(ZoneSynchMessage.MODE_SYNCH_ZONE);
	}
	
	public void addMetaEntry(final byte[] content,final String name,
			final long modified,final Set<Short> ids,final byte command,
			final short schedule) {
		
		short clen = content==null?0:(short)content.length;
		metaList.add(new MetaEntry(clen,content,name,modified,ids,command,
				schedule> MetaData.NOT_SYNCHED?SCHEDULED:NOT_SCHEDULED));
	}
	
	public void addPartialEdge(final short id, final long lastModified) {
		
		partialEdgeList.add(new PartailEdgeEntry(id, lastModified));
	}

	public List<MetaEntry> getMetaEntries() {
		
		return metaList;
	}
	
	public List<PartailEdgeEntry> getEdgeEntries() {
		
		return partialEdgeList;
	}

	public short getMessageLength() {
		
		int len = super.getMessageLength();
		
		/*
		 * TODO later I'll decide if use this or not
		//Partial Edge Entry size
		len += Types.Bytes;
		if(partialEdgeList!=null)
		{
			for(Iterator<PartailEdgeEntry> it=partialEdgeList.iterator();it.hasNext();)
			{
				len += Types.ShortBytes+Types.LongBytes;
			}
		}
		*/
		
		if(metaList!=null&&metaList.size()>0) {
			
			for(Iterator<MetaEntry> it=metaList.iterator();it.hasNext();) {
				
				MetaEntry e = it.next();
				short content_len = e.content==null?(short)0:(short)e.content.length;
				//Holding contents length+contents themselves
				
				/*
				 * content_len + content + zone_name_len + zone_name + last_modified + command + schedule
				 */
				len += Types.ShortBytes+content_len+Types.ShortBytes+e.zonename.getBytes().length
						+Types.LongBytes+Types.Bytes+Types.Bytes;
				
				//awareIds len
				len += Types.Bytes;
				if(e.getIds()!=null) {
					
					len += Types.ShortBytes*e.getIds().size();
				}
			}
		}
		
		return (short)len;
	}
}
