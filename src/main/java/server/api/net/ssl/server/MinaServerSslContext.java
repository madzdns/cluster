package server.api.net.ssl.server;

import java.io.File;
import java.security.KeyStore;

import org.apache.mina.filter.ssl.KeyStoreFactory;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.config.Config;

import static server.api.net.ssl.server.JavaSSLContext.*;

public class MinaServerSslContext {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	public SslContextFactory getSslContextFactory() {
		final String KEYSTORE_PASSWORD = Config.getApp().getSslConfig().getKeyStorePass();
		final String TRUSTSTORE_PASSWORD = Config.getApp().getSslConfig().getTrustStorePass();
		final String KEYSTORE_PASSWORD_2ND = Config.getApp().getSslConfig().getKeyStorePass2();
		final String keyStoreFileName = Config.getApp().getSslConfig().getKeyStore();
		final String trustStoreFileName = Config.getApp().getSslConfig().getTrustStore();
		SslContextFactory sslContextFact = null;
		try {
			
			String keystore = new StringBuilder()
			.append(Config.KEY_STORES)
			.append(keyStoreFileName)
			.toString();
			
			String truststore = new StringBuilder()
			.append(Config.KEY_STORES)
			.append(trustStoreFileName)
			.toString();
			
			File keyStoreFile = new File(keystore);
			File trustStoreFile = new File(truststore);
 
			if (keyStoreFile.exists() && trustStoreFile.exists()) {
				
				final KeyStoreFactory keyStoreFactory = new KeyStoreFactory();
				keyStoreFactory.setDataFile(keyStoreFile);
				keyStoreFactory.setPassword(KEYSTORE_PASSWORD);

				final KeyStoreFactory trustStoreFactory = new KeyStoreFactory();
				trustStoreFactory.setDataFile(trustStoreFile);
				trustStoreFactory.setPassword(TRUSTSTORE_PASSWORD);
				
				final SslContextFactory sslContextFactory = new SslContextFactory();
				final KeyStore keyStore = keyStoreFactory.newInstance();
				sslContextFactory.setKeyManagerFactoryKeyStore(keyStore);
				
				final KeyStore trustStore = keyStoreFactory.newInstance();
				sslContextFactory.setTrustManagerFactoryKeyStore(trustStore);
				sslContextFactory.setKeyManagerFactoryKeyStorePassword(KEYSTORE_PASSWORD_2ND);
				sslContextFact = sslContextFactory;
			}
			else {
				
				log.error("Keystore or Truststore file does not exist");
			}
		}
		catch (Exception ex) {
			
			log.error("",ex);
		}
		return sslContextFact;
	}
}
