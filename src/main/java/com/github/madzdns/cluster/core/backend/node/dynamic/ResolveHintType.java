package com.github.madzdns.cluster.core.backend.node.dynamic;

public enum ResolveHintType {

	HTTP_SINGLE_DOMAIN("HTTP_SINGLE_DOMAIN",(byte)1),
	HTTP_SET_PROXY("HTTP_SET_PROXY",(byte)2),
	HTTP_ORIGIN_REF("HTTP_ORIGIN_REF",(byte)3),
	HTTP_STEAKY_SESSION("HTTP_STEAKY_SESSION",(byte)4);
	
	private String type;
	private byte value;
	
	private ResolveHintType(String type, byte value) {
		
		this.type = type;
		this.value = value;
	}

	public String getType() {
		
		return type;
	}

	public byte getValue() {
		
		return value;
	}
	
	public static byte valueFromType(String type) {
		
        for(ResolveHintType e : ResolveHintType.values()) {
        	
            if(type.equals(e.type)) return e.value;
        }
        
        return 0;
    }
	
	public static ResolveHintType fromType(String type){
		
        for(ResolveHintType e : ResolveHintType.values()){
        	
            if(type.equals(e.type)) return e;
        }
        
        return null;
    }
}
