package com.github.madzdns.cluster.rtmpt;

import com.github.madzdns.cluster.http.HTTPCode;
import com.github.madzdns.cluster.http.HTTPMessage;
import com.github.madzdns.cluster.http.HTTPRequest;
import com.github.madzdns.cluster.http.HTTPResponse;
import com.github.madzdns.cluster.http.codec.HttpMinaDecoder;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.DefaultWriteRequest;
import org.apache.mina.core.write.WriteRequest;
import org.red5.server.net.rtmp.RTMPConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.github.madzdns.cluster.rtmp.RtmpServer;
import com.github.madzdns.cluster.rtmp.handler.RTMPRedirectMinaHandler;
import com.github.madzdns.cluster.rtmpt.handler.RTMPTRedirectMinaHandler;
import com.github.madzdns.cluster.core.config.Config;

/**
 * Note the class fields should be safe because we instantiate RTMPTMinaFilter
 * per session where a session just created
 * @author pendrive
 *
 */

public class RTMPTMinaFilter extends IoFilterAdapter {
	
	public static final String RTMPT_FLAG = RTMPTMinaFilter.class.getName()+ "rtmptthisIs#22"; 
	
	private static final String RTMPT_HEADER = "application/core-fcs";
	
	//check to see if rtmp session is already created
	private static final String FROM_RTMP = RTMPTMinaFilter.class.getName()+"_FOR_RTMP";
	
	public static final String RTMP_MIN_HANDLER = RTMPTMinaFilter.class.getName()+ "RTMP_HANDLER";
	
	public static final String RTMPT_MINAFILTER = RTMPTMinaFilter.class.getName() + "RTMPT_MINAFILTER";
	
	public static final byte DEFAULT_POLLING = 1;
	
	private final Logger log = LoggerFactory.getLogger(RTMPTMinaFilter.class);
	
	public static final String ORIGINAL_MSGFORRTMP = RTMPTMinaFilter.class.getName() + "ORIGINAL_MSGFORRTMP";
	
	private final static String ident = new StringBuffer("<fcs><Company>").append(Config.getApp().getName())
			.append("</Company><Team>").append(Config.getApp().getName()).append("</Team></fcs>").toString();
	
	protected void write(final NextFilter nextFilter, final IoSession session, final Object message) {
		
		Boolean fromRtmp = (Boolean)session.getAttribute(FROM_RTMP);
		if(fromRtmp!= null && fromRtmp) {
			
			if(message instanceof HTTPResponse) {
				
				final HTTPResponse responce = (HTTPResponse) message;
				
				try {
					
					//since it is directly from rtmp port, we pass content to next filter 
					// that should be a rtmp or rtmpe filter
					nextFilter.filterWrite(session, new DefaultWriteRequest(responce.toByteBuffer()));
					return;
				} catch (Exception e) {
					
					log.error("",e);
					return;
				}
			}
		}
		
		//else use the http write
		nextFilter.filterWrite(session, new DefaultWriteRequest(message));
	}

	@Override
	public void sessionCreated(NextFilter nextFilter, IoSession session)
			throws Exception {

		nextFilter.sessionCreated(session);
	}
	
	@Override
	public void messageReceived(NextFilter nextFilter, IoSession session,
			Object message) throws Exception {

		final Boolean rtmptState = (Boolean) session.getAttribute(RTMPT_FLAG);
		
		/*log.info("rtmptState {}, message instanceof HTTPRequest {}, session {}", 
				rtmptState, message instanceof HTTPRequest, session.getId());*/
		
		if(rtmptState != null && !rtmptState) {
			
			nextFilter.messageReceived(session, message);
		}
		
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
					session.setAttribute(RTMPT_FLAG, Boolean.FALSE);
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
				
				session.setAttribute(RTMPT_FLAG, Boolean.FALSE);
				//no rtmpt filter anymore 
				session.getFilterChain().remove(RTMPT_MINAFILTER);
				nextFilter.messageReceived(session, message);
				
				return;
			}
			
			if( rtmptState == null) {
				
				//we mark session as rtmpt
				session.setAttribute(RTMPT_FLAG, Boolean.TRUE);
			}
			
			final char path = pathString.charAt(1);
			
			if(path == RTMPTRequestPath.FCS_IDENT.getPath()) {

				if(!pathString.endsWith("2")) {
					
					//it should be a 404 not found
					response.setCode(HTTPCode.NotFound);
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.setCommonHeaders();
					
					// no need to pass to rtmp, we directly answer in http
					write(nextFilter, session, response);
					
					return;
				}
				
				//It should be a 404 not found
				response.setCode(HTTPCode.NotFound);
				response.setHeader("Connection", "Keep-Alive");
				response.setHeader("Cache-Control", "no-cache");
				response.setCommonHeaders();
				response.appendToBuffer(ident);
				
				// no need to pass to rtmp, we directly answer in http
				write(nextFilter, session, response);
				return;
			}
			
			if(path == RTMPTRequestPath.OPEN1.getPath()) {
				
				Boolean fromRtmp = (Boolean)session.getAttribute(FROM_RTMP);
				
				if(fromRtmp == null || !fromRtmp) {
					
					//If it is not directly from RTMP, then we must initialize rtmp session.
					//This way proper RTMP filters will be added to session.
					
					final RTMPTRedirectMinaHandler rtmptMinaHandler = RtmpServer.CTX.getBean("red5.core", ApplicationContext.class)
							.getBean("rtmptMinMinaIoHandler",RTMPTRedirectMinaHandler.class);
					rtmptMinaHandler.sessionCreated(session);
					
					//opennning a rtmp connection
					rtmptMinaHandler.sessionOpened(session);
					
					//storing true handler to use later
					session.setAttribute(RTMP_MIN_HANDLER, rtmptMinaHandler);
				}
				else {
					
					session.setAttribute(RTMP_MIN_HANDLER, session.getHandler());
				}
				
				String sessId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
				
				if(sessId != null) {
					
					response.setCode(HTTPCode.OK);
					response.setHeader("Content-Type", RTMPT_HEADER);
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.setCommonHeaders();
					sessId = new StringBuilder(sessId).append("\n").toString();
					response.appendToBuffer(sessId);
					
					//At this point we only need sessionId from rtmp
					//then write using http because as this point we sure
					//rtmp and rtmpe filters have not been added yet
					write(nextFilter, session, response);
					return;
				}
				else {
					
					response.setCode(HTTPCode.NotFound);
					response.setCommonHeaders();
					response.setHeader("Connection", "Keep-Alive");
					response.setHeader("Cache-Control", "no-cache");
					response.appendToBuffer((byte)48); //48 means 0 ascii
					write(nextFilter, session, response);
					nextFilter.filterClose(session);
					return;
				}	
			}
			
			if(path == RTMPTRequestPath.CLOSE.getPath()) {
				
				response.setCode(HTTPCode.OK);
				response.setCommonHeaders();
				response.setHeader("Connection", "Keep-Alive");
				response.setHeader("Cache-Control", "no-cache");
				response.appendToBuffer((byte)48); //48 means 0 ascii
				write(nextFilter, session, response);
				nextFilter.filterClose(session);
				return;
			}
			
			final String sessId = (String) session.getAttribute(RTMPConnection.RTMP_SESSION_ID);
			
			if(sessId != null) {
				
				if (path == RTMPTRequestPath.IDLE.getPath()) {
					
					if(pathString.indexOf(sessId) != -1) {
						//no matter what is the sequence	
						//start of idle
						
						response.setCode(HTTPCode.OK);
						response.setHeader("Connection", "Keep-Alive");
						response.setHeader("Cache-Control", "no-cache");
						response.setHeader("Content-Type", RTMPT_HEADER);
						response.setCommonHeaders();
						//BECAUSE we are just a load balancer, I put polling delay 1 every time
						response.appendToBuffer(DEFAULT_POLLING);
						write(nextFilter, session, response);
						return;
					}
				}
				else if(path == RTMPTRequestPath.SEND.getPath()) {
					
					final RTMPRedirectMinaHandler handler = (RTMPRedirectMinaHandler) session.getAttribute(RTMP_MIN_HANDLER);
					
					log.info("ASKED FOR SEND, handler is : {}", handler);
					
					if(handler != null) {
						
						byte[] data = request.getBuffer();
						
						log.info("data length is : {}", data.length);
						
						if(data != null && data.length > 0) {
							
							IoBuffer dataBuffer = IoBuffer.wrap(data);
							
							//log.info("dataBuffer {}", dataBuffer.getHexDump());
							
							nextFilter.messageReceived(session, dataBuffer);	
						}
						
						return;
					}
				}
			}
			
			//SOMETHINGs wrong
			response.setCode(HTTPCode.NotFound);
			response.setCommonHeaders();
			response.setHeader("Connection", "Keep-Alive");
			response.setHeader("Cache-Control", "no-cache");
			response.appendToBuffer((byte)48); //48 means 0 ascii
			write(nextFilter, session, response);
			nextFilter.filterClose(session);
			return;
		}
		
		if(message instanceof IoBuffer) {
			
			//try to see if rtmpt is requested directly in rtmp port
			/*log.info("trying to see if rtmpt directly requested in rtmp port for session {}", session.getId());*/
			
			final IoBuffer buffer = (IoBuffer) message;
			
			final HttpMinaDecoder decoder = new HttpMinaDecoder();
			
			final RTMPTProtocolDecoderOutput decoderOut = new RTMPTProtocolDecoderOutput(this);
			
			try {
				
				session.setAttribute(ORIGINAL_MSGFORRTMP, buffer);
				
				decoder.decode(session, buffer, decoderOut);

				session.setAttribute(FROM_RTMP, Boolean.TRUE);
				
				decoderOut.flush(nextFilter, session);
			}
			catch (Exception e) {
				
				//log.error("",e);
				//it is rtmp or rtmpe
				/*log.info("flip buffer to use as rtmp input for {}, session.getId() {}", nextFilter, session.getId());*/
				
				buffer.flip();
				
				//no need to rtmpt filter anymore 
				//session.getFilterChain().remove(RTMPT_MINAFILTER);

				nextFilter.messageReceived(session, buffer);
				
				if(buffer.hasRemaining()) {
					
					messageReceived(nextFilter, session, buffer);
				}
			}
		}
	}
	
	@Override
	public void filterWrite(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest) throws Exception {
		
		final Object message = writeRequest.getMessage();
		
		if(message instanceof HTTPResponse) {
			
			nextFilter.filterWrite(session, writeRequest);
			return;
		}
		
		final Boolean rtmptState = (Boolean) session.getAttribute(RTMPT_FLAG);
		
		if(rtmptState != null && !rtmptState) {
			
			nextFilter.filterWrite(session, writeRequest);
			return;
		}
		
		//then we assume its rtmp or rtmps or rtmpe, so just capsulate in http
		HTTPResponse response = new HTTPResponse();
		
		response.setCode(HTTPCode.OK);
		response.setHeader("Connection", "Keep-Alive");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Type", RTMPT_HEADER);
		
		if(session.containsAttribute("_ff_redirect")) {
			
			response.setHeader("Location", (String)session.getAttribute("_ff_redirect"));
		}
		
		response.setCommonHeaders();
		
		final IoBuffer buf = (IoBuffer) message;
		
		response.appendToBuffer(DEFAULT_POLLING);
		
		//log.info("buf.getHexDump() {}", buf.getHexDump());
		
		response.appendToBuffer(buf.array());
		
		//log.info("response buffer {}", IoBuffer.wrap(response.getBuffer()).getHexDump());
		
		Boolean fromRtmp = (Boolean) session.getAttribute(FROM_RTMP);
		
		if(fromRtmp != null && fromRtmp) {
			
			nextFilter.filterWrite(session, new DefaultWriteRequest(response.toByteBuffer(), writeRequest.getFuture(),
					writeRequest.getDestination()));
			return;
		}
		
		nextFilter.filterWrite(session, new DefaultWriteRequest(response, writeRequest.getFuture(),
				writeRequest.getDestination()));
	}
	
	@Override
	public void messageSent(NextFilter nextFilter, IoSession session,
			WriteRequest writeRequest) throws Exception {

		/*log.info("session.getId(): {}, writeRequest.getMessage() instanceof HTTPMessage {}", 
				session.getId(), writeRequest.getMessage() instanceof HTTPMessage);*/
		
		if(writeRequest.getMessage() instanceof HTTPMessage) {

			Boolean fromRtmp = (Boolean) session.getAttribute(FROM_RTMP);
			
			if(fromRtmp != null && fromRtmp) {

				nextFilter.messageSent(session, new DefaultWriteRequest(session.getAttribute(ORIGINAL_MSGFORRTMP), writeRequest.getFuture(),
						writeRequest.getDestination()));
				
				session.setAttribute(ORIGINAL_MSGFORRTMP, null);
				return;
			}
			
			nextFilter.messageSent(session, writeRequest);
			return;
		}
		
		super.messageSent(nextFilter, session, writeRequest);
	}
}
