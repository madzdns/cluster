package com.github.madzdns.server.core.api.net.ssl.server.filter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import com.github.madzdns.server.core.api.net.NetProvider;
import com.github.madzdns.server.core.config.Config;
import com.github.madzdns.server.core.config.MyEntry;
import com.github.madzdns.server.core.config.SSLCredentials;
import com.github.madzdns.server.core.utils.DomainHelper;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.api.LookupResult;
import com.github.madzdns.server.core.api.LookupService;
import com.github.madzdns.server.core.api.ProtoInfo;
import com.github.madzdns.server.core.api.ResolveResult;
import com.github.madzdns.server.core.api.Resolver;

public class MinaSslFilter extends SslFilter {

    private Logger log = LoggerFactory.getLogger(getClass());

    public static final String NAME = MinaSslFilter.class.getName() + "ssl_filter";

    public static final String DETECT_SNI = MinaSslFilter.class.getName() + "DETECT_SNI";

    static final String HANDSHAKE_PARSED = MinaSslFilter.class.getName() + "#$Handed3435";

    public final static String FOUND_SERVER = MinaSslFilter.class.getName() + "#$found3erver";

    public final static String HAS_SSL = MinaSslFilter.class.getName() + "HAS_SSL";

    public final static String SSL_CTX_KEY = MinaSslFilter.class.getName() + "SSL_CTX_KEY";

    /**
     * Applications should check this one to see if any core
     * has chosen in SSL layer
     */
    public final static String RESOLVED_RESULT = "#$Thi3IIsrslEerver";

    private final static String SERVER_CHECKED = "#$3erverch3cked";

    public final static String LOOKUP_RESULT_REDIRECT = MinaSslFilter.class.getName() + "LOOKUP_RESULT_REDIRECT";

    private final static String PROTO_INFO = MinaSslFilter.class.getName() + "PROTO_INFO";

    public MinaSslFilter(SSLContext sslContext, IoSession session, ProtoInfo protoInfo) {

        super(sslContext);

        session.setAttribute(PROTO_INFO, protoInfo);
    }

    public MinaSslFilter(SSLContext sslContext, IoSession session, ProtoInfo protoInfo, boolean auto_start) {

        super(sslContext, auto_start);

        session.setAttribute(PROTO_INFO, protoInfo);
    }

    public MinaSslFilter(SSLContext sslContext, IoSession session, ProtoInfo protoInfo, boolean auto_start, boolean detectSNI) {

        this(sslContext, session, protoInfo, auto_start);

        session.setAttribute(DETECT_SNI, detectSNI);
        session.setAttribute(PROTO_INFO, protoInfo);

        session.setAttribute(SSL_CTX_KEY, sslContext);
    }

    @Override
    public void messageReceived(NextFilter next, IoSession session, Object message)
            throws SSLException {

        if (session.containsAttribute(IS_NOT_SSL)) {

            next.messageReceived(session, message);

            return;
        }

        boolean wrongMSG = false;

        try {

            if (!checkServerForSsl(message, session)) {

                wrongMSG = true;
            }

            super.messageReceived(next, session, message);

            if (wrongMSG) {

                throw new SSLException("NOT PROPER SNI EXTENSION FOUND");
            }

            session.setAttribute(HAS_SSL, Boolean.TRUE);

        } catch (Exception e) {

            if (wrongMSG && !session.containsAttribute(IS_NOT_SSL)) {

                //errornous SSL
                throw e;
            }

            if (message instanceof IoBuffer) {

                String clientip = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

                ProtoInfo protoInfo = (ProtoInfo) session.getAttribute(PROTO_INFO);

                ((IoBuffer) message).flip();
                log.warn("Handshake Error, REQUEST SOURCE <{}>, PROTO NAME <{}> - continue as non ssl message",
                        clientip, protoInfo.getServiceName());

                next.messageReceived(session, message);
            }
        }
    }

    @Override
    public void filterWrite(NextFilter next, IoSession session, WriteRequest write)
            throws SSLException {

        if (session.containsAttribute(IS_NOT_SSL)) {

            next.filterWrite(session, write);

            return;
        }

        super.filterWrite(next, session, write);
    }

    @Override
    public void sessionClosed(NextFilter nextFilter, IoSession session)
            throws SSLException {


        super.sessionClosed(nextFilter, session);
    }

    private List<SNIServerName> tryDetectSNI(Object message, IoSession session) {

        IoBuffer inNetBuffer = (IoBuffer) message;

        SSLCapabilities capabilities = null;

        int position = inNetBuffer.position();
        int limit = inNetBuffer.limit();

        try {

            capabilities = SSLExplorer.explore(inNetBuffer.buf());

        } catch (Exception e) {

            log.error("{}", e.getMessage());

            inNetBuffer.position(position);
            inNetBuffer.limit(limit);

            return null;
        }

        inNetBuffer.position(position);
        inNetBuffer.limit(limit);

        if (capabilities != null) {

            session.setAttribute(HANDSHAKE_PARSED, Boolean.TRUE);

            List<SNIServerName> serverNames = capabilities.getServerNames();

            log.debug("Record version: {}, Hello version: {}, SNI {}", capabilities.getRecordVersion()
                    , capabilities.getHelloVersion()
                    , serverNames);

            if (serverNames != null && serverNames.size() > 0) {

                return serverNames;
            }
        }

        return null;
    }

    private boolean checkServerForSsl(Object message, IoSession session) {


        Boolean detectSNI = (Boolean) session.getAttribute(DETECT_SNI);

        if (detectSNI == null || !detectSNI) {

            return true;
        }

        if (session.getAttribute(FOUND_SERVER) != null) {

            return true;
        }

        if (session.getAttribute(SERVER_CHECKED) == null) {

            List<SNIServerName> sniNames = tryDetectSNI(message, session);

            session.setAttribute(SERVER_CHECKED, Boolean.TRUE);

            if (sniNames == null) {

                return false;
            }

            for (Iterator<SNIServerName> sniIt = sniNames.iterator(); sniIt.hasNext(); ) {

                SNIServerName sniName = sniIt.next();

                if (sniName instanceof SNIHostName) {

                    String apikey = null;
                    String host = ((SNIHostName) sniName).getAsciiName();

                    short fwdportnum = 0;
                    String fwdProto = null;

                    DomainHelper helper = new DomainHelper(host);

                    if (helper.IsFullDomain()) {

                        apikey = helper.GetApiKey();
                        host = helper.GetPureDomain();

                        String fwdPP = helper.GetFwdPort();

                        fwdProto = helper.GetFwdProto();

                        if (fwdPP != null) {

                            try {

                                fwdportnum = Short.parseShort(fwdPP);
                            } catch (Exception e) {
                            }
                        }
                    }

                    ResolveResult cfg = null;

                    if (apikey != null) {

                        //I beleive we don't use apikey right know so I ignored setting up ssl here

                        try {

                            ProtoInfo protoInfo = (ProtoInfo) session.getAttribute(PROTO_INFO);

                            if (fwdportnum == 0) {

                                fwdportnum = protoInfo.getDefaultPort();
                            }

                            String clientip = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

                            cfg = Resolver.getBestServer(clientip, apikey, protoInfo.getSrvProto(), fwdportnum, true);

                            cfg.setFwdPort(fwdportnum);
                            cfg.setFwdProto(fwdProto);

                        } catch (Exception e) {

                            return false;
                        }
                    } else {

                        try {

                            LookupResult result = LookupService.dnsConfigLookup(host);

                            if (result == null) {

                                //throw new IOException("Could not find proper CNAME");
                                continue;

                                //result = new LookupResult("www.frfra.biz.", "frfra-h.www.0canq.cl.frfra.com.");
                                //result = new LookupResult("frfra-p8443.www.0canq.cl.frfra.com.", "frfra-p8443.www.0canq.cl.frfra.com.");
                            }

                            MyEntry entry = Config.getMyEntryWithFullAname(result.getFqdnName());

                            if (entry == null) {

                                throw new IOException(String.format("Could not find entry config for %s", result.getFqdnName()));
                            }

                            SSLCredentials credentials = getHostCredential(entry.getCredentialsMap(), host);

                            if (credentials == null) {

                                throw new IOException(String.format("Could not find ssl credentials for %s", host));
                            }

                            SSLContext sslContext = (SSLContext) session.getAttribute(SSL_CTX_KEY);

                            if (!NetProvider.configSSLContext(sslContext, credentials.getCertificate(),
                                    credentials.getKey())) {

                                throw new IOException(String.format("Certificate initialization error for %s", host));
                            }

                            ProtoInfo protoInfo = (ProtoInfo) session.getAttribute(PROTO_INFO);

                            if (result.isRedirectResult() && (!result.isHttpOnly() || ProtoInfo.HTTP_SRV.
                                    equals(protoInfo.getServiceName()))) {

                                String redirectTo = result.getRedirectName();
                                cfg = new ResolveResult(null, redirectTo, false);

                                cfg.setFwdPort(protoInfo.getDefaultPort());

                                session.setAttribute(LOOKUP_RESULT_REDIRECT);
                            } else {

                                if (result.getFullApiResult() != null) {

                                    fwdportnum = result.getFullApiResult().getForwardPort();
                                    fwdProto = result.getFullApiResult().getForwardProto();
                                }

                                if (fwdportnum == 0) {

                                    fwdportnum = protoInfo.getDefaultPort();
                                }

                                String clientip = ((InetSocketAddress) session.getRemoteAddress()).getAddress().getHostAddress();

                                cfg = Resolver.getBestServer(clientip, result.getFqdnName(), protoInfo.getSrvProto(), fwdportnum, true);

                                cfg.setFwdPort(fwdportnum);
                                cfg.setFwdProto(fwdProto);
                            }

                        } catch (Exception e) {

                            log.error("", e);
                            return false;
                        }
                    }

                    session.setAttribute(RESOLVED_RESULT, cfg);

                    session.setAttribute(FOUND_SERVER, cfg.getServer());

                    return true;
                }
            }
        }

        return false;
    }

    private SSLCredentials getHostCredential(Map<String, SSLCredentials> credentialsMap, String host) {

        if (credentialsMap == null) {

            return null;
        }

        SSLCredentials credentials = credentialsMap.get(host);

        if (credentials != null) {

            return credentials;
        }

        String[] labels = host.split("\\.");

        int length = Math.min(labels.length, 5);

        String lablePath = "";
        String lable = null;

        for (int i = length - 1; i > 0; i--) {

            lable = labels[i];

            lablePath = new StringBuilder(".").append(lable).
                    append(lablePath).toString();

            log.info("checking {}", new StringBuilder("*").append(lablePath).toString());
            credentials = credentialsMap.get(new StringBuilder("*").append(lablePath).toString());

            if (credentials != null) {

                return credentials;
            }
        }

        return null;
    }

}
