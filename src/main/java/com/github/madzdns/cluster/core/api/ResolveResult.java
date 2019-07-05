package com.github.madzdns.cluster.core.api;

import java.util.List;

import com.github.madzdns.cluster.core.config.ResolveHint;

public class ResolveResult {

	private List<ResolveHint> hints = null;
	
	private String[] servers = null;
	
	private short fwdPort;
	
	private String fwdProto;
	
	private boolean withProxy = false;

	public ResolveResult(List<ResolveHint> hints, String server, boolean withProxy) {

		this.hints = hints;
		this.servers = new String[] {server};
		this.withProxy = withProxy;
	}
	
	public ResolveResult(List<ResolveHint> hints, String[] servers, boolean withProxy) {

		this.hints = hints;
		this.servers = servers;
		this.withProxy = withProxy;
	}

	public List<ResolveHint> getHints() {
		
		return hints;
	}

	public String getServer() {
		
		return servers[0];
	}
	
	public String[] getServers() {
		
		return servers;
	}

	public short getFwdPort() {
		
		return fwdPort;
	}

	public void setFwdPort(short fwdPort) {
		
		this.fwdPort = fwdPort;
	}

	public String getFwdProto() {
		
		return fwdProto;
	}

	public void setFwdProto(String fwdProto) {
		
		this.fwdProto = fwdProto;
	}

	public boolean isWithProxy() {
		
		return withProxy;
	}

	public void setWithProxy(boolean withProxy) {
		
		this.withProxy = withProxy;
	}
	
}
