package com.github.madzdns.cluster.core.backend.node;

import com.github.madzdns.cluster.http.HttpServer;

import java.io.IOException;

import com.github.madzdns.cluster.core.backend.node.statics.BackendServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.IServer;
import com.github.madzdns.cluster.core.config.Bind;

public class BackendService implements IServer {

	private Bind bind = null;
	private Logger log;
	public BackendService(Bind bind) {
		
		log = LoggerFactory.getLogger(HttpServer.class);
		
		this.bind = bind;
	}
	
	@Override
	public void start() throws IOException {
		
		new com.github.madzdns.cluster.core.backend.node.dynamic.BackendServer(bind).start();
		
		new BackendServer(bind).start();
		
		log.info("Backend service is up");
	}

}
