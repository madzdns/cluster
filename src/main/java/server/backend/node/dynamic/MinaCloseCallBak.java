package server.backend.node.dynamic;

import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.service.Monitor;

public class MinaCloseCallBak implements IoFutureListener<CloseFuture> {
	
	private Logger log = LoggerFactory.getLogger(MinaCloseCallBak.class);

	String node = null;
	
	public MinaCloseCallBak(String server) {
		
		node = server;
	}
	
	@Override
	public void operationComplete(CloseFuture close) {
		
		if(close.getSession()==null) {
			
			log.error("Session is null for edge {}'s close future. Will clean up without checking id");
			
			Monitor.catchStopedNode(node,0);
			
			return;
		}

		Monitor.catchStopedNode(node,close.getSession().getId());
		
		close.setClosed();
	}

}
