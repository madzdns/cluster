package com.github.madzdns.cluster.http.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import com.github.madzdns.cluster.http.HTTPResponse;
import com.github.madzdns.cluster.http.HTTPCode;
import com.github.madzdns.cluster.http.HTTPMessage;
import com.github.madzdns.cluster.http.HTTPMessage.Method;
import com.github.madzdns.cluster.http.HTTPRequest;
import com.github.madzdns.cluster.http.codec.HttpMinaDecoder;
import com.github.madzdns.cluster.http.codec.HttpMinaEncoder;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.rtmpt.RTMPTConnectionMinaFilter;
import com.github.madzdns.cluster.core.api.LookupResult;
import com.github.madzdns.cluster.core.api.LookupService;
import com.github.madzdns.cluster.core.api.ProtoInfo;
import com.github.madzdns.cluster.core.api.ResolveResult;
import com.github.madzdns.cluster.core.api.Resolver;
import com.github.madzdns.cluster.core.api.net.NetProvider;
import com.github.madzdns.cluster.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.cluster.core.backend.node.dynamic.ResolveHintType;
import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.cluster.core.utils.DomainHelper;
import com.github.madzdns.cluster.core.utils.QueryStringHelper;
import com.github.madzdns.cluster.core.utils.QueryStringHelper.ParsedQuery;
import com.github.madzdns.cluster.core.codec.ServerCodecFactory;
import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.core.config.ResolveHint;
import com.github.madzdns.cluster.core.core.http.HttpProxy;
import com.github.madzdns.cluster.core.core.http.ProxyResponse;

public class HttpHandler extends IoHandlerAdapter {
	
	
	/*
	 * 
	 * private final static String SOAP_CONTENT_TYPE = "application/soap";
	private final static String FORM_CONTENT_TYPE = "application/core-www-form-urlencoded";
	private final static String JSON_ACCEPT = "application/json";
	 */

	private final static String CROSS_DOMAIN = "/crossdomain.xml";
	private final static String HLS_EXTENTION = ".m3u8";
	private final static String HDS_EXTENTION = ".f4m";
	private final static String APPLICATION_CONTENT_TYPE = "application";
	private final static String XML_CONTENT_TYPE = "text/xml";
	private final static String JSON_APP_ACCEPT = "application/json";
	
	
	private final static Pattern HasExtention =  Pattern.compile(".*/.*\\.[^/]*", Pattern.CASE_INSENSITIVE);
	private final static Pattern DynamiExtention =  Pattern.compile(".*\\.(php|php3|php|phtml|asp|aspx|jsp|cgi)$", Pattern.CASE_INSENSITIVE);
	private final static Pattern StaticAccept =  Pattern.compile(".*(css|image).*", Pattern.CASE_INSENSITIVE);
			
	private final static String DEFULT_PROTO = "com/github/madzdns/x/http";
	
	private final Logger log = LoggerFactory.getLogger(HttpHandler.class);
	
	private final static String SINGLE_DOMAIN_IFRAME = "<!DOCTYPE html /><html><head><style>html, body {width:100%%;height:100%%;border:0px;padding:0px;margin:0px;}iframe{width:100%%;height:100%%;border:0px;padding:0px;margin:0px;}</style></head><body><iframe src=\"%s\" >YOUR BROWSER IS NOT SUPPORTING IFRAMES. PLEASE SEE CONTENTS DIRECTLY AT <a href=\"%s\" />%s</a></iframe></body></html>";
	
	private static String steakyCookieId = null;
	
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		
		if(Config.getApp().isServSSLEnabled()) {

			SSLContext ssl = NetProvider.getServerTLSContext(false);
			
			if(ssl != null) {
				
				//we use incomming port as the default port number
				final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
				
				MinaSslFilter sslFilter = new MinaSslFilter(ssl, session, new ProtoInfo(ServiceInfo.TCP, ProtoInfo.HTTP_SRV, DEFAULT_PORT_NUM), false, true);
				session.getFilterChain().addLast(MinaSslFilter.NAME, sslFilter);
			}
		}
		
		session.getFilterChain().addLast("http_coder",new ProtocolCodecFilter(new ServerCodecFactory(new HttpMinaDecoder(),new HttpMinaEncoder())));
		
		session.getFilterChain().addLast(RTMPTConnectionMinaFilter.RTMPT_MINAFILTER, new RTMPTConnectionMinaFilter());
	}
	
	@Override
	public void messageReceived(IoSession session, Object message)
			throws Exception {
		
		if(message instanceof HTTPRequest) {
			
			final HTTPRequest request = (HTTPRequest)message;
			final HTTPResponse response = new HTTPResponse();
			String host = request.getHost();
			String path = request.getUrl();
			
			if(path == null) {
				
				path = "";	
			}
			
			String acceptHeader = request.getHeader("Accept");
			
			String queryString = null;
			
			int idx = path.indexOf("?");
			
			if (idx != -1) {

				queryString = path.substring(idx);
				path = path.substring(0,idx);
			}

			boolean canBeProxied = true;
			
			if(acceptHeader != null) {
				
				Matcher m = StaticAccept.matcher(acceptHeader);
				
				if(m.matches()) {
					
					canBeProxied = false;
				}
			}
			
			if(canBeProxied) {
			
				Matcher m = HasExtention.matcher(path);
				
				if(m.matches()) {
					
					m = DynamiExtention.matcher(path);
				
					if(!m.matches()) {
						
						canBeProxied = false;
					}
				}
			}
			
			log.debug("Accept:{}, canBeProxied:{}, path:{}, query:{}",request.getHeader("Accept"), canBeProxied, path, queryString);
			
			final String clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
			
			final String serverip = ((InetSocketAddress)session.getLocalAddress()).getAddress().getHostAddress();
			
			if(steakyCookieId == null) {
				
				steakyCookieId = new StringBuilder(Config.getApp().getApiPrefix()).append("_srvId").toString();
			}
			
			String serverIpFromCookie = request.getCookie(steakyCookieId);
			
			if(serverIpFromCookie != null) {
				
				serverIpFromCookie = new String(org.apache.commons.codec.binary.Base64.decodeBase64(serverIpFromCookie));
			}
			
			if(host == null || host.equals("")) {
				
				log.error("WRONG REQUEST FROM {} - Host directive was not set", clientip);
				response.setCode(HTTPCode.BadRequest);
				response.setCommonHeaders();
				session.write(response);
				return;
			}
			
			final int colonIndex = host.indexOf(":");
			
			if(colonIndex != -1) {
				
				host = host.substring(0, colonIndex);
			}
			
			//we use incomming port as the default port number
			final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
			
			final String originalRequestLine = new StringBuilder(host).append(path).toString();
			
			ResolveResult cfg = (ResolveResult) session.getAttribute(MinaSslFilter.RESOLVED_RESULT);
			
			if(cfg != null) {

				if(cfg.getFwdProto() == null) {
					
					cfg.setFwdProto("https");
				}

				if (queryString != null) {
					
					QueryStringHelper apiqstring = new QueryStringHelper();

					final ParsedQuery PQ = apiqstring.parseQs(queryString);
					queryString = PQ.queryString;
				}
				
				if(log.isDebugEnabled()) {
				
					log.debug("Received Https request isfull={},host={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
							false,host,path,"",queryString,clientip,cfg.getFwdPort(),cfg.getFwdProto());
				}
				
				if(session.containsAttribute(MinaSslFilter.LOOKUP_RESULT_REDIRECT)) {
					
					if(queryString == null) {
						
						queryString = "";
					}
					
					String redirect = new StringBuilder()
					.append(cfg.getFwdProto())
					.append("://")
					.append(cfg.getServer())
					.append(":")
					.append(DEFAULT_PORT_NUM)
					.append(path)
					.append(queryString).toString();
					
					log.debug("Redirecting https request to {} for redirect name {}",redirect, host);
					
					response.setCode(HTTPCode.MovedPermanently);
					response.setHeader("Location",redirect);
					response.setCommonHeaders();
					session.write(response);
					return;
				}
			}
			else {
				
				String proto = null;
				short fwdportnum = 0;
				String apikey= null;
				boolean isfull = false;
				
				final DomainHelper helper = new DomainHelper(host);
				
				if(helper.IsFullDomain()) {
					
					final String fwdPort = helper.GetFwdPort();

					if(fwdPort!=null) {
						
						try {
							
							fwdportnum = Short.parseShort(fwdPort);	
							
						} catch(Exception e) {}
					}
					
					apikey = helper.GetApiKey();
					proto = helper.GetFwdProto();
					isfull = true;
					host = helper.GetPureDomain();
				}
				else {

					if (queryString != null) {
						
						final QueryStringHelper apiqstring = new QueryStringHelper();

						final ParsedQuery PQ = apiqstring.parseQs(queryString);
						apikey = PQ.apiKey;
						final String fwdPort = PQ.fwdPort;
						
						if(fwdPort != null) {
							
							try {
								
								fwdportnum = Short.parseShort(fwdPort);	
								
							} catch(Exception e) {}
						}
						
						proto = PQ.fwdProto;
						queryString = PQ.queryString;
					}	
				}
				
				if(log.isDebugEnabled()) {
				
					log.debug("Received Http request isfull={},host={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
							isfull,host,path,apikey,queryString,clientip,fwdportnum,proto);
				}
	
				if(apikey != null) {
					
					try {
						
						if(fwdportnum == 0) {
							
							fwdportnum = DEFAULT_PORT_NUM;
						}
						
						cfg = Resolver.getBestServer(clientip,apikey,ServiceInfo.TCP,fwdportnum, true,
								serverIpFromCookie, canBeProxied, serverip);
						
					} catch (Exception e) {
						
						log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}>, API KEY <{}>",clientip, apikey,e);
						response.setCode(HTTPCode.NotFound);
						response.setCommonHeaders();
						session.write(response);
						return;
					}
				}
				else {
					
					try {

						LookupResult result = LookupService.dnsConfigLookup(host);
						
						if(result == null) {
							//only for test on localhost where cname does not work
							//result = new LookupResult("frfra-p80.www.0canq.cl.frfra.com");
							//result = new LookupResult("www.frfra.ir.", "frfra-h-fs.ir.0canq.cl.frfra.com.");
							
							log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - Error in resolving Host {} - CNAME CONFIG ERROR",clientip, host);
							response.setCode(HTTPCode.NotFound);
							response.setCommonHeaders();
							session.write(response);
							return;
						}

						if(result.isRedirectResult()) {
							
							String redirectTo = result.getRedirectName();
							
							if(queryString == null) {
								
								queryString = "";
							}
							
							proto = DEFULT_PROTO;
							
							if(result.isRedirectWithSSL()) {
								
								proto = "https";
							}
							
							String redirect = new StringBuilder()
							.append(proto)
							.append("://")
							.append(redirectTo)
							.append(":")
							.append(DEFAULT_PORT_NUM)
							.append(path)
							.append(queryString).toString();
							
							log.debug("Redirecting request to {} for redirect name {}",redirect, host);
							
							response.setCode(HTTPCode.MovedPermanently);
							response.setHeader("Location",redirect);
							response.setCommonHeaders();
							session.write(response);
							return;
						}
						
						log.debug("CNAME lookup result fullapi {} for {}", result.getFullApiResult(), host);
						
						if(result.getFullApiResult() != null) {
							
							if(fwdportnum == 0) {
								
								fwdportnum = result.getFullApiResult().getForwardPort();
							}
							
							if(proto == null) {
								
								proto = result.getFullApiResult().getForwardProto();
							}
						}
						else if(fwdportnum == 0) {
							
							fwdportnum = DEFAULT_PORT_NUM;
						}
						
						cfg = Resolver.getBestServer(clientip,result.getFqdnName(),ServiceInfo.TCP,
								fwdportnum, true, serverIpFromCookie, canBeProxied, serverip);
						
					} catch (Exception e) {
						
						log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - Error in resolving Host {} - ",clientip, host, e);
						response.setCode(HTTPCode.NotFound);
						response.setCommonHeaders();
						session.write(response);
						return;
					}
				}
				
				if(cfg == null) {
					
					response.setCode(HTTPCode.NotFound);
					response.setCommonHeaders();
					session.write(response);
					return;
				}
				
				if(proto == null) {
					
					if(session.getAttribute(MinaSslFilter.HAS_SSL) != null) {
						
						proto = "https";
					}
					else {
						
						proto = DEFULT_PROTO;
					}
				}
				
				cfg.setFwdProto(proto);
				cfg.setFwdPort(fwdportnum);
			}
			
			if(queryString == null) {
				
				queryString = "";
			}

			final String fwdPort = new StringBuilder().append(":").append(cfg.getFwdPort()).toString();
			
			String redirect = new StringBuilder()
				.append(cfg.getFwdProto())
				.append("://")
				.append(cfg.getServer())
				.append(fwdPort)
				.append(path)
				.append(queryString).toString();
			
			if(cfg.getHints() != null) {
				
				String contentType = request.getHeader("Content-Type");
				
				String accept = request.getHeader("Accept");
				
				if(contentType == null) {
					
					contentType = "";
				}
				
				if(accept == null) {
					
					accept = "";
				}
				
				for(Iterator<ResolveHint> rIt = cfg.getHints().iterator(); rIt.hasNext();) {
					
					final ResolveHint h = rIt.next();
					
					if(h.getByteType() == ResolveHintType.HTTP_STEAKY_SESSION.getValue()) {
						
						String cookie = new StringBuilder(steakyCookieId).append('=').append(org.apache.commons.codec.binary.Base64.
								encodeBase64String(cfg.getServer().getBytes())).toString();
						
						response.setHeader("Set-Cookie", cookie);
					}
					else if(h.getByteType() == ResolveHintType.HTTP_ORIGIN_REF.getValue()) {
						
						String refer = request.getHeader("Referer");
						
						if(refer!= null && !refer.equals("")) {
							
							/*final String testStr = new StringBuilder(path).append(queryString).toString();*/
							
							refer = org.apache.commons.codec.binary.
									Base64.encodeBase64String(refer.getBytes());
							
							refer = URLEncoder.encode(refer, "UTF-8");

							if(redirect.indexOf("?") != -1) {
								
								redirect = new StringBuilder(redirect)
									.append("&")
									.append(Config.ORIGIN_REFERRER).append("=")
									.append(refer).toString();
							}
							else {
								
								redirect = new StringBuilder(redirect)
								.append("?")
								.append(Config.ORIGIN_REFERRER).append("=")
								.append(refer).toString();
							}	
						}
					}
					else if(h.getByteType() == ResolveHintType.HTTP_SINGLE_DOMAIN.getValue() 
							&& !cfg.isWithProxy()) {
						
						if(!h.isFallback() || (request.getMethod() 
								!= HTTPMessage.Method.POST
								&& HTTPMessage.Method.HEAD != request.getMethod()
								&& HTTPMessage.Method.PUT != request.getMethod()
								&& path.indexOf(CROSS_DOMAIN) == -1
								&& !path.endsWith(HLS_EXTENTION)
								&& !path.endsWith(HDS_EXTENTION)
								&& contentType.indexOf(APPLICATION_CONTENT_TYPE) == -1
								&& accept.indexOf(JSON_APP_ACCEPT) == -1
								&& contentType.indexOf(XML_CONTENT_TYPE) == -1)) {
							
							if(h.getFields() != null && h.getFields().size() > 0 ) { // he needs referrer
								
								final String testStr = new StringBuilder(path).append(queryString).toString();
								String encodedOriginalRequestLine = org.apache.commons.codec.binary.
										Base64.encodeBase64String(originalRequestLine.getBytes());
								encodedOriginalRequestLine = URLEncoder.encode(encodedOriginalRequestLine, "UTF-8");
								
								if(testStr.indexOf("?") != -1) {

									redirect = new StringBuilder(redirect)
										.append("&")
										.append(Config.REFERRER).append("=")
										.append(encodedOriginalRequestLine).toString();
								}
								else {
									
									redirect = new StringBuilder(redirect)
									.append("?")
									.append(Config.REFERRER).append("=")
									.append(encodedOriginalRequestLine).toString();
								}
							}
							
							log.debug("Redirecting request to iframe {}",redirect);
							
							String contents = String.format(SINGLE_DOMAIN_IFRAME, redirect, redirect, redirect);
							
							response.setCode(HTTPCode.OK);
							//response.setHeader("Cache-Control", "no-cache");
							response.setHeader("Connection","close");
							response.setHeader("Content-Type","text/html");
							response.appendToBuffer(contents);
							session.write(response);
							
							return;
						}
					}
					else if(h.getByteType() == ResolveHintType.HTTP_SET_PROXY.getValue() 
							&& !cfg.isWithProxy()) {
						
						if(!h.isFallback()) { //right now no one supports set proxy
							
							log.debug("Redirecting request to setproxy {}",redirect);
							//TODO should not this just send an IP address?
							response.setCode(HTTPCode.Use_Proxy);
							String proxyURI = new StringBuilder()
							.append("SET ; proxyURI = \"")
							.append(redirect)
							.append("\"")
							.append(" scope=\"http://\", seconds=3").toString();
							response.setHeader("Set-proxy",proxyURI);
							session.write(response);
							return;
						}
					}
				}
			}
			
			if(cfg.isWithProxy()) {
				
				int method = request.getMethod() == Method.GET? HttpProxy.METHOD_GET:
					request.getMethod() == Method.POST?HttpProxy.METHOD_POST:
						request.getMethod() == Method.HEAD? HttpProxy.METHOD_HEAD:
							request.getMethod() == Method.PUT? HttpProxy.METHOD_PUT:
								request.getMethod() == Method.DELETE? HttpProxy.METHOD_DELETE:0;
				
				HttpProxy proxy = new HttpProxy(method, redirect, cfg.getServer(), request.getHeaders(),
						request.getBuffer(), request.getHeader("Content-Type"));
				
				ProxyResponse presonse = proxy.request();
				
				if(presonse != null && presonse.getContent() != null) {
					
					log.debug("Responsing proxy request for {} from {}", redirect, clientip);
					response.setHeaders(presonse.getHeaders());
					response.appendToBuffer(presonse.getContent());
					response.setCommonHeaders();
					session.write(response);
					return;
				}
				else {
					
					log.error("REQUEST FAIL FOR PROXY URL {} FROM {}", redirect, clientip);
					response.setCode(HTTPCode.BadRequest);
					response.setCommonHeaders();
					session.write(response);
					return;
				}
			}
			
			log.debug("Redirecting request to {}",redirect);
						
			response.setCode(HTTPCode.Temporary_Redirect);
			response.setHeader("Location",redirect);
			response.setCommonHeaders();
			session.write(response);
			return;
		}
		
		/*//THEN We assume its something about rtmpt
		IoHandlerAdapter handler = (IoHandlerAdapter) session.getAttribute(RTMPTMinaFilter.RTMP_MIN_HANDLER);
		
		if(handler != null) {

			handler.messageReceived(session, message);
			return;
		}*/
		
		//otherwise we don't no how to answer
		throw new Exception("Did not know how to answer");
	}
	
	@Override
	public void exceptionCaught(IoSession session, Throwable cause)
			throws Exception {
		
		if(cause instanceof IOException) {

			InetSocketAddress p = ((InetSocketAddress)session.getRemoteAddress());
			log.error("{} by {}:{}",cause.getMessage(), 
					p.getAddress().getHostAddress(), p.getPort());
			return;
		}
		
		log.error("",cause);
		
		NetProvider.closeMinaSession(session,true);
	}

	@SuppressWarnings("unused")
	private void crossDomainResp(HTTPResponse response) throws IOException {
		
		String crossdomain = "<?xml version=\"1.0\"?><!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\"><cross-domain-policy><allow-access-from domain=\"*\" /><site-control permitted-cross-domain-policies=\"all\"/></cross-domain-policy>";
		response.setCode(HTTPCode.OK);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection","close");
		response.setHeader("Content-Type","text/xml");
		response.appendToBuffer(crossdomain);
	}
	
	protected void handleRequest(String host, String path, String port) {
		
		
	}
}
