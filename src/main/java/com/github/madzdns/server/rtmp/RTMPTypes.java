package com.github.madzdns.server.rtmp;

import java.util.List;

public enum RTMPTypes {

	RTMP("RTMP"),
	RTMPT("RTMPT"),
	RTMPS("RTMPS"),
	RTMPTS("RTMPTS"),
	RTMPE("RTMPE"),
	RTMPTE("RTMPTE"),
	RTMPTES("RTMPTES");
	
	private String type;
	
	private RTMPTypes(String type) {
		
		this.type = type;
	}
	
	public String getType() {
		
		return this.type;
	}
	
	public static RTMPTypes getFinalProto(List<RTMPTypes> types) {
		
		if(types == null || types.size() == 0) {
			
			return RTMP;
		}
		
		if(types.contains(RTMPT)) {
			
			if(types.contains(RTMPE)) {
				
				return RTMPTE;
			}
			
			if(types.contains(RTMPS)) {
				
				return RTMPTS;
			}
			
			return RTMPT;
		}
		
		if(types.contains(RTMPE)) {
			
			return RTMPE;
		}
		
		if(types.contains(RTMPS)) {
			
			return RTMPS;
		}
		
		return RTMP;
	}
}
