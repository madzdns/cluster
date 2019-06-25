package com.github.madzdns.server.core.api;

public class ProtoInfo {
	
	public final static String HTTP_SRV = "HTTP";
	public final static String RTMP_SRV = "RTMP";
	public final static String RTSP_SRV = "RTSP";

	private byte srvProto;
	private String serviceName;
	private short defaultPort = 0;

	public ProtoInfo(byte srvProto, String serviceName, short defaultPort) {

		this.srvProto = srvProto;
		this.serviceName = serviceName;
		this.defaultPort = defaultPort;
	}

	public byte getSrvProto() {
		
		return srvProto;
	}

	public String getServiceName() {
		
		return serviceName;
	}

	public short getDefaultPort() {
		
		return defaultPort;
	}
}
