package com.github.madzdns.server.http.codec;


import com.github.madzdns.server.http.HTTPCode;
import com.github.madzdns.server.http.HTTPMessage;
import com.github.madzdns.server.http.HTTPRequest;
import com.github.madzdns.server.http.HTTPResponse;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.rtmpt.RTMPTConnectionMinaFilter;

public class HttpMinaDecoder implements ProtocolDecoder{
	
	public enum ReadState {
		/** Unrecoverable error occurred */
		Failed,
		/** Trying to resync */
		Sync,
		/** Waiting for a command */
		Ready,
		/** Reading interleaved packet */
		Packet,
		/** Reading command (request or command line) */
		Command,
		/** Reading headers */
		Header,
		/** Reading body (entity) */
		Body,
		/** Fully formed message */
		Dispatch
	}
	
	private static Logger log = LoggerFactory.getLogger(HttpMinaDecoder.class);
	private static final Pattern httpRequestPattern = Pattern
			.compile("([A-Z_]+) ([^ ]+) HTTP/[0-9\\.]+");

	private static final Pattern httpResponsePattern = Pattern
			.compile("HTTP/[0-9\\.]+ ([0-9]+) .+");
	private static final Pattern httpHeaderPattern = Pattern
			.compile("([a-zA-Z\\-]+[0-9]?):\\s?(.*)");

	@Override
	public void decode(IoSession session, IoBuffer in,
			ProtocolDecoderOutput out) throws ProtocolDecoderException {
		
		BufferedReader reader = null;
		
		int bytesRead = in.position();
		
		try {
			
			reader = new BufferedReader(new InputStreamReader(in
					.asInputStream(), "US-ASCII"));
			
		} catch (UnsupportedEncodingException e1) {}

		// Retrieve status from session
		ReadState state = (ReadState) session.getAttribute("state");
		if (state == null)
			state = ReadState.Command;
		
		HTTPMessage httpMessage = (HTTPMessage) session
				.getAttribute("httpMessage");
		
		try {
			
			while (true) {
				
				if (state != ReadState.Command && state != ReadState.Header)
					// the "while" loop is only used to read commands and
					// headers
					break;
				
				String line = reader.readLine();
				
				if (line == null)
					// there's no more data in the buffer
					break;

				if (line.length() == 0) {
					// This is the empty line that marks the end
					// of the headers section
					
					//CRLF
					bytesRead += 2;
					
					state = ReadState.Body;
					break;
				}
				
				bytesRead += (line.getBytes().length +2);
				
				switch (state) {
				
					case Command:
						
						if (line.startsWith("HTTP")) {
							
							Matcher m = httpResponsePattern.matcher(line);
							
							if (!m.matches())
								throw new ProtocolDecoderException(
										"Malformed response line: " + line);

							HTTPCode code = HTTPCode.fromString(m.group(1));
							httpMessage = new HTTPResponse();
							((HTTPResponse)(httpMessage)).setCode(code);

						} else {
							/*log.info("session {}, LIIIIIIIIIIIIIINE {}", session.getId(), line);*/
							// this is a HTTP request
							Matcher m = httpRequestPattern.matcher(line);
							
							if (!m.matches()) {
								
								throw new ProtocolDecoderException(
										"Malformed request line for session: "+ session.getId() + "," + line);
							}

							String method = m.group(1);
							String strUrl = m.group(2);
							httpMessage = new HTTPRequest();
							((HTTPRequest)httpMessage).setUrl(strUrl);
							((HTTPRequest)httpMessage).setMethod(HTTPMessage.Method.fromString(method));
						}
						state = ReadState.Header;
						break;

					case Header:
						// this is a header
						Matcher m = httpHeaderPattern.matcher(line);

						if (!m.matches())
							throw new ProtocolDecoderException(
									"HTTP header not valid");

						httpMessage.setHeader(m.group(1), m.group(2));
						
						if("cookie".equals(m.group(1).toLowerCase())) {
							
							httpMessage.setCookieStr(m.group(2));
							
							String cstr = new StringBuilder("; ").append(m.group(2)).toString();
							
							String[] cookieArr = cstr.split("; ");

							if(cookieArr.length > 0) {
								
								for(int i = 0; i< cookieArr.length; i++) {
									
									String cval = cookieArr[i];
									
									String[] cvalArr = cval.split("=");

									if(cvalArr.length > 1) {

										httpMessage.setCookie(cvalArr[0], cvalArr[1]);
									}
								}
							}
						}
						
						break;
				default:
					break;

				}
			}

			if (state == ReadState.Body) {
				// Read the message body
				String len = httpMessage.getHeader(
						"Content-Length");
				
				int bufferLen = 0;
				
				if(len == null) {
					
					len = httpMessage.getHeader(
							"Content-length");
				}
				
				if(len != null && !len.equals("")) {
					
					try {
						
						bufferLen = Integer.parseInt(len);
						
					} catch (Exception e) {
					}
				}
						
				
				if (bufferLen == 0) {
					// there's no buffer to be read
					state = ReadState.Dispatch;

				} else {
					// we have a content buffer to read
					int bytesToRead = bufferLen - httpMessage.getBufferSize();
					
					if(bytesToRead  > 0) {
						
						if(bytesRead>0) {
						
							in.flip();
							in.position(bytesRead);
						}
						
						if(in.remaining() >= bytesToRead) {
							
							// read the content buffer
							byte[] bufferContent = new byte[bytesToRead];
							in.get(bufferContent);
							
							httpMessage.appendToBuffer(bufferContent);	
						}
						else if(in.remaining()>0) {
							
							byte[] bufferContent = new byte[in.remaining()];
							in.get(bufferContent);
							
							httpMessage.appendToBuffer(bufferContent);
						}
					}
					
					if (httpMessage.getBufferSize() >= bufferLen) {
						
						// The HTTP message parsing is completed
						state = ReadState.Dispatch;
					}
				}
			}
		} catch (Exception e) {
			/*
			 * error on input stream should not happen since the input stream is
			 * coming from a bytebuffer.
			 */
			
			if(!session.containsAttribute(RTMPTConnectionMinaFilter.RTMPT_MINAFILTER_TESTING)) {
			
				log.error("", e);
			}
			else {
				
				log.error("Checking RTMPT {}", e.getMessage());
			}
			
			throw new ProtocolDecoderException(e.getMessage());

		} finally {
			
			try {
				
				reader.close();
			} catch (Exception e) {}
		}

		if (state == ReadState.Dispatch) {
			// The message is already formed
			// send it
			session.removeAttribute("state");
			session.removeAttribute("httpMessage");
			
			out.write(httpMessage);
			return;
		}

		// log.debug( "INCOMPLETE MESSAGE \n" + httpMessage );

		// Save attributes in session
		session.setAttribute("state", state);
		session.setAttribute("httpMessage", httpMessage);
	}

	@Override
	public void dispose(IoSession arg0) throws Exception {
		
	}

	@Override
	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
			throws Exception {
		
	}

}
