package server.core.http;

import java.util.Properties;

public class ProxyResponse {

	Properties headers = new Properties();
	String content;
	int responseCode = 200;
	
	public void setHeader(String key, String value) {
		
		headers.setProperty(key, value);
	}
	
	public String getHeader(String key) {
		
		return headers.getProperty(key);
	}
	
	public Properties getHeaders() {
		
		return headers;
	}

	public String getContent() {
		
		return content;
	}

	public void setContent(String content) {
		
		this.content = content;
	}

	public int getResponseCode() {
		
		return responseCode;
	}

	public void setResponseCode(int responseCode) {
		this.responseCode = responseCode;
	}
}
