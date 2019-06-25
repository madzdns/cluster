package rtsp;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxlibp.media.MediaInfo;
import jxlibp.packetize.exception.CodecNotSupportedException;
import jxlibp.packetize.rtp.RTPpacketizer;

import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rtp.handler.RtpTransportHandler;
import rtsp.RTSPRequest;
import rtsp.RTSPResponse;
import rtsp.RTSPMessage;
import rtsp.Verb;
import rtsp.RTSPTransport;
import server.api.LookupResult;
import server.api.LookupService;
import server.api.ResolveResult;
import server.api.Resolver;
import server.api.net.ssl.server.filter.MinaSslFilter;
import server.backend.node.dynamic.ServiceInfo;
import server.utils.DomainHelper;
import server.utils.QueryStringHelper;
import server.utils.QueryStringHelper.ParsedQuery;

public class RTSPNegotiation {
	
	private final Logger log = LoggerFactory.getLogger(RTSPNegotiation.class);
	
	private final static String DEFULT_PROTO = "rtsp";

	private final static Pattern urlp = Pattern.compile("rtsp://(.*?)/(.*)",Pattern.CASE_INSENSITIVE);
	
	/*private final static Pattern urlp = Pattern.compile("^(?:(?:rtsp|rtsps)://)(?:\\S+(?::\\S*)?@)?(?:(?:(?:[1-9]\\d?|1\\d\\d|2[01]\\d|22[0-3])(?:\\.(?:1?\\d{1,2}|2[0-4]\\d|25[0-5])){2}(?:\\.(?:[0-9]\\d?|1\\d\\d|2[0-4]\\d|25[0-4]))|(?:(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)(?:\\.(?:[a-z\\u00a1-\\uffff0-9]+-?)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))|localhost)(?::\\d{2,5})?(?:(/|\\?|#)[^\\s]*)?$",
			Pattern.CASE_INSENSITIVE);*/
	
	private final static QueryStringHelper apiqstring = new QueryStringHelper();
	
	private final static String FORWARD_STATUS_KEY = RTSPNegotiation.class.getName()+"server stae";
	
	private final static String NOT_FOUND_MEDIA_INFO_KEY = RTSPNegotiation.class.getName()+"MediAINf o stae";
	
	private final static String RTSP_TRANSPORT_KEY = RTSPNegotiation.class.getName()+"RtspTTCon stae";
	
	private boolean getForwardStatus(final IoSession session) {
		
		Object status = session.getAttribute(FORWARD_STATUS_KEY);
		
		if(status == null) {
			
			return true;
		}
		
		return false;
	}
	
	private MediaInfo getNotFoundMediaInfo(final IoSession session) {
		
		String path =  (String)session.getAttribute(NOT_FOUND_MEDIA_INFO_KEY);
		
		if( path != null) {
			
			MediaInfo mediaInfo = new MediaInfo(path);
			
			if(!mediaInfo.isFailed()) {
				
				return mediaInfo;
			}
		}
		return null;
	}
	
	private RTSPSessionTransports getRtspTransport(final IoSession session) {
		
		return (RTSPSessionTransports)session.getAttribute(RTSP_TRANSPORT_KEY);
	}
	
	private void setRtspTransport(final IoSession session, final RTSPSessionTransports transports) {
		
		session.getAttribute(RTSP_TRANSPORT_KEY,transports);
	}
	
	public RTSPResponse make(final IoSession session, final RTSPMessage message) {
		
		if(message.getType() != RTSPMessage.Type.TypeRequest)
			
			return null;
		
		RTSPRequest request = (RTSPRequest) message;
		
		Verb verb = request.getVerb();
		
		RTSPResponse response = new RTSPResponse();
		
		try {	
			
			switch (verb) {	
			
				case OPTIONS:
					
					response.setCommonHeaders();
					response.setHeader("Content-Length","0");
					response.setHeader("CSeq",request.getHeader("CSeq","1"));
					response.setHeader("Public","DESCRIBE,GET_PARAMETER,OPTIONS,PAUSE,PLAY,RECORD,REDIRECT,SETUP,TEARDOWN");
					
					break;
				
				case DESCRIBE:
					
					if(getForwardStatus(session)) {
						
						if(redirect(session,response,(RTSPRequest)request,
								((InetSocketAddress)session.getRemoteAddress())
								.getAddress().getHostAddress())) {
							
							break;
						}
						
						if (getForwardStatus(session)) {
							
							break;
						}
					}
					
					MediaInfo mediaInfo = getNotFoundMediaInfo(session);
					
					if( mediaInfo == null ) {
						
						response.setCode(RTSPCode.NotFound);
						
						break;
					}
					
					String sdp = mediaInfo.getSdpString();
					
					log.debug("SDP {} ",sdp);
					
					response.setCommonHeaders();
					
					response.setHeader("Date", new Date().toString());
					
					response.setHeader("Content-clientBackend", "application/sdp");
					
					response.setHeader("Content-Length", String.valueOf(sdp.length()));
					
					response.setHeader("Content-Base", ((RTSPRequest)request).getUrl().toString());	
					
					response.setHeader("Cache-Control", "no-cache");
					
					response.setHeader("CSeq", ((RTSPRequest)request).getHeader("CSeq","1"));
					
					StringBuffer other = new StringBuffer();
					
					other.append(sdp);
					
					response.appendToBuffer(other);
					
					break;
				
				case SETUP:
					
					if(getForwardStatus(session)) {
						
						if(redirect(session,response,(RTSPRequest)request,
								((InetSocketAddress)session.getRemoteAddress())
								.getAddress().getHostAddress())) {
							
							break;
						}
						
						if (getForwardStatus(session)) {
							
							break;
						}
					}
					
					mediaInfo = getNotFoundMediaInfo(session);
					
					if( mediaInfo == null ) {
						
						response.setCode(RTSPCode.NotFound);
						
						break;
					}
					
					response.setCommonHeaders();
					
					response.setHeader("Date", new Date().toString());
					
					String transportStr = ((RTSPRequest)request).getHeader("Transport");
					
					RTSPSessionTransports transports = null;
					
					RTSPTransport rtspTransport = null;
					
					String sessid = ((RTSPRequest)request).getHeader("Session");
					
					int trackId = request.getTrackId();
					
					boolean newTransport = false;
					
					boolean newTransports = false;
					
					if(sessid != null) {
						
						transports = getRtspTransport(session);
						
						if( transports == null) {
							
							newTransports = true;
							
							transports = new RTSPSessionTransports();
						}
						else
						
							rtspTransport = transports.getRTSPTransport(trackId);	
					}
					else {
						
						transports = new RTSPSessionTransports();
						
						newTransports = true;
					}
					
					if(rtspTransport == null) {
						
						rtspTransport = new RTSPTransport(transportStr,request.getUrl());
						
						newTransport = true;
					}
					
					if(rtspTransport.getSSRC() == null) {
						
						rtspTransport.setSSRC(new Random().nextLong());
					}
					
					try {
						
						RtpTransportHandler connection = 
								new RtpTransportHandler(new RTPpacketizer(mediaInfo,trackId), rtspTransport
										,((InetSocketAddress)session.getRemoteAddress())
										.getAddress(),((InetSocketAddress)session.getLocalAddress()).getAddress());
						
						rtspTransport.setLocalPorts(rtspTransport.getLocalPorts());
	
						if(sessid == null || newTransports) {
							
							sessid = RTSPSession.create(session).getId();
							
							setRtspTransport(session, transports);	
						}
						else {
							
							ITransportHandler transportHandler = rtspTransport.getTransportHandler();
							
							//XXX not from rfc. should read whats the rule
							if(transportHandler != null) {
								
								transportHandler.stop();
							}
						}
						
						rtspTransport.setTransportHandler(connection);
						
						if(newTransport) {
							
							transports.addTransport(trackId, rtspTransport);
						}
						
						rtspTransport.setMode("play");
						
						response.setHeader("Transport",rtspTransport.toString());
						response.setHeader("Session",sessid );
						response.setHeader("Content-Length", "0");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("CSeq", ((RTSPRequest)request).getHeader("CSeq","1"));
						
					}
					catch(ConnectException e) {
						
						log.debug("",e);
						
						response.setCode(RTSPCode.InternalServerError);
					}
					catch (CodecNotSupportedException e) {
						
						log.debug("{}",e.getMessage());
						
						response.setCode(RTSPCode.UnsupportedMediaType);
					}
					
					break;
					
				case PLAY:
					
					response.setCommonHeaders();
					
					sessid = ((RTSPRequest)request).getHeader("Session");
					
					if(!RTSPSession.isContains(session,sessid))
						
						response.setCode(RTSPCode.SessionNotFound);
					
					else {
						
						transports = getRtspTransport(session);
						
						trackId = request.getTrackId();
						
						if(!transports.play(trackId)) {
							
							response.setCode(RTSPCode.NotFound);
							
							break;
						}
						
						response.setHeader("Date", new Date().toString());
						response.setHeader("Session", sessid);
						response.setHeader("Content-Length", "0");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("CSeq", ((RTSPRequest)request).getHeader("CSeq","1"));
						
						response.setHeader("RTP-Info", transports.createTransportInfo(trackId));
					}
					
					break;
					
				case GET_PARAMETER:
					
					response.setCommonHeaders();
					
					sessid = ((RTSPRequest)request).getHeader("Session");
					
					if(!RTSPSession.isContains(session,sessid))
						
						response.setCode(RTSPCode.SessionNotFound);
					
					else {
						
						response.setHeader("Date", new Date().toString());
						response.setHeader("Session",sessid);
						response.setHeader("Content-Length", "0");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("CSeq", ((RTSPRequest)request).getHeader("CSeq","1"));
					}
					
					break;
					
				case TEARDOWN:
					
					response.setCommonHeaders();
					
					sessid = ((RTSPRequest)request).getHeader("Session");
					
					if(!RTSPSession.isContains(session,sessid))
						
						response.setCode(RTSPCode.SessionNotFound);
					
					else {
						
						transports = getRtspTransport(session);
						
						trackId = request.getTrackId();
						
						if(transports.stop(trackId)) {
							
							setRtspTransport(session, null);
							
							RTSPSession.close(session, sessid);
						}
						
						response.setHeader("Date", new Date().toString());
						
						response.setHeader("Session",sessid);
						
						response.setHeader("Content-Length", "0");
						
						response.setHeader("Cache-Control", "no-cache");
						
						response.setHeader("CSeq", ((RTSPRequest)request).getHeader("CSeq","1"));					
					}
					
					break;
					
				default:
					
					response.setCommonHeaders();
					response.setCode(RTSPCode.OptionNotSupported);
					
					break;
			}
			
		} catch (Exception e) {
			/*
			 * error on input stream should not happen since the input stream is
			 * coming from a bytebuffer.
			 */
			log.error("", e);
			return null;
		}
		
		return response;
	}
	
	private void handleNotFound(final IoSession session) {
		
		//session.setAttribute(FORWARD_STATUS_KEY, Boolean.FALSE);
			
		//session.setAttribute(NOT_FOUND_MEDIA_INFO_KEY, "res/sample.mp4");
	}
	
	private boolean redirect(final IoSession session, final RTSPResponse response, final RTSPRequest request,
			final String clientip) {
		
		response.setCommonHeaders();
		
		response.setHeader("CSeq",((RTSPRequest)request).getHeader("CSeq","1"));
		
		if(request.getUrl()==null) {
			
			response.setCode(RTSPCode.NotAcceptable);
			
			return false;
		}
		
		final String url = request.getUrl().toString();
		
		final Matcher m = urlp.matcher(url);
		
		if(!m.matches()) {
			
			log.warn("WRONG RTSP MSG - REQUEST SROURCE <{}>", clientip);
			response.setCode(RTSPCode.NotAcceptable);
			
			return false;
		}
			
		String host = m.group(1);
		
		String path = m.group(2);
		
		String queryString = "";
		
		boolean movedPermanent = false;
		
		if(host != null) {
		
			final int colonIndex = host.indexOf(":");
			if(colonIndex != -1) {
				
				host = host.substring(0, colonIndex);
			}
		}
		
		final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
		
		ResolveResult cfg = (ResolveResult) session.getAttribute(MinaSslFilter.RESOLVED_RESULT);
		
		if(cfg != null) {
			
			if(cfg.getFwdProto() == null) {
				
				cfg.setFwdProto("rtsps");
			}
			
			if (path != null && path.indexOf("?") != -1) {
				
				int idx = path.indexOf("?");
				queryString = path.substring(idx);
				final ParsedQuery PQ = apiqstring.parseQs(queryString);

				queryString = PQ.queryString;
				path = path.substring(0,idx);
			}
			
			if(cfg.getFwdPort() <= 0) {
				
				cfg.setFwdPort(DEFAULT_PORT_NUM);
			}
			
			if(log.isDebugEnabled()) {
				
				log.debug("Received RTSPS request isfull={},host={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
						false,host,path,"",queryString,clientip,cfg.getFwdPort(),cfg.getFwdProto());
			}
		}
		else {
			
			String apikey= null;
			short fwdportnum = 0;
			boolean isfull = false;
			String proto = null;
			
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
				
				if (path != null && path.indexOf("?") != -1) {
					
					int idx = path.indexOf("?");
					queryString = path.substring(idx);
					final ParsedQuery PQ = apiqstring.parseQs(queryString);
					apikey = PQ.apiKey;
					final String fwdPort = PQ.fwdPort;
					
					if(fwdPort!=null) {
						
						try {
							
							fwdportnum = Short.parseShort(fwdPort);	
							
						} catch(Exception e) {}
						
						if(fwdportnum == 0) {}
					}
					proto = PQ.fwdProto;
					queryString = PQ.queryString;
					path = path.substring(0,idx);
				}	
			}
			
			if(log.isDebugEnabled()) {
			
				log.debug("Received Rtsp request isfull={},host={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
						isfull,host,path,apikey,queryString,clientip,fwdportnum,proto);
			}
			
			if(apikey!=null) {
				
				try {
					
					if(fwdportnum == 0) {
						
						fwdportnum = DEFAULT_PORT_NUM;
					}
					
					cfg = Resolver.getBestServer(clientip,apikey,ServiceInfo.TCP,fwdportnum, false);
					
				} catch (Exception e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - RESOLVE KEY <{}> - ", clientip, apikey, e);
					
					handleNotFound(session);
					
					return false;
				}
			}
			else {
				
				try {
					
					LookupResult result = LookupService.dnsConfigLookup(host);
					
					if(result == null) {
						
						//only for test on localhost where cname does not work
						//result = new LookupResult("frfra-p554.www.0canq.cl.frfra.com");
						//result = new LookupResult("www.frfra.com.", "frfra-h-p1935.www.0canq.cl.frfra.com.");
						throw new IOException("CNAME CONFIG EXCEPTION");
					}
					
					if(result.isRedirectResult() && !result.isHttpOnly()) {
						
						String redirectTo = result.getRedirectName();
						
						if(queryString == null) {
							
							queryString = "";
						}
						
						if(result.isRedirectWithSSL()) {
							
							proto = "rtsps";
						}
						
						cfg = new ResolveResult(null, redirectTo, false);
						fwdportnum = DEFAULT_PORT_NUM;
						
						movedPermanent = true;
					}
					else {
						
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
		
						cfg = Resolver.getBestServer(clientip,result.getFqdnName(),ServiceInfo.TCP,fwdportnum, false);	
					}
					
				} catch (Exception e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - Host <{}> - ", clientip, host, e);
					
					handleNotFound(session);
					
					return false;
				}
			}
			
			if(cfg == null) {
				
				handleNotFound(session);
				
				return false;
			}
			
			if(proto == null) {
				
				if(session.getAttribute(MinaSslFilter.HAS_SSL) != null) {
					
					proto = "rtsps";
				}
				else {
					
					proto = DEFULT_PROTO;
				}
			}
			
			cfg.setFwdProto(proto);
			cfg.setFwdPort(fwdportnum);
		}
			

		final String fwdPort = new StringBuilder().append(":").append(cfg.getFwdPort()).toString();

		String redirect = new StringBuilder()
			.append(cfg.getFwdProto())
			.append("://")
			.append(cfg.getServer())
			.append(fwdPort)
			.append("/")
			.append(path)
			.append(queryString).toString();
		
		log.debug("Redirecting request to {}",redirect);
		
		response.setCode(!movedPermanent?RTSPCode.MovedTemporarily:RTSPCode.MovedPermanently);
		response.setHeader("Location",redirect);
		return true;
	}
}
