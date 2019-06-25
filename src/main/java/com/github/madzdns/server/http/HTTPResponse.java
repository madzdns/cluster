package com.github.madzdns.server.http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Date;

import org.apache.mina.core.buffer.IoBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPResponse extends HTTPMessage{
	
	
	private static Logger log = LoggerFactory.getLogger(HTTPMessage.class);
	
	HTTPCode code = HTTPCode.Temporary_Redirect;
	
	public HTTPResponse() {}
	
	public HTTPCode getCode() {
		
		return code;
	}
	
	public void setCode(HTTPCode code) {
		
		this.code = code;
	}
	
	public Type getType() {
		
		return Type.TypeResponse;
	}
	
	@Override
	public String toString() {

		try {
			
			return new String(toByteArray());
		}
		catch(Exception e) {}
		return "";
	}
	
	public void setCommonHeaders() {
		
		setHeader("Server",serverSignature);
		setHeader("Date",new Date().toString());
	}
	
	public byte[] toByteArray() throws Exception {
		
		try {
			
			int initSize = 0;
			String headers = getHeadersString();
			
			initSize = headers.getBytes().length + getBufferSize();
			
			ByteArrayOutputStream bb = new ByteArrayOutputStream(initSize);
			
			DataOutputStream sb = new DataOutputStream(bb);
			
			sb.write("HTTP/1.1 ".getBytes());
			sb.write(String.valueOf(code.value()).getBytes());
			sb.write(" ".getBytes());
			sb.write(code.description().getBytes());
			sb.write(CRLF.getBytes());	
			sb.write(headers.getBytes());
			sb.write("Content-Length: ".getBytes());
			sb.write(String.valueOf(getBufferSize()).getBytes());
			sb.write(CRLF.getBytes());
			sb.write(CRLF.getBytes());

			if (getBufferSize() > 0) {
				
				sb.write(getBuffer());
			}
			
			return bb.toByteArray();
			
		} catch (Exception e) {
			
			log.error("Failed to get message to byteArray", e);
			throw e;
		}
	}
	
	public IoBuffer toByteBuffer() throws Exception {
		
		try {
			
			IoBuffer buffer = IoBuffer.wrap(toByteArray());

			return buffer;
			
		} catch (Exception e) {
			
			log.error("Failed to serialize message to bytebuffer", e);
			throw e;
		}
	}
}
