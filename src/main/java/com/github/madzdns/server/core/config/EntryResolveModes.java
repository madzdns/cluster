package com.github.madzdns.server.core.config;

public enum EntryResolveModes {

	DNS_RESOLVE(1),
	PROTO_RESOLVE(2),
	DNS_JUST_HA_RESOLVE(3),
	PROTO_RESOLVE_PROXY(4);
	/*
	DNS_WITH_FIXED_RESOLVE(4),
	DNS_WITH_FIXED_RESOLVE_RR(5),
	PROTO_WITH_FIXED_RESOLVE(6),
	PROTO_WITH_FIXED_RESOLVE_RR(7),
	DNS_WITH_FIXED_ALIVE_CHECK(8),
	PROTO_WITH_FIXED_ALIVE_CHECK(9),
	DNS_WITH_FIXED_ALIVE_CHECK_RR(10),
	PROTO_WITH_FIXED_ALIVE_CHECK_RR(11)*/;
	
	private int value;
	
	private EntryResolveModes(int value) {
		
		this.value = value;
	}
	
	public int getValue() {
		
		return this.value;
	}
	
	public static EntryResolveModes fromValue(int code){
		
        for(EntryResolveModes e : EntryResolveModes.values()){
            if(code == e.value) return e;
        }
        return null;
    }
}