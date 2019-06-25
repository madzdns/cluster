package rtsp;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RTSPSessionTransports {

	ConcurrentMap<Integer, RTSPTransport> transports = new ConcurrentHashMap<Integer, RTSPTransport>();
	
	public void addTransport(int trakId,RTSPTransport transport) {
		
		transports.put(trakId, transport);
	}
	
	public RTSPTransport getRTSPTransport(int trackId) {
		
		return transports.get(trackId);
	}
	
	public String createTransportInfo(int trackId) {
		
		if(trackId < 0 ) {
			
			StringBuilder sb = new StringBuilder();
			
			boolean firstStep = true;
			
			for(Iterator<Entry<Integer,RTSPTransport>> it = transports.entrySet().iterator()
					;it.hasNext();) {
				
				RTSPTransport t = it.next().getValue();
				
				if(!firstStep) {
					
					sb.append(",");
				}
				sb.append(t.createTransportInfo());
				
				firstStep = false;
			}
			
			return sb.toString();
			
		}
		else {
			
			RTSPTransport transport = getRTSPTransport(trackId);
			
			if(transport == null)
				
				return "";
			else
				return transport.createTransportInfo();
		}
	}
	
	public boolean play(int trackId) {
		
		if(trackId < 0 ) {
			
			boolean ok = false;
			
			for(Iterator<Entry<Integer,RTSPTransport>> it = transports.entrySet().iterator()
					;it.hasNext();) {
				
				RTSPTransport t = it.next().getValue();
				
				ok = true;
				
				t.getTransportHandler().play();
			}
			
			return ok;
			
		}
		else {
			
			RTSPTransport transport = getRTSPTransport(trackId);
			
			if(transport != null) {
				
				transport.getTransportHandler().play();	
				
				return true;
			}
			else {
				
				return false;
			}
		}
	}
	
	public boolean stop(int trackId) {
		
		if(trackId < 0 ) {
			
			for(Iterator<Entry<Integer,RTSPTransport>> it = transports.entrySet().iterator()
					;it.hasNext();) {
				
				RTSPTransport t = it.next().getValue();
				
				t.getTransportHandler().stop();
				
				it.remove();
			}
			
			return true;
		}
		else {
			
			RTSPTransport transport = getRTSPTransport(trackId);
			
			if(transport != null) {
				
				transport.getTransportHandler().stop();	
				
				transports.remove(trackId);
			}
			
			return false;
		}
	}
}
