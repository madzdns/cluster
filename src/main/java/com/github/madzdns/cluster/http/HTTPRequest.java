package com.github.madzdns.cluster.http;

public class HTTPRequest extends HTTPMessage{
	
	String host = "";
	String url = "";
	Method method = Method.GET;
	
	public Method getMethod() {
		
		return method;
	}
	
	public void setMethod(Method method) {
		
		this.method = method;
	}
	
	@Override
	public String toString() {
		
		StringBuffer sb = new StringBuffer();
		sb.append(method);
		return sb.toString();
	}
	
	public String getHost() {
		
		if(host.equals(""))
			this.host = getHeader("Host");
		return host=host==null?"":host;
	}
	
	public void setHost(String host) {
		
		this.host = host;
		setHeader("Host",host);
	}
	
	public String getUrl() {
		
		return url;
	}
	
	public void setUrl(String url) {
		
		this.url = url;
	}
}
