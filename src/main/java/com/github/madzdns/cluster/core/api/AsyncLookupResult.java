package com.github.madzdns.cluster.core.api;

import java.util.Date;

public class AsyncLookupResult {

	private LookupResult result = null;
	
	private String host;
	private String path;
	private String querySring;
	
	private long waitTime;
	
	private long startTime = 0;
	
	private boolean ready = false;
	
	public boolean isResultReady() {
		
		if(waitTime > -1 && new Date().getTime() - startTime >= waitTime) {
			
			return true;
		}

		return ready;
	}
	
	public AsyncLookupResult(String host, String path, String queryString, long waitTime) {
		
		this.host = host;
		this.path = path;
		this.querySring = queryString;
		
		this.waitTime = waitTime;
		
		startTime = new Date().getTime();
	}

	public LookupResult getResult() {
		
		return result;
	}

	public void setResult(LookupResult result) {
		
		ready = true;
		
		this.result = result;
	}
	
	public String getHost() {
		
		return host;
	}

	public String getPath() {
		return path;
	}

	public String getQuerySring() {
		return querySring;
	}
	
}
