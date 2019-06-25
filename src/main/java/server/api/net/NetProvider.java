package server.api.net;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.api.net.ssl.client.MinaClientSslContext;
import server.api.net.ssl.server.JavaSSLContext;
import server.api.net.ssl.server.MinaServerSslContext;
import sun.security.provider.X509Factory;

public class NetProvider {

    private static Logger log = LoggerFactory.getLogger(NetProvider.class);

    private static SslContextFactory SSL_CTX = null;

    private static SSLContext TLS_CTX = null;

    public static SSLContext getServerSSLContext() {

        try {

            if (SSL_CTX != null)

                return SSL_CTX.newInstance();

            return (SSL_CTX = new MinaServerSslContext().getSslContextFactory()).newInstance();

        } catch (Exception e) {

            log.error("", e);

            SSL_CTX = null;
            return null;
        }
    }

    public static SSLContext getServerTLSContext(boolean configure) {

        try {

            if (!configure) {
                SSLContext ctx = SSLContext.getInstance("TLS");
                ctx.init(null, null, null);
            }

            if (TLS_CTX != null) {
                return TLS_CTX;
            }

            return TLS_CTX = JavaSSLContext.getSSLcontextFactory();
        } catch (Exception e) {
            log.error("", e);
            return TLS_CTX = null;
        }
    }

    public static SSLContext getServerTLSContext(String pemCertificateChain, String pemKey) {

        if (pemCertificateChain == null || pemKey == null) {
            return null;
        }

        try {

            return JavaSSLContext.getServerTLSContext(pemCertificateChain.getBytes(), pemKey.getBytes());

        } catch (Exception e) {

            log.error("", e);

            return null;
        }
    }

    public static boolean configSSLContext(SSLContext sslContext, String[] pemCertificateChain, String pemKey) {

        if (pemCertificateChain == null || pemKey == null) {

            return false;
        }

        try {

            byte[][] certificates = new byte[pemCertificateChain.length][];

            for (int i = 0; i < pemCertificateChain.length; i++) {

                certificates[i] = pemCertificateChain[i].getBytes();
            }

            JavaSSLContext.configSSLContext(sslContext, certificates, pemKey.getBytes());

            return true;

        } catch (Exception e) {

            log.error("", e);
            return false;
        }
    }

    public static boolean configSSLContext(SSLContext sslContext, String pemCertificateChain, String pemKey) {

        if (pemCertificateChain == null || pemKey == null) {

            return false;
        }

        try {

            ArrayList<String> certificateChain = new ArrayList<>();

            Pattern p = Pattern.compile(X509Factory.BEGIN_CERT + "(.*?)" + X509Factory.END_CERT, Pattern.DOTALL);
            Matcher m = p.matcher(pemCertificateChain);

            while (m.find()) {

                certificateChain.add(m.group(1));
            }

            byte[][] certificates = new byte[certificateChain.size()][];

            for (int i = 0; i < certificateChain.size(); i++) {

                certificates[i] = certificateChain.get(i).getBytes();
            }

            JavaSSLContext.configSSLContext(sslContext, certificates, pemKey.getBytes());

            return true;

        } catch (Exception e) {

            log.error("", e);
            return false;
        }
    }

    public static SSLContext getClientSslContext() {

        try {

            return new MinaClientSslContext().getSslClientContextFactory();

        } catch (GeneralSecurityException e) {

            log.error("", e);
            return null;
        }
    }

    public static SSLContext getClientSslContext(String certFilePath) {

        try {

            return new MinaClientSslContext().getSslClientContextFactory(certFilePath);

        } catch (GeneralSecurityException e) {

            log.error("", e);
            return null;

        } catch (IOException e) {

            log.error("", e);
            return null;
        }
    }

    public static void closeMinaSession(IoSession session, boolean immidiate) {

        if (session != null) {
            /*if (session.isConnected()) {
                // Wait until the chat ends.
                session.getCloseFuture().awaitUninterruptibly();
            }*/

            if (immidiate) {

                session.closeNow();
                return;
            }

            session.closeOnFlush();
        }
    }
}
