package com.github.madzdns.cluster.http.web;

import java.net.InetSocketAddress;
import java.util.Properties;

public abstract class WebController {
	
	private InetSocketAddress remote;
	
	private InetSocketAddress local;
	
	private Properties cookies;

	public InetSocketAddress getRemote() {
		return remote;
	}

	public void setRemote(InetSocketAddress remote) {
		this.remote = remote;
	}

	public InetSocketAddress getLocal() {
		return local;
	}

	public void setLocal(InetSocketAddress local) {
		this.local = local;
	}

	public Properties getCookies() {
		return cookies;
	}

	public void setCookies(Properties cookies) {
		this.cookies = cookies;
	}
	
	public String getCookie(String key) {
		
		return cookies.getProperty(key);
	}
}
