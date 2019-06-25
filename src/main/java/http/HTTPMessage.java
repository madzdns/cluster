package http;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.api.FRfra;

public abstract class HTTPMessage {
	
	private static Logger log = LoggerFactory.getLogger(HTTPMessage.class);
	
	Properties headers = new Properties();
	
	Properties cookies = new Properties();
	
	String cookieStr = null;
	
	public enum Type {
		/** Generic message (internal use) */
		TypeNone,
		/** Request message */
		TypeRequest,
		/** Response message */
		TypeResponse
	};
	
	public enum Method {
		
		GET,POST,PUT,DELETE,NON,HEAD;
		
		public static Method fromString(String method) {
			for (Method code : Method.values()) {
				if (code.name().equals(method))
					return code;
			}
			return Method.NON;
		}
	}
	// CRLF
	public static final String CRLF = "\r\n";
	
	private ByteArrayOutputStream baos = new ByteArrayOutputStream();
	private DataOutputStream buffer = new DataOutputStream(baos);
	
	protected final static String serverSignature;

	static {
		
		serverSignature = FRfra.getServerVersion() + FRfra.getSystemVersion();
		log.info("HTTP signature set to: {}", serverSignature);
	}
	
	public Type getType() {
		
		return Type.TypeNone;
	}
	
	public String getHeader(String key) {
		
		return headers.getProperty(key);
	}
	
	public void removeHeader(String key) {
		
		headers.remove(key);
	}
	
	public void setHeader(String key,String value) {
		
		this.headers.put(key, value);
	}
	
	public void setHeaders(Properties headers) {
		
		this.headers = headers;
	}
	
	public void appendToBuffer(byte[] other) throws IOException {
		
		if(other == null) {
			
			return;
		}
			
		this.buffer.write(other);
	}
	
	public void appendToBuffer(String other) throws IOException {
		
		if(other == null) {
			
			return;
		}
			
		this.buffer.write(other.getBytes());
	}
	
	public void appendToBuffer(CharBuffer other) throws IOException {
		
		if(other == null) {
			
			return;
		}
		
		appendToBuffer(other.toString());
	}
	
	public void appendToBuffer(byte other) throws IOException {
		
		this.buffer.writeByte(other);
	}
	
	public void appendToBuffer(int other) throws IOException {
		
		this.buffer.writeInt(other);
	}
	
	public void setBuffer(String other) throws IOException {
		
		if(other == null) {
			
			return;
		}
		
		this.baos = new ByteArrayOutputStream(other.length());
		this.buffer = new DataOutputStream(baos);
		
		appendToBuffer(other);
	}
	
	public void setBuffer(CharBuffer other) throws IOException {
		
		this.baos = new ByteArrayOutputStream(other.length());
		this.buffer = new DataOutputStream(baos);
		
		appendToBuffer(other);
	}
	
	public Properties getHeaders() {
		
		return headers;
	}
	
	public String getHeadersString() {
		
		StringBuilder buf = new StringBuilder();
		
		for (Object key : headers.keySet()) {
			
			String value = headers.getProperty((String) key);
			buf.append(new StringBuilder().append(key).append(": ").append(value).append(CRLF).toString());
		}
		
		return buf.toString();
	}
	
	public byte[] getBuffer() {
		
		return baos.toByteArray();
	}
	
	public StringBuffer getBufferAsStringBuffer() {
		
		return new StringBuffer(new String(baos.toByteArray()));
	}
	
	public int getBufferSize() {
		
		return baos.size();
	}
	
	public void setCookie(String key, String value) {
		
		cookies.put(key, value);
	}
	
	public String getCookie(String key) {
		
		return cookies.getProperty(key);
	}

	public String getCookieStr() {
		return cookieStr;
	}

	public void setCookieStr(String cookieStr) {
		this.cookieStr = cookieStr;
	}

}
