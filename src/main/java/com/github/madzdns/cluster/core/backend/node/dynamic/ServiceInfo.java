package com.github.madzdns.cluster.core.backend.node.dynamic;

public class ServiceInfo {
	
	public static byte typeFromString(String proto) {
		
		if(proto.equalsIgnoreCase(UDPPROTO))
			return UDP;
		if(proto.equalsIgnoreCase(TCPPROTO))
			return TCP;
		return TCP;
	}
	
	public String getTypeAsString() {
		
		return type==UDP?UDPPROTO:type==TCP?TCPPROTO:"";
	}
	
	private static final String UDPPROTO = "udp";
	private static final String TCPPROTO = "tcp";
	public static final byte UDP = 1;
	public static final byte TCP = 2;
	public static final byte UP = 1;
	public static final byte DOWN = 0;
	public short port;
	public byte type;
	public byte value;
	//maped defined in client, don't need maped here
	public double raise = 0;
	public ServiceInfo()
	{}
	
	public ServiceInfo(final short port,final byte type,final byte value,double raise) {
		
		this.port=port;
		this.type=type;
		this.value=value;
		this.raise=raise;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof ServiceInfo) {
			
			ServiceInfo o = (ServiceInfo) obj;
			if(this.port==o.port&&this.type==o.type)
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		
		return getTypeAsString()+"://"+port+":"+(short)value+"/raise="+raise;
	}
}
