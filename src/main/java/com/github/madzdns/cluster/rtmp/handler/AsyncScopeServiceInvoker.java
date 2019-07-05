package com.github.madzdns.cluster.rtmp.handler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.github.madzdns.cluster.rtmp.RTMPTypes;
import org.apache.mina.core.session.IoSession;
import org.red5.io.utils.ObjectMap;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IServiceCall;
import org.red5.server.api.service.IServiceInvoker;
import org.red5.server.exception.ClientRejectedException;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.status.StatusObject;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmp.status.StatusObjectService;
import org.red5.server.service.Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.rtmpt.RTMPTMinaFilter;
import com.github.madzdns.cluster.core.api.AsyncLookupResult;
import com.github.madzdns.cluster.core.api.LookupResult;
import com.github.madzdns.cluster.core.api.ResolveResult;
import com.github.madzdns.cluster.core.api.Resolver;
import com.github.madzdns.cluster.core.api.net.ssl.server.filter.MinaSslFilter;
import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;

public class AsyncScopeServiceInvoker implements IServiceInvoker{
	
	private final Logger log = LoggerFactory.getLogger(AsyncScopeServiceInvoker.class);
	
	IoSession session;
	
	StatusObjectService statusObjectService;
	
	public AsyncScopeServiceInvoker(IoSession session, StatusObjectService statusObjectService) {
		
		this.session = session;
		
		this.statusObjectService = statusObjectService;
	}

	@Override
	public boolean invoke(IServiceCall call, IScope scope) {
		
		final String clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
		
		final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();

		AsyncLookupResult alresult = (AsyncLookupResult)session.getAttribute("_gg");
		
		boolean ready = alresult.isResultReady();
		
		if(!ready) {
			
			return false;
		}
		else {
			
			LookupResult lresult = alresult.getResult();
			
			if(lresult == null) {

				call.setStatus(Call.STATUS_ACCESS_DENIED);
				
				if (call instanceof IPendingServiceCall) {
					
					IPendingServiceCall pc = (IPendingServiceCall) call;
					StatusObject status = getStatus(StatusCodes.NC_CONNECT_REJECTED);
					Object reason = "CNAME CONFIG EXCEPTION";
					
					if (reason != null) {
						
						status.setApplication(reason);
						//should we set description?
						status.setDescription(reason.toString());
					}
					
					pc.setResult(status);
					
					return true;
				}
			}
			else {
			
				String apikey= null;
				short fwdportnum = 0;
				boolean isfull = false;
				String proto = null;
				
				IPendingServiceCall pc = (IPendingServiceCall) call;
				
				StatusObject result = getStatus(StatusCodes.NC_CONNECT_REJECTED);
				
				ResolveResult cfg = null;
				
				final List<RTMPTypes> sessionProtocols = new ArrayList<RTMPTypes>(7);
				
				try {
					
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
				
				} catch (IOException e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, alresult.getHost(), e);
					throw new ClientRejectedException("The Domain Has Not Properly Configured For LoadBalancers");
				}
				catch (Exception e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, alresult.getHost(), e);
					throw new ClientRejectedException("Could Not Find Proper Node To Answer");
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
							isfull,alresult.getHost(),alresult.getPath(),apikey,alresult.getQuerySring(),clientip,cfg.getFwdPort(),cfg.getFwdProto());
				}
			
				final String fwdPort = new StringBuilder().append(":").append(cfg.getFwdPort()).toString();
				
				if(cfg.getFwdProto() == null) {
					
					if(session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN) != null) {
						
						sessionProtocols.add(RTMPTypes.RTMPE);
					}
					
					Object flag = session.getAttribute(RTMPTMinaFilter.RTMPT_FLAG);
					
					if(flag != null && ((Boolean)flag)) {
						
						sessionProtocols.add(RTMPTypes.RTMPT);
					}

					cfg.setFwdProto(RTMPTypes.getFinalProto(sessionProtocols).getType());
				}
				
				final String redirect = new StringBuilder()
					.append(cfg.getFwdProto())
					.append("://")
					.append(cfg.getServer())
					.append(fwdPort)
					.append("/")
					.append(alresult.getPath())
					.append(alresult.getQuerySring()).toString();
				
				log.debug("Redirecting request to {}",redirect);
				
				final ObjectMap<String,String> extra = new ObjectMap<String, String>();
				extra.put("code","302");
				extra.put("redirect",redirect);
				result.setAdditional("ex",extra);
				pc.setResult(result);
				
				return true;
			}
		}
		
		return false;
	}

	@Override
	public boolean invoke(IServiceCall call, Object service) {
		
		final String clientip = ((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress();
		
		final short DEFAULT_PORT_NUM = (short)((InetSocketAddress)session.getLocalAddress()).getPort();

		AsyncLookupResult alresult = (AsyncLookupResult)session.getAttribute("_gg");
		
		boolean ready = alresult.isResultReady();
		
		if(!ready) {
			
			return false;
		}
		else {
			
			LookupResult lresult = alresult.getResult();
			
			if(lresult == null) {

				call.setStatus(Call.STATUS_ACCESS_DENIED);
				
				if (call instanceof IPendingServiceCall) {
					
					IPendingServiceCall pc = (IPendingServiceCall) call;
					StatusObject status = getStatus(StatusCodes.NC_CONNECT_REJECTED);
					Object reason = "CNAME CONFIG EXCEPTION";
					
					if (reason != null) {
						
						status.setApplication(reason);
						//should we set description?
						status.setDescription(reason.toString());
					}
					
					pc.setResult(status);
					
					return true;
				}
			}
			else {
			
				String apikey= null;
				short fwdportnum = 0;
				boolean isfull = false;
				String proto = null;
				
				IPendingServiceCall pc = (IPendingServiceCall) call;
				
				StatusObject result = getStatus(StatusCodes.NC_CONNECT_REJECTED);
				
				ResolveResult cfg = null;
				
				final List<RTMPTypes> sessionProtocols = new ArrayList<RTMPTypes>(7);
				
				try {
					
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
				
				} catch (IOException e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, alresult.getHost(), e);
					throw new ClientRejectedException("The Domain Has Not Properly Configured For LoadBalancers");
				}
				catch (Exception e) {
					
					log.error("EXCEPTION GETTING BEST SERVER - REQUEST SROURCE <{}> - HOST <{}> - ",clientip, alresult.getHost(), e);
					throw new ClientRejectedException("Could Not Find Proper Node To Answer");
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
							isfull,alresult.getHost(),alresult.getPath(),apikey,alresult.getQuerySring(),clientip,cfg.getFwdPort(),cfg.getFwdProto());
				}
			
				final String fwdPort = new StringBuilder().append(":").append(cfg.getFwdPort()).toString();
				
				if(cfg.getFwdProto() == null) {
					
					if(session.getAttribute(RTMPConnection.RTMPE_CIPHER_IN) != null) {
						
						sessionProtocols.add(RTMPTypes.RTMPE);
					}
					
					Object flag = session.getAttribute(RTMPTMinaFilter.RTMPT_FLAG);
					
					if(flag != null && ((Boolean)flag)) {
						
						sessionProtocols.add(RTMPTypes.RTMPT);
					}

					cfg.setFwdProto(RTMPTypes.getFinalProto(sessionProtocols).getType());
				}
				
				final String redirect = new StringBuilder()
					.append(cfg.getFwdProto())
					.append("://")
					.append(cfg.getServer())
					.append(fwdPort)
					.append("/")
					.append(alresult.getPath())
					.append(alresult.getQuerySring()).toString();
				
				log.debug("Redirecting request to {}",redirect);
				
				final ObjectMap<String,String> extra = new ObjectMap<String, String>();
				extra.put("code","302");
				extra.put("redirect",redirect);
				result.setAdditional("ex",extra);
				pc.setResult(result);
				
				return true;
			}
		}
		
		return false;
	}
	
	private StatusObject getStatus(String code) {
		return statusObjectService.getStatusObject(code);
	}
}
