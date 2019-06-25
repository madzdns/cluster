package com.github.madzdns.server.core.api;

import java.util.List;

import com.github.madzdns.server.core.utils.DomainHelper;

public class LookupResult {
	
	public static class FullApiResult {
		
		private short forwardPort;
		private String forwardProto;
		
		private byte serviceProto = 0;
		
		public short getForwardPort() {
			
			return forwardPort;
		}
		
		public String getForwardProto() {
			
			return forwardProto;
		}
		
		public byte getServiceProto() {
			
			return serviceProto;
		}
		
		@Override
		public String toString() {

			return new StringBuilder("serviceProto=").
					append(serviceProto).
					append(",forwardProto").
					append(forwardProto).
					append(",forwardPort=").
					append(forwardPort).toString();
		}
	}

	private String fqdnName;
	private FullApiResult fullApiResult = null;
	
	private boolean redirectResult = false;
	
	private String redirectName = null;
	
	private boolean httpOnly = false;
	
	private boolean sslRedirect = false;
	
	private List<XbillDnsService.NameRecord> nsNameRecords = null;
	
	/**
	 * Just a non null Object
	 */
	public LookupResult() {}
	
	public LookupResult(String resolvedName, String configName) {
		
		DomainHelper helper = null;
		
		if(configName != null) {
			
			helper = new DomainHelper(configName);
			
			if(helper.IsFullDomain()) {
				
				if(helper.isForward()) {
					
					this.redirectResult = true;
					
					if(resolvedName.endsWith(".")) {
						
						this.redirectName = resolvedName.substring(0, resolvedName.length() - 1);
					}
					else {
						
						this.redirectName = resolvedName;
					}
					
					if(helper.isHttpOnlyForward()) {
						
						this.httpOnly = true;
					}
					
					if(helper.isSslForward()) {
						
						this.sslRedirect = true;
					}
				}
			}
		}
		
		if(helper == null || !helper.IsFullDomain()) {
			
			helper = new DomainHelper(resolvedName);
		}

		if(helper.IsFullDomain()) {
			
			this.fqdnName = helper.GetPureDomain();
			
			String fwdPort = helper.GetFwdPort();
			
			short fwdportnum = 0;
			
			if( fwdPort != null ) {
				
				try {
					
					fwdportnum = Short.parseShort(fwdPort);
					
				} catch(Exception e) {
					
					this.fullApiResult = null;
				}
				
				if(fwdportnum > 0) {
					
					this.fullApiResult = new FullApiResult();
					this.fullApiResult.forwardPort = fwdportnum;
				}
			}
			
			if(this.fullApiResult != null) {
				
				this.fullApiResult.forwardProto = helper.GetFwdProto();
				this.fullApiResult.serviceProto = helper.GetSrvProto();
			}
		}
		else {
			
			this.fqdnName = resolvedName;
		}
		
		if(!this.fqdnName.endsWith(".")) {
			
			this.fqdnName = new StringBuilder().append(this.fqdnName).append(".").toString();
		}
	}
	
	public LookupResult(List<XbillDnsService.NameRecord> nsRecords) {
		
		this.nsNameRecords = nsRecords;
	}
	
	public String getFqdnName() {
		
		return fqdnName;
	}
	
	public FullApiResult getFullApiResult() {
		
		return fullApiResult;
	}
	
	public List<XbillDnsService.NameRecord> getNsNameRecords() {
		
		return this.nsNameRecords;
	}
	
	public boolean isRedirectResult() {
		
		return redirectResult;
	}
	
	public boolean isRedirectWithSSL() {
		
		return sslRedirect;
	}

	public String getRedirectName() {
		
		return redirectName;
	}

	public boolean isHttpOnly() {
		
		return httpOnly;
	}
}
