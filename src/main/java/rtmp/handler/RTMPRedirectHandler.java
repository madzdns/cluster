package rtmp.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.mina.core.session.IoSession;
import org.red5.io.object.StreamAction;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.IConnection.Encoding;
import org.red5.server.api.IContext;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.stream.IStreamService;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.ICommand;
import org.red5.server.net.rtmp.Channel;
import org.red5.server.net.rtmp.DeferredResult;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.RTMPHandler;
import org.red5.server.net.rtmp.RTMPMinaConnection;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.event.Ping;
import org.red5.server.net.rtmp.event.StreamActionEvent;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.red5.server.service.Call;
import org.red5.server.stream.StreamService;
import org.red5.server.util.ScopeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rtmp.RTMPTypes;
import rtmpt.RTMPTMinaFilter;
import server.api.LookupResult;
import server.api.LookupService;
import server.api.ResolveResult;
import server.api.Resolver;
import server.api.net.ssl.server.filter.MinaSslFilter;
import server.backend.node.dynamic.ServiceInfo;
import server.utils.DomainHelper;
import server.utils.QueryStringHelper;
import server.utils.QueryStringHelper.ParsedQuery;

public class RTMPRedirectHandler extends RTMPHandler {
	
	private final Logger log = LoggerFactory.getLogger(RTMPRedirectHandler.class);
	
	/** {@inheritDoc} */
	@SuppressWarnings({ "unchecked" })
	@Override
	protected void onCommand(RTMPConnection conn, Channel channel, Header source, ICommand command) {

		log.debug("onCommand {}", command);
		// get the call
		final IServiceCall call = command.getCall();
		log.trace("call: {}", call);
		// get the method name
		final String action = call.getServiceMethodName();
		
		log.debug("action>>>>>>>>>>>>{}", action);
		
		// If it's a callback for server remote call then pass it over to callbacks handler and return
		if ("_result".equals(action) || "_error".equals(action)) {
			
			handlePendingCallResult(conn, (Invoke) command);
			return;
		}
		boolean disconnectOnReturn = false;
		// "connected" here means that there is a scope associated with the connection (post-"connect")
		boolean connected = conn.isConnected();
		if (connected) {
			// If this is not a service call then handle connection...
			if (call.getServiceName() == null) {
				StreamAction streamAction = StreamAction.getEnum(action);
				if (log.isDebugEnabled()) {
					log.debug("Stream action: {}", streamAction.toString());
				}
				// TODO change this to an application scope parameter and / or change to the listener pattern
				if (isDispatchStreamActions()) {
					// pass the stream action event to the handler
					try {
						conn.getScope().getHandler().handleEvent(new StreamActionEvent(streamAction));
					} catch (Exception ex) {
						log.warn("Exception passing stream action: {} to the scope handler", streamAction, ex);
					}
				}
				//if the "stream" action is not predefined a custom clientBackend will be returned
				switch (streamAction) {
					case DISCONNECT:
						conn.close();
						break;
					case CREATE_STREAM:
					case INIT_STREAM:
					case CLOSE_STREAM:
					case RELEASE_STREAM:
					case DELETE_STREAM:
					case PUBLISH:
					case PLAY:
					case PLAY2:
					case SEEK:
					case PAUSE:
					case PAUSE_RAW:
					case RECEIVE_VIDEO:
					case RECEIVE_AUDIO:
						IStreamService streamService = (IStreamService) ScopeUtils.getScopeService(conn.getScope(), IStreamService.class, StreamService.class);
						Status status = null;
						try {
							log.debug("Invoking {} from {} with service: {}", new Object[] { call, conn, streamService });
							if (invokeCall(conn, call, streamService)) {
								log.debug("Stream service invoke {} success", action);
							} else {
								status = getStatus(NS_INVALID_ARGUMENT).asStatus();
								status.setDescription(String.format("Failed to %s (stream id: %d)", action, source.getStreamId()));
							}
						} catch (Throwable err) {
							log.error("Error while invoking {} on stream service. {}", action, err);
							status = getStatus(NS_FAILED).asStatus();
							status.setDescription(String.format("Error while invoking %s (stream id: %d)", action, source.getStreamId()));
							status.setDetails(err.getMessage());
						}
						if (status != null) {
							channel.sendStatus(status);
						} else {
							log.debug("Status for {} was null", action);
						}
						break;
					default:
						log.debug("Defaulting to invoke for: {}", action);
						invokeCall(conn, call);
				}
			} else {
				// handle service calls
				invokeCall(conn, call);
			}
			
		} else if (StreamAction.CONNECT.equals(action)) {
				
			log.debug("connect stream adction");
			final Map<String, Object> params = command.getConnectionParams();
			
			String host = getHostname((String) params.get("tcUrl"));
			String path = (String) params.get("app");
			String queryString = "";
			
			String colonPort = "";
			
			if(host != null) {
			
				int colonIndex = host.indexOf(":");
				if(colonIndex != -1) {
					
					host = host.substring(0, colonIndex);
					colonPort = host.substring(colonIndex);
				}
			}
			
			params.put("path", path);
			
			String setupHost = new StringBuilder(host).append(colonPort).toString();
			
			conn.setup(setupHost, path, params);
			
			try {
					
				try {
					
					IoSession session = null;
					
					String clientip = null;
					
					short DEFAULT_PORT_NUM = 0;
					
					boolean RTMPT_FROM_CONN_TYPE = false;
					
					if(conn instanceof RTMPMinaConnection) {
						
						session = ((RTMPMinaConnection)conn).getIoSession();
						
						clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
						
						DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();
					}
					else if(conn instanceof RTMPTConnection) {
						
						session = ((RTMPTConnection)conn).getIoSession();
						
						clientip = ((RTMPTConnection) conn).getRemoteAddress();
						DEFAULT_PORT_NUM = (short)((RTMPTConnection) conn).getRemotePort();
						
						RTMPT_FROM_CONN_TYPE = true;
					}
					
					call.setStatus(Call.STATUS_SUCCESS_RESULT);
					
					if (call instanceof IPendingServiceCall) {
						
						IPendingServiceCall pc = (IPendingServiceCall) call;

						StatusObject result = getStatus(NC_CONNECT_REJECTED);//getStatus(NC_CONNECT_SUCCESS);
						
						if( host == null || clientip == null) {
							
							throw new ClientRejectedException("Malformed Request");
						}
						
						ResolveResult cfg = null;
						
						if(session != null) {
							
							cfg = (ResolveResult) session.getAttribute(MinaSslFilter.RESOLVED_RESULT);
						}
						
						final List<RTMPTypes> sessionProtocols = new ArrayList<RTMPTypes>(8);
						
						if(cfg != null) {
							
							if(cfg.getFwdProto() == null) {
								
								sessionProtocols.add(RTMPTypes.RTMPS);
							}
							
							if (path != null && path.indexOf("?") != -1) {
								
								final int idx = path.indexOf("?");
								queryString = path.substring(idx);
								
								final QueryStringHelper apiqstring = new QueryStringHelper();
								
								final ParsedQuery PQ = apiqstring.parseQs(queryString);
								
								queryString = PQ.queryString;
								path = path.substring(0,idx);
							}

							if(cfg.getFwdPort() <= 0) {
							
								cfg.setFwdPort(DEFAULT_PORT_NUM);
							}
							
							if(log.isDebugEnabled()) {
							
								log.debug("Received RTMP?S request isfull={},tcUrl={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
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
										
									} catch(Exception e){}
								}
								
								apikey = helper.GetApiKey();
								proto = helper.GetFwdProto();
								isfull = true;
								host = helper.GetPureDomain();
							}
							else {
								
								if (path != null && path.indexOf("?") != -1) {
									
									final int idx = path.indexOf("?");
									queryString = path.substring(idx);
									
									final QueryStringHelper apiqstring = new QueryStringHelper();
									
									final ParsedQuery PQ = apiqstring.parseQs(queryString);
									apikey = PQ.apiKey;
									final String fwdPort = PQ.fwdPort;
									
									if(fwdPort!=null) {
										
										try {
											
											fwdportnum = Short.parseShort(fwdPort);	
											
										} catch(Exception e) {}
									}
									
									queryString = PQ.queryString;
									proto = PQ.fwdProto;
									//params.put("queryString",queryString);
									path = path.substring(0,idx);
								}	
							}

							if(apikey != null) {
								
								try {
									
									if(fwdportnum == 0) {
										
										fwdportnum = DEFAULT_PORT_NUM;
									}
									
									cfg = Resolver.getBestServer(clientip,apikey,ServiceInfo.TCP,fwdportnum, false);
								
								} catch (Exception e) {
									
									log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - RESOLVE KEY {} -",clientip, apikey, e);
									throw new ClientRejectedException("Could Not Find Proper Node To Answer");
								}
							}
							else {
								
								try {

									LookupResult lresult = LookupService.dnsConfigLookup(host);
									
									if(lresult == null) {
										
										//only for test on localhost where cname does not work
										//lresult = new LookupResult("frfra-p5080.www.0canq.cl.frfra.com", "frfra-p5080.www.0canq.cl.frfra.com");
										//lresult = new LookupResult("www.frfra.ir.", "frfra-h-p1935.www.0canq.cl.frfra.com.");
										
										throw new IOException("CNAME CONFIG EXCEPTION");
									}
									
									if(lresult.isRedirectResult() && !lresult.isHttpOnly()) {
										
										String redirectTo = lresult.getRedirectName();
										
										if(queryString == null) {
											
											queryString = "";
										}
										
										if(lresult.isRedirectWithSSL()) {
											
											sessionProtocols.add(RTMPTypes.RTMPS);
										}
										
										cfg = new ResolveResult(null, redirectTo, false);
										fwdportnum = DEFAULT_PORT_NUM;
									}
									else {
									
										if(lresult.getFullApiResult() != null) {
											
											if(fwdportnum == 0) {
												
												fwdportnum = lresult.getFullApiResult().getForwardPort();
											}
											
											if(proto == null) {
												
												proto = lresult.getFullApiResult().getForwardProto();
											}
										}
										else if(fwdportnum == 0) {
											
											fwdportnum = DEFAULT_PORT_NUM;
										}
										
										cfg = Resolver.getBestServer(clientip,lresult.getFqdnName(),ServiceInfo.TCP,fwdportnum, false);
									}
									
								} catch (IOException e) {
									
									log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, host, e);
									throw new ClientRejectedException("Domain Was Not Properly Configured For LoadBalancers");
								}
								catch (Exception e) {
									
									log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, host, e);
									throw new ClientRejectedException("Could Not Find Proper Node To Answer");
								}
							}
							
							if(cfg == null) {
								
								throw new ClientRejectedException("Could Not Find Proper Node To Answer");
							}
							
							if(proto == null) {
								
								if(session.getAttribute(MinaSslFilter.HAS_SSL) != null) {
									
									sessionProtocols.add(RTMPTypes.RTMPS);
								}
							}
							else {
								
								cfg.setFwdProto(proto);
							}
							
							cfg.setFwdPort(fwdportnum);
							
							if(log.isDebugEnabled()) {
								
								log.debug("Received Rtmp request isfull={},tcUrl={},path={},apikey={},querystring={},clientip={},fwdport={},fwdproto={}",
										isfull,host,path,apikey,queryString,clientip,cfg.getFwdPort(),cfg.getFwdProto());
							}
							
						}

						final String fwdPort = new StringBuilder().append(":").append(cfg.getFwdPort()).toString();
						
						if(cfg.getFwdProto() == null) {
							
							if(session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN) != null) {
								
								sessionProtocols.add(RTMPTypes.RTMPE);
							}
							
							if(RTMPT_FROM_CONN_TYPE) {
								
								sessionProtocols.add(RTMPTypes.RTMPT);
							}
							else {
								
								Object flag = session.getAttribute(RTMPTMinaFilter.RTMPT_FLAG);
								
								if(flag != null && ((Boolean)flag)) {
									
									sessionProtocols.add(RTMPTypes.RTMPT);
								}	
							}

							cfg.setFwdProto(RTMPTypes.getFinalProto(sessionProtocols).getType());
						}
						
						final String redirect = new StringBuilder()
							.append(cfg.getFwdProto())
							.append("://")
							.append(cfg.getServer())
							.append(fwdPort)
							.append("/")
							.append(path)
							.append(queryString).toString();

						/*,IoBuffer.wrap(redirect.getBytes()).getHexDump()*/
						log.debug("Redirecting request to {}", redirect);
						
						final ObjectMap<String,String> extra = new ObjectMap<String, String>();
						extra.put("code","302");
						extra.put("redirect",redirect);
						result.setAdditional("ex",extra);
						pc.setResult(result);
						
						session.setAttribute("_ff_redirect", redirect);
					}
					// Measure initial roundtrip time after connecting
					conn.ping(new Ping(Ping.STREAM_BEGIN, 0, -1));
					disconnectOnReturn = false;
						
				} catch (ClientRejectedException rejected) {
					
					log.debug("Connect rejected");
					
					call.setStatus(Call.STATUS_ACCESS_DENIED);
					
					if (call instanceof IPendingServiceCall) {
						
						IPendingServiceCall pc = (IPendingServiceCall) call;
						StatusObject status = getStatus(NC_CONNECT_REJECTED);
						Object reason = rejected.getReason();
						
						if (reason != null) {
							
							status.setApplication(reason);
							//should we set description?
							status.setDescription(reason.toString());
						}
						
						pc.setResult(status);
					}
					
					disconnectOnReturn = true;
				}
				
			} catch (RuntimeException e) {
				
				call.setStatus(Call.STATUS_GENERAL_EXCEPTION);
				
				log.error("Error connecting {}", e);
				
				if (call instanceof IPendingServiceCall) {
					
					IPendingServiceCall pc = (IPendingServiceCall) call;
					pc.setResult(getStatus(NC_CONNECT_FAILED));
				}
				
				disconnectOnReturn = true;
			}
			// Evaluate request for AMF3 encoding
			// Evaluate request for AMF3 encoding
            if (new Double(3d).equals(params.get("objectEncoding"))) {
                if (call instanceof IPendingServiceCall) {
                    Object pcResult = ((IPendingServiceCall) call).getResult();
                    Map<String, Object> result;
                    if (pcResult instanceof Map) {
                        result = (Map<String, Object>) pcResult;
                        result.put("objectEncoding", 3);
                    } else if (pcResult instanceof StatusObject) {
                        result = new HashMap<String, Object>();
                        StatusObject status = (StatusObject) pcResult;
                        result.put("code", status.getCode());
                        result.put("description", status.getDescription());
                        result.put("application", status.getApplication());
                        result.put("level", status.getLevel());
                        result.put("objectEncoding", 3);
                        ((IPendingServiceCall) call).setResult(result);
                    }
                }
                conn.getState().setEncoding(Encoding.AMF3);
            }
				
		} else {
			
			// not connected and attempting to send an invoke
			log.warn("Not connected, closing connection for action {}", action);
			conn.close();
		}
		
		if (command instanceof Invoke) {
			
			if ((source.getStreamId().intValue() != 0) && (call.getStatus() == Call.STATUS_SUCCESS_VOID || call.getStatus() == Call.STATUS_SUCCESS_NULL)) {
				// This fixes a bug in the FP on Intel Macs.
				log.debug("Method does not have return value, do not reply");
				return;
			}
			
			boolean sendResult = true;
			
			if (call instanceof IPendingServiceCall) {
				
				IPendingServiceCall psc = (IPendingServiceCall) call;
				Object result = psc.getResult();
				
				if (result instanceof DeferredResult) {
					
					// Remember the deferred result to be sent later
					DeferredResult dr = (DeferredResult) result;
					dr.setServiceCall(psc);
					dr.setChannel(channel);
					dr.setTransactionId(command.getTransactionId());
					conn.registerDeferredResult(dr);
					sendResult = false;
				}
			}
			
			if (sendResult) {
				// The client expects a result for the method call.
				Invoke reply = new Invoke();
				reply.setCall(call);
				reply.setTransactionId(command.getTransactionId());
				channel.write(reply);
				
				if (disconnectOnReturn) {
					
					log.debug("Close connection due to connect handling exception: {}", conn.getSessionId());
					conn.close();
				}
			}
		}
	}
	
	private boolean invokeCall(RTMPConnection conn, IServiceCall call, Object service) {
		
		final IScope scope = conn.getScope();
		final IContext context = scope.getContext();
		log.debug("Scope: {} Context: {}", scope, context);
		log.debug("Service: {}", service);
		return context.getServiceInvoker().invoke(call, service);
	}
}
