package server.backend.kafka.codec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import server.api.Types;
import server.backend.BackendAddress;

public class ClusterSynchMessage extends SynchMessage {

	public final static byte MODE_SYNCH_CLUSTER = 1;

	public final static byte SYNCHED = 0;
	public final static byte NOT_SYNCHED = -1;
	
	public class EdgeEntry {
		
		private short id;
		
		private boolean useSsl = true;
		
		private boolean authByKey = true;
		
		private String key;
		
		private long lastModified = 0;
		
		private Set<Short> awareIds = null;
		
		private Set<BackendAddress> edgeAddrz;
		
		private Set<BackendAddress> backendAddrz;
		
		private byte command;
		
		private short monitorInterval = 600;
		
		private short monitorDelay = 20;
		
		private short reportInterval = 600;
		
		private short reportDelay = 20;
		
		private byte schedule;
		
		public EdgeEntry(final short id,
				final boolean useSsl,
				final boolean authByKey,
				final String key,
				long modified,
				final Set<Short> awareIds,
				final Set<BackendAddress> edgeAddrz,
				final Set<BackendAddress> backendAddrz,
				byte command,
				final short monitorInterval,
				final short monitorDelay,
				final short reportInterval,
				final short reportDelay,
				final byte schedule) {
			
			this.id=id;
			this.useSsl=useSsl;
			this.authByKey=authByKey;
			this.key = key;
			this.lastModified = modified;
			this.awareIds = awareIds;
			this.edgeAddrz = edgeAddrz;
			this.backendAddrz = backendAddrz;
			this.command = command;
			
			this.monitorDelay = monitorDelay;
			this.monitorInterval = monitorInterval;
			this.reportDelay = reportDelay;
			this.reportInterval = reportInterval;
			this.schedule = schedule;
			
		}

		public short getId() {
			
			return id;
		}

		public void setId(short id) {
			
			this.id = id;
		}

		public boolean isUseSsl() {
			
			return useSsl;
		}

		public void setUseSsl(boolean useSsl) {
			
			this.useSsl = useSsl;
		}

		public boolean isAuthByKey() {
			
			return authByKey;
		}

		public void setAuthByKey(boolean authByKey) {
			
			this.authByKey = authByKey;
		}

		public String getKey() {
			
			return key;
		}

		public void setKey(String key) {
			
			this.key = key;
		}

		public long getLastModified() {
			
			return lastModified;
		}

		public void setLastModified(long lastModified) {
			
			this.lastModified = lastModified;
		}

		public Set<Short> getAwareIds() {
			
			return awareIds;
		}

		public void setAwareIds(Set<Short> awareIds) {
			
			this.awareIds = awareIds;
		}

		public Set<BackendAddress> getEdgeAddrz() {
			
			return edgeAddrz;
		}

		public void setEdgeAddrz(Set<BackendAddress> edgeAddrz) {
			
			this.edgeAddrz = edgeAddrz;
		}

		public Set<BackendAddress> getBackendAddrz() {
			
			return backendAddrz;
		}

		public void setBackendAddrz(Set<BackendAddress> backendAddrz) {
			
			this.backendAddrz = backendAddrz;
		}

		public byte getCommand() {
			
			return command;
		}

		public void setCommand(byte command) {
			
			this.command = command;
		}
		
		public short getMonitorInterval() {
			
			return monitorInterval;
		}

		public void setMonitorInterval(short monitorInterval) {
			
			this.monitorInterval = monitorInterval;
		}

		public short getReportInterval() {
			
			return reportInterval;
		}

		public void setReportInterval(short reportInterval) {
			
			this.reportInterval = reportInterval;
		}

		public short getMonitorDelay() {
			
			return monitorDelay;
		}

		public void setMonitorDelay(short monitorDelay) {
			
			this.monitorDelay = monitorDelay;
		}

		public short getReportDelay() {
			
			return reportDelay;
		}

		public void setReportDelay(short reportDelay) {
			
			this.reportDelay = reportDelay;
		}
		
		public byte getSchedule() {
			
			return schedule;
		}
	}
	
	public final static byte USE_SSL = 1;
	
	public final static byte AUTH_BY_KEY = 1;
	
	private List<EdgeEntry> edgeEntries = new ArrayList<EdgeEntry>();
	
	private EdgeEntry srcEdgeEntry = null;
	
	public ClusterSynchMessage() {
		
		setMode(ClusterSynchMessage.MODE_SYNCH_CLUSTER);
	}
	
	public ClusterSynchMessage(short len) {
		
		super(len);
		setMode(ClusterSynchMessage.MODE_SYNCH_CLUSTER);
	}
	
	public void addEdgeEntry(final short id,
			final boolean useSsl,
			final boolean authByKey,
			final String key,
			final long modified,
			final Set<Short> awareIds,
			final Set<BackendAddress> edgeAddrz,
			final Set<BackendAddress> backendAddrz,
			final byte command,
			final short monitorDelay,
			final short monitorInterval,
			final short reportDelay,
			final short reportInterval,
			final short schedule) {
		
		EdgeEntry eEntry = new EdgeEntry(id, useSsl, authByKey, key, modified, awareIds,
				edgeAddrz, backendAddrz, command, monitorDelay, monitorInterval, 
				 reportDelay, reportInterval,schedule>NOT_SYNCHED?SCHEDULED:NOT_SCHEDULED);
		
		edgeEntries.add(eEntry);
		
		if(id == getId()) {
			
			srcEdgeEntry = eEntry;
		}
	}
	
	@Override
	public short getMessageLength() {
		
		short len = super.getMessageLength();
		
		//edges entries count
		len += Types.Bytes;
		
		if(edgeEntries!=null&&edgeEntries.size()>0) {
			
			for(Iterator<EdgeEntry> it=edgeEntries.iterator();it.hasNext();) {
				
				EdgeEntry edge = it.next();
				
				//holding id
				len += Types.ShortBytes;
				
				//key size
				len += Types.Bytes;
				
				if(edge.getKey()!=null)
					len += edge.getKey().getBytes().length;
				
				//usessl
				len += Types.Bytes;
				
				//authbykey
				len += Types.Bytes;
				
				//command
				len += Types.Bytes;
				
				//schedule
				len += Types.Bytes;
				
				//last modified
				len += Types.LongBytes;
				
				//awareIds len
				len += Types.Bytes;
				if(edge.getAwareIds()!=null) {
					
					len += Types.ShortBytes*edge.getAwareIds().size();
				}
				
				//size of edge addresses
				len += Types.Bytes;
				if(edge.getEdgeAddrz()!=null) {
					
					for(Iterator<BackendAddress> it2=edge.getEdgeAddrz().iterator();it2.hasNext();) {
						
						BackendAddress addr = it2.next();
						len += (Types.Bytes + addr.getAddress().getAddress().length + Types.ShortBytes);
					}
				}
				
				//size of backend addresses
				len += Types.Bytes;
				if(edge.getBackendAddrz()!=null) {
					
					for(Iterator<BackendAddress> it2=edge.getBackendAddrz().iterator();it2.hasNext();) {
						
						BackendAddress addr = it2.next();
						len += (Types.Bytes + addr.getAddress().getAddress().length + Types.ShortBytes);
					}
				}
				
				//reports and monitors
				len += (Types.ShortBytes * 4);
			}
		}
		
		return len;
	}

	public List<EdgeEntry> getEdgeEntries() {
		
		return edgeEntries;
	}

	public EdgeEntry getSrcEdgeEntry() {
		
		return srcEdgeEntry;
	}

}
