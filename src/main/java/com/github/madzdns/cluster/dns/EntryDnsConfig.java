package com.github.madzdns.cluster.dns;

import com.github.madzdns.cluster.core.config.EntryResolveModes;

public class EntryDnsConfig {

	private EntryResolveModes resolveMode;
	
	private long ttl;
	
	public EntryDnsConfig(int mode, long ttl) {
		
		this.resolveMode = EntryResolveModes.fromValue(mode);
		this.ttl = ttl;
	}

	public EntryResolveModes getResolveMode() {
		
		return resolveMode;
	}

	public void setResolveMode(EntryResolveModes resolveMode) {
		
		this.resolveMode = resolveMode;
	}

	public long getTtl() {
		
		return ttl;
	}

	public void setTtl(long ttl) {
		
		this.ttl = ttl;
	}
	
	public boolean isDnsResolve() {
		
		if(this.resolveMode == null) {
			
			return false;
		}
		
		return this.resolveMode == EntryResolveModes.DNS_RESOLVE;
	}
	
	public boolean isProtoResolved() {
		
		if(this.resolveMode == null) {
			
			return false;
		}
		
		return this.resolveMode == EntryResolveModes.PROTO_RESOLVE;
	}
	
	public boolean isJustDnsHaResolved() {
		
		if(this.resolveMode == null) {
			
			return false;
		}
		
		return this.resolveMode == EntryResolveModes.DNS_JUST_HA_RESOLVE;
	}
}
