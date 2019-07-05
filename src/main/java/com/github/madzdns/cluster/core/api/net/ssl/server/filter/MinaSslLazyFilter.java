package com.github.madzdns.cluster.core.api.net.ssl.server.filter;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import com.github.madzdns.cluster.core.api.net.NetProvider;
import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.core.config.MyEntry;
import com.github.madzdns.cluster.core.config.SSLCredentials;
import com.github.madzdns.cluster.core.utils.DomainHelper;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.filter.ssl.SslFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.api.LookupResult;
import com.github.madzdns.cluster.core.api.LookupService;
import com.github.madzdns.cluster.core.api.ProtoInfo;
import com.github.madzdns.cluster.core.api.ResolveResult;
import com.github.madzdns.cluster.core.api.Resolver;

public class MinaSslLazyFilter extends IoFilterAdapter {

private Logger log = LoggerFactory.getLogger(getClass());
	
	public static final String NAME = MinaSslLazyFilter.class.getName()+"ssl_filter";
	
	static final String HANDSHAKE_PARSED = MinaSslLazyFilter.class.getName()+"#$Handed3435";
	
	public final static String FOUND_SERVER = MinaSslFilter.class.getName()+"#$found3erver";
	
	public final static String HAS_SSL = MinaSslLazyFilter.class.getName()+"HAS_SSL";
	
	/**
	 * Applications should check this one to see if any core
	 * has chosen in SSL layer
	 */
	public final static String RESOLVED_RESULT = "#$Thi3IIsrslEerver";
	
	private final static String SERVER_CHECKED = "#$3erverch3cked";
	
	public final static String LOOKUP_RESULT_REDIRECT = MinaSslLazyFilter.class.getName()+"LOOKUP_RESULT_REDIRECT";
	
	private final static String PROTO_INFO = MinaSslLazyFilter.class.getName()+"PROTO_INFO";
	
	public MinaSslLazyFilter(IoSession session, ProtoInfo protoInfo) {

		session.setAttribute(PROTO_INFO, protoInfo);
	}
	
	@Override
	public void messageReceived(NextFilter next, IoSession session, Object message)
			throws SSLException {
		
		if(session.containsAttribute(SslFilter.IS_NOT_SSL)) {
			
			next.messageReceived(session, message);

			return;
		}
		
		try {

			if(!checkServerForSsl(next, message, session)) {

				throw new SSLException("NOT PROPER SNI EXTENSION FOUND");
			}
			
			session.setAttribute(HAS_SSL, Boolean.TRUE);
			
		} catch(Exception e) {
			
		}
		
		next.messageReceived(session, message);
	}
	
	@Override
	public void filterWrite(NextFilter next, IoSession session, WriteRequest write)
			throws SSLException {
		
		next.filterWrite(session, write);
	}
	
	@Override
	public void sessionClosed(NextFilter nextFilter, IoSession session)
			throws SSLException {
		
		nextFilter.sessionClosed(session);
	}
	
	private List<SNIServerName> tryDetectSNI(Object message, IoSession session) {

		IoBuffer inNetBuffer = (IoBuffer) message;
		
		SSLCapabilities capabilities = null;
		
        int position = inNetBuffer.position();
        int limit = inNetBuffer.limit();

        try {

			capabilities = SSLExplorer.explore(inNetBuffer.buf());
			
		} catch (Exception e) {
			log.error("", e);
			return null;
		}
        
        inNetBuffer.position(position);
        inNetBuffer.limit(limit);
        
        if (capabilities != null) {
        	
        	session.setAttribute(HANDSHAKE_PARSED, Boolean.TRUE);
        	
        	List<SNIServerName> serverNames = capabilities.getServerNames();
        	
        	log.debug("Record version: {}, Hello version: {}, SNI {}",capabilities.getRecordVersion()
        			, capabilities.getHelloVersion()
        			,serverNames);
        	
        	if(serverNames != null && serverNames.size()>0) {

        		return serverNames;
        	}
        }
        
        return null;
	}
	
	private boolean checkServerForSsl(NextFilter next, Object message, IoSession session) {
		
		if(session.getAttribute(FOUND_SERVER) != null) {
			
			return true;
		}
		
		if(session.getAttribute(SERVER_CHECKED) == null) {
			
			List<SNIServerName> sniNames = tryDetectSNI(message, session);
			
			session.setAttribute(SERVER_CHECKED, Boolean.TRUE);
			
			if(sniNames == null) {
				
				return false;
			}
			
			for(Iterator<SNIServerName> sniIt = sniNames.iterator(); sniIt.hasNext();) {
				
				SNIServerName sniName = sniIt.next();
				
				if(sniName instanceof SNIHostName) {
					
					String apikey = null;
					String host = ((SNIHostName) sniName).getAsciiName();
					
					short fwdportnum = 0;
					String fwdProto = null;
					
					DomainHelper helper = new DomainHelper(host);
					
					if(helper.IsFullDomain()) {
						
						apikey = helper.GetApiKey();
						host = helper.GetPureDomain();
						
						String fwdPP = helper.GetFwdPort();
						
						fwdProto = helper.GetFwdProto();
						
						if(fwdPP != null) {
						
							try {
							
								fwdportnum = Short.parseShort(fwdPP);
							}
							catch(Exception e) {}
						}
					}
					
					ResolveResult cfg = null;
					
					if(apikey != null) {
						
						try {
							
							ProtoInfo protoInfo = (ProtoInfo) session.getAttribute(PROTO_INFO);
							
							if(fwdportnum == 0) {
								
								fwdportnum = protoInfo.getDefaultPort();
							}
	
							String clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
							
							cfg = Resolver.getBestServer(clientip, apikey, protoInfo.getSrvProto(), fwdportnum, true);
							
							cfg.setFwdPort(fwdportnum);
							cfg.setFwdProto(fwdProto);
							
						} catch (Exception e) {
							
							return false;
						}
					}
					else {
						
						try {

							LookupResult result = LookupService.dnsConfigLookup(host);
							
							if(result == null) {
								
								//throw new IOException("Could not find proper CNAME");
								continue;
							}
							
							MyEntry entry = Config.getMyEntryWithFullAname(result.getFqdnName());

							if(entry == null) {
								
								throw new IOException(String.format("Could not find entry config for %s", result.getFqdnName()));
							}

							Map<String, SSLCredentials> credentialsMap = entry.getCredentialsMap();
							
							SSLCredentials credentials = credentialsMap == null? null:credentialsMap.get(host); 
							
							if(credentials == null) {
								
								throw new IOException(String.format("Could not find ssl credentials for %s", host));
							}
							
							SSLContext sslContext = NetProvider.getServerTLSContext(credentials.getCertificate(),
									credentials.getKey());
							
							if(sslContext == null) {
								
								throw new IOException(String.format("Certificate initialization error for %s", host));
							}
							
							ProtoInfo protoInfo = (ProtoInfo) session.getAttribute(PROTO_INFO);
							
							SslFilter sslFilter = new MinaSslFilter(sslContext, session, protoInfo, false, false);
							
							session.getFilterChain().addFirst(MinaSslLazyFilter.NAME, sslFilter);
							
							sslFilter.messageReceived(next, session, message);
							
							if(result.isRedirectResult() && (!result.isHttpOnly() || ProtoInfo.HTTP_SRV.equals(protoInfo.getServiceName()))) {
								
								String redirectTo = result.getRedirectName();
								cfg = new ResolveResult(null, redirectTo, false);
								
								cfg.setFwdPort(protoInfo.getDefaultPort());
								
								session.setAttribute(LOOKUP_RESULT_REDIRECT);
							}
							else {

								if(result.getFullApiResult() != null) {
									
									fwdportnum = result.getFullApiResult().getForwardPort();
									fwdProto = result.getFullApiResult().getForwardProto();
								}
								
								if(fwdportnum == 0) {
									
									fwdportnum = protoInfo.getDefaultPort();
								}
								
								String clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
								
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
					
					session.setAttribute(FOUND_SERVER,cfg.getServer());
					
					return true;	
				}	
			}
		}
		
		return false;
	}
}
