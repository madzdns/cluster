package server.backend.frsynch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.frfra.frsynch.ClusterSnapshot;
import com.frfra.frsynch.SynchContext;
import com.frfra.frsynch.SynchHandler;
import com.frfra.frsynch.config.SynchConfig;

import server.api.net.ssl.server.JavaSSLContext;
import server.config.Bind;
import server.config.Config;
import server.config.Socket;

public class SynchServer implements server.IServer{
	
	private Bind synchBindings,
	backendBindings;
	
	private Logger log;
	public SynchServer(Bind synchBindings, Bind backendBindings) {
		
		log = LoggerFactory.getLogger(SynchServer.class);
		this.synchBindings = synchBindings;
		this.backendBindings = backendBindings;
	}

	@Override
	public void start() throws IOException {
		final String KEYSTORE_PASSWORD = Config.getApp().getSslConfig().getKeyStorePass();
		final String TRUSTSTORE_PASSWORD = Config.getApp().getSslConfig().getTrustStorePass();
		final String KEYSTORE_PASSWORD_2ND = Config.getApp().getSslConfig().getKeyStorePass2();
		final String keyStoreFileName = Config.getApp().getSslConfig().getKeyStore();
		final String trustStoreFileName = Config.getApp().getSslConfig().getTrustStore();
		final String CERTIFICATE_PATH = Config.getApp().getSslConfig().getCertificate();
		String keystore = new StringBuilder()
		.append(Config.KEY_STORES)
		.append(keyStoreFileName)
		.toString();
		
		String truststore = new StringBuilder()
		.append(Config.KEY_STORES)
		.append(trustStoreFileName)
		.toString();
		
		SynchConfig config = new SynchConfig(Config.APPHOME+File.separator+Config.CLUSTER_FILE,
				keystore, truststore,
				KEYSTORE_PASSWORD, TRUSTSTORE_PASSWORD, KEYSTORE_PASSWORD_2ND,
				CERTIFICATE_PATH);
		
		SynchContext context = new SynchContext(Config.getApp().getId(), config);
		
		Frsynch.synchContext = context;
		
		SynchHandler handler = context.make()
				.withCallBack(new ZoneSynchCallback())
				.withEncoder(ZoneSynchMessage.class);
		
		com.frfra.frsynch.config.Bind sBindings = new com.frfra.frsynch.config.Bind();
		com.frfra.frsynch.config.Bind bBindings = new com.frfra.frsynch.config.Bind();
		
		List<com.frfra.frsynch.config.Socket> sockets = new ArrayList<>();
		
		for(Socket b: synchBindings.getSockets()) {
			
			com.frfra.frsynch.config.Socket s = new com.frfra.frsynch.config.Socket();
			
			s.setValue(b.getValue());
			sockets.add(s);
		}
		
		sBindings.setSockets(sockets);
		
		sockets = new ArrayList<>();
		
		for(Socket b: backendBindings.getSockets()) {
			
			com.frfra.frsynch.config.Socket s = new com.frfra.frsynch.config.Socket();
			
			s.setValue(b.getValue());
			sockets.add(s);
		}
		
		bBindings.setSockets(sockets);
		
		new com.frfra.frsynch.SynchServer(handler, sBindings, bBindings).start();
		
		ClusterSnapshot cs = context.getSnapshot();
			
		log.info("Known cluster nodes {}", cs.getCluster());
		
		new StartupManager().startSynchingResourcesWithCluster(context, Config.getApp().getId());
		
		log.info("SYNCH is listning on {} ", sBindings.getSockets());
	}
}
