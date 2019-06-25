package rtmpt;

import http.HTTPCode;
import http.HTTPRequest;
import http.HTTPResponse;
import http.codec.HttpMinaDecoder;
import http.codec.HttpMinaEncoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.red5.server.net.rtmpe.RTMPEIoFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rtmp.handler.RTMPRedirectMinaHandler;
import server.codec.ServerCodecFactory;
import server.config.Config;

public class RTMPTConnectionMinaFilter extends IoFilterAdapter {

	
	private final Logger log = LoggerFactory.getLogger(RTMPTConnectionMinaFilter.class);
	
	public static final String RTMPT_HEADER = "application/x-fcs";
	
	public static final String RTMPT_CONNECTION_MANAGER = RTMPTConnectionMinaFilter.class.getName() + "RTMPT_CONNECTION_MANAGER";
	
	public static final String RTMPT_MINAFILTER = RTMPTConnectionMinaFilter.class.getName() + "RTMPT_MINAFILTER";
	
	public static final String RTMPT_MINAFILTER_TESTING = RTMPTConnectionMinaFilter.class.getName() + "RTMPT_MINAFILTER_TESTING";
	
	public final static String ident = new StringBuffer("<fcs><Company>").append(Config.getApp().getName())
			.append("</Company><Team>").append(Config.getApp().getName()).append("</Team></fcs>").toString();
			
	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session,
			Object message) throws Exception {
		
		if(message instanceof HTTPRequest) {
			
			//THEN it is comming fom a HTTP decoder, check to see if rtmpt headers are set
			//otherwise its a pure http message
			
			final HTTPRequest request = (HTTPRequest)message;
			final HTTPResponse response = new HTTPResponse();
			String contentType = request.getHeader("Content-Type");
			
			if(contentType == null || !contentType.equals(RTMPT_HEADER)) {
				
				contentType = request.getHeader("Content-type");
				
				if(contentType == null || !contentType.equals(RTMPT_HEADER)) {
				
					log.debug("RTMPT HEADER WAS NOT SET");
					
					//no rtmpt filter anymore 
					session.getFilterChain().remove(RTMPT_MINAFILTER);
					nextFilter.messageReceived(session, message);
					
					return;
				}
			}
			
			final String pathString = request.getUrl();
			
			if(pathString == null) {
				
				//Despite that RTMPT headers was set, but path info was incorrect.
				//So we treating it as pure HTTP

				//no rtmpt filter anymore 
				session.getFilterChain().remove(RTMPT_MINAFILTER);
				nextFilter.messageReceived(session, message);
				
				return;
			}
			
			final char path = pathString.charAt(1);
			
			if(path == RTMPTRequestPath.FCS_IDENT.getPath()) {

				if(!pathString.endsWith("2")) {
					
					//it should be a 404 not found
					response.setCode(HTTPCode.NotFound);
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.setCommonHeaders();
					
					nextFilter.filterWrite(session, new DefaultWriteRequest(response));
					
					return;
				}
				
				//It should be a 404 not found
				response.setCode(HTTPCode.NotFound);
				response.setHeader("Connection", "Keep-Alive");
				response.setHeader("Cache-Control", "no-cache");
				response.setCommonHeaders();
				response.appendToBuffer(ident);
				
				nextFilter.filterWrite(session, new DefaultWriteRequest(response));
				return;
			}
			
			RTMPTConnactionManager connectionManager = (RTMPTConnactionManager) session.getAttribute(RTMPT_CONNECTION_MANAGER);
			
			if(connectionManager == null) {
				
				connectionManager = new RTMPTConnactionManager();
				
				session.setAttribute(RTMPT_CONNECTION_MANAGER, connectionManager);
			}
			
			if(path == RTMPTRequestPath.OPEN1.getPath()) {
				
				byte[] sessionId = connectionManager.handleOpen(session, request.getBufferSize());
				
				String sessId = null;
				
				if(sessionId.length > 1) {
					
					sessId = new String(sessionId);
				}
				
				if(sessId != null) {
					
					response.setCode(HTTPCode.OK);
					response.setHeader("Content-Type", RTMPT_HEADER);
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.setCommonHeaders();
					sessId = new StringBuilder(sessId).append("\n").toString();
					response.appendToBuffer(sessId);
					
					nextFilter.filterWrite(session, new DefaultWriteRequest(response));
					return;
				}
				else {
					
					response.setCode(HTTPCode.InternalServerError);
					response.setCommonHeaders();
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.appendToBuffer(sessionId);
					nextFilter.filterWrite(session, new DefaultWriteRequest(response));
					nextFilter.filterClose(session);
					return;
				}	
			}
			
			if(path == RTMPTRequestPath.CLOSE.getPath()) {
				
				if(connectionManager.handleClose()) {
					
					response.setCode(HTTPCode.OK);	
					response.appendToBuffer((byte)0);
				}
				else {
					
					response.setCode(HTTPCode.BadRequest);
					
					response.appendToBuffer(connectionManager.handleBadRequest(
							String.format("Close: unknown client for session: %s", 
							connectionManager.getSessionId())));
				}
				
				response.setCommonHeaders();
				response.setHeader("Connection", "Keep-Alive");
				response.setHeader("Cache-Control", "no-cache");
				
				nextFilter.filterWrite(session, new DefaultWriteRequest(response));
				nextFilter.filterClose(session);
				return;
			}
			
			final String sessId = connectionManager.getSessionId();
			
			if(sessId != null) {
				
				if (path == RTMPTRequestPath.IDLE.getPath()) {
					
					if(pathString.indexOf(sessId) != -1) {
						//no matter what is the sequence	
						//start of idle
						
						byte[] result = connectionManager.handleIdle(request.getBufferSize());
						
						if(result == null) {
							
							response.setCode(HTTPCode.BadRequest);
							response.appendToBuffer(connectionManager.handleBadRequest(
									String.format("Idle: unknown client session: %s",
									connectionManager.getSessionId())));
						}
						else {
							
							response.setCode(HTTPCode.OK);	
							response.appendToBuffer(result);
						}
						
						response.setHeader("Connection", "Keep-Alive");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("Content-Type", RTMPT_HEADER);
						response.setCommonHeaders();

						nextFilter.filterWrite(session, new DefaultWriteRequest(response));
						return;
					}
				}
				else if(path == RTMPTRequestPath.SEND.getPath()) {
					
					byte[] data = request.getBuffer();
					
					if(data != null && data.length > 0) {
					
						byte[] result = connectionManager.handleSend(request.getBufferSize(), data);
						
						if(result == null) {
							
							response.setCode(HTTPCode.BadRequest);
							response.appendToBuffer(connectionManager.handleBadRequest(
									String.format("Send: unknown client session: %s",
									connectionManager.getSessionId())));
						}
						else {
							
							response.setCode(HTTPCode.OK);	
							response.appendToBuffer(result);
						}
						
						response.setHeader("Connection", "Keep-Alive");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("Content-Type", RTMPT_HEADER);
						response.setCommonHeaders();

						nextFilter.filterWrite(session, new DefaultWriteRequest(response));
						return;	
					}
				}
			}
			
			//SOMETHINGs wrong
			response.setCode(HTTPCode.BadRequest);
			response.appendToBuffer(connectionManager.handleBadRequest(
					String.format("Unknown request for session: %s",
					connectionManager.getSessionId())));
			response.setCommonHeaders();
			response.setHeader("Connection", "Keep-Alive");
			response.setHeader("Cache-Control", "no-cache");
			
			nextFilter.filterWrite(session, new DefaultWriteRequest(response));
			nextFilter.filterClose(session);
			return;
		}
		
		
		final IoBuffer buffer = (IoBuffer) message;
		IoBuffer cpy = IoBuffer.allocate(buffer.remaining());
		IoBuffer tmp = buffer.duplicate();
		tmp.limit (buffer.position() + buffer.limit());
		
		cpy.put(tmp);
		cpy.flip();
		
		final HttpMinaDecoder decoder = new HttpMinaDecoder();
		
		final RTMPTProtocolDecoderOutput decoderOut = new RTMPTProtocolDecoderOutput(this);
		
		try {

			session.setAttribute(RTMPT_MINAFILTER_TESTING, Boolean.TRUE);
			
			decoder.decode(session, cpy, decoderOut);
			
			session.getFilterChain().addBefore(RTMPTConnectionMinaFilter.RTMPT_MINAFILTER, "http_coder", 
					new ProtocolCodecFilter(new ServerCodecFactory(new HttpMinaDecoder(),new HttpMinaEncoder())));
			
			buffer.free();
			
			decoderOut.flush(nextFilter, session);
		}
		catch (Exception e) {
			
			cpy.free();
			
			session.getFilterChain().addAfter(RTMPTConnectionMinaFilter.RTMPT_MINAFILTER, "rtmpeFilter", 
					new RTMPEIoFilter());
			
			session.getFilterChain().remove(RTMPT_MINAFILTER);
			
			RTMPRedirectMinaHandler rtmptHandler = (RTMPRedirectMinaHandler) session.getHandler();
			
			rtmptHandler.afterSessionCreated(session);
			rtmptHandler.baseSessionOpend(session);

			nextFilter.messageReceived(session, message);
		}
	}
}
