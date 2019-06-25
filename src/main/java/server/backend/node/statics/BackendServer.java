package server.backend.node.statics;

import http.HttpServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.IServer;
import server.config.Bind;
import server.config.Socket;
import server.wsi.WebService;

public class BackendServer implements IServer{

	private Bind bind = null;
	private Logger log;
	public BackendServer(Bind bind) {
		
		log = LoggerFactory.getLogger(HttpServer.class);
		
		List<Socket> socketsForStaticBackends = new ArrayList<Socket>();
		
		this.bind = new Bind();
		
		for(Socket s:bind.getSockets()) {
			
			if((s.getPort()%10) != 0x8) {
				
				continue;
			}
			
			socketsForStaticBackends.add(s);
		}
		
		this.bind.setSockets(socketsForStaticBackends);
	}
	
	@Override
	public void start() throws IOException {

		WebService service = new WebService(bind);
		service.addController(RequestController.class);
		
		service.start();
		
		log.info("Static backend service started");
	}
}
