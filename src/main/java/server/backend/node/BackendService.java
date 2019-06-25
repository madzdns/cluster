package server.backend.node;

import http.HttpServer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.IServer;
import server.config.Bind;

public class BackendService implements IServer {

	private Bind bind = null;
	private Logger log;
	public BackendService(Bind bind) {
		
		log = LoggerFactory.getLogger(HttpServer.class);
		
		this.bind = bind;
	}
	
	@Override
	public void start() throws IOException {
		
		new server.backend.node.dynamic.BackendServer(bind).start();
		
		new server.backend.node.statics.BackendServer(bind).start();
		
		log.info("Backend service is up");
	}

}
