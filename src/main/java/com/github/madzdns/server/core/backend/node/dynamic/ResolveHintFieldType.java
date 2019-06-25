package com.github.madzdns.server.core.backend.node.dynamic;

public enum ResolveHintFieldType {

	REFERRE(ResolveHintType.HTTP_SINGLE_DOMAIN, "REFERRER", (byte)1);
	
	private ResolveHintType resolveHintType;
	private String type;
	private byte value;
	
	private ResolveHintFieldType(ResolveHintType rh, String type, byte value) {
		
		this.resolveHintType = rh;
		this.type = type;
		this.value = value;
	}

	public String getType() {
		
		return type;
	}

	public byte getValue() {
		
		return value;
	}
	
	public ResolveHintType getResolveHintType() {
		
		return this.resolveHintType;
	}
	
	public static byte valueFromType(ResolveHintType resolveHintType, String type) {
		
        for(ResolveHintFieldType e : ResolveHintFieldType.values()) {
        	
            if(e.resolveHintType == resolveHintType && e.type.equals(type)) return e.value;
        }
        return 0;
    }
	
	public static byte valueFromType(String resolveHintType, String type) {
		
        for(ResolveHintFieldType e : ResolveHintFieldType.values()) {
        	
            if(e.resolveHintType.getType().equals(resolveHintType) && e.type.equals(type)) return e.value;
        }
        
        return 0;
    }
}
