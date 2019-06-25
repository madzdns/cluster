package com.github.madzdns.server.core.api.net.ssl.server;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.github.madzdns.server.core.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.cert.Certificate;
import java.security.PrivateKey;
import java.security.KeyStore;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class JavaSSLContext {

    @SuppressWarnings("unused")
    private static Logger log = LoggerFactory.getLogger(JavaSSLContext.class);

    public static SSLContext getSSLcontextFactory() throws Exception {
        final String KEYSTORE_PASSWORD = Config.getApp().getSslConfig().getKeyStorePass();
        final String TRUSTSTORE_PASSWORD = Config.getApp().getSslConfig().getTrustStorePass();
        final String KEYSTORE_PASSWORD_2ND = Config.getApp().getSslConfig().getKeyStorePass2();
        final String keyStoreFileName = Config.getApp().getSslConfig().getKeyStore();
        final String trustStoreFileName = Config.getApp().getSslConfig().getTrustStore();
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

            // First initialize the key and trust material
            KeyStore ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(new FileInputStream(keyStoreFile), KEYSTORE_PASSWORD.toCharArray());
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(new FileInputStream(trustStoreFile), TRUSTSTORE_PASSWORD.toCharArray());

            // KeyManagers decide which key material to use
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(ksKeys, KEYSTORE_PASSWORD_2ND.toCharArray());

            // TrustManagers decide whether to allow connections
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ksTrust);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            return sslContext;
        }

        return null;
    }

    public static void configSSLContext(SSLContext context, byte[][] pemCertificateChain, byte[] pemKey) throws Exception {
        final String KEYSTORE_PASSWORD = Config.getApp().getSslConfig().getKeyStorePass();
        Certificate[] certs = new Certificate[pemCertificateChain.length];

        for (int i = 0; i < pemCertificateChain.length; i++) {

            byte[] certBytes = org.apache.commons.codec.binary.
                    Base64.decodeBase64(pemCertificateChain[i]);

            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").
                    generateCertificate(new ByteArrayInputStream(certBytes));

            certs[i] = cert;
        }


        byte[] keyBytes = org.apache.commons.codec.binary.
                Base64.decodeBase64(pemKey);

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        PrivateKey key = (RSAPrivateKey) factory.generatePrivate(spec);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        keystore.setCertificateEntry("cert-alias", certs[0]);
        keystore.setKeyEntry("key-alias", key, KEYSTORE_PASSWORD.toCharArray(), certs);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, KEYSTORE_PASSWORD.toCharArray());

        context.init(kmf.getKeyManagers(), null, null);
    }

    public static SSLContext getServerTLSContext(byte[] pemCertificateChain, byte[] pemKey) throws Exception {
        final String KEYSTORE_PASSWORD = Config.getApp().getSslConfig().getKeyStorePass();
        byte[] certBytes = org.apache.commons.codec.binary.
                Base64.decodeBase64(pemCertificateChain);
        byte[] keyBytes = org.apache.commons.codec.binary.
                Base64.decodeBase64(pemKey);

        X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").
                generateCertificate(new ByteArrayInputStream(certBytes));

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory factory = KeyFactory.getInstance("RSA");

        PrivateKey key = (RSAPrivateKey) factory.generatePrivate(spec);

        KeyStore keystore = KeyStore.getInstance("JKS");
        keystore.load(null, null);
        keystore.setCertificateEntry("cert-alias", cert);
        keystore.setKeyEntry("key-alias", key, KEYSTORE_PASSWORD.toCharArray(), new Certificate[]{cert});

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(keystore, KEYSTORE_PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), null, null);

        return sslContext;
    }
}
