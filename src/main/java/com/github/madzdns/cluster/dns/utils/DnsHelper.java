package com.github.madzdns.cluster.dns.utils;

import java.net.InetAddress;

public class DnsHelper {
	
	private final static String IPV4_ARPA = "in-addr.arpa.";
	
	public final static String IPV6_ARPA = "ip6.arpa.";
	
	public final static String ARPA_ZONE = "arpa.";

	public static String getPtrIP(String ip) throws Exception {

		if(ip == null) {
			
			throw new IllegalArgumentException("Null");
		}
		
		if(ip.endsWith(IPV4_ARPA) || ip.endsWith(IPV6_ARPA)) {
			
			return ip;
		}
		
		if(ip.contains(".")) {
			
			String[] parts = ip.split("\\.");
			
			if(parts.length > 0) {
				
				StringBuilder sb = new StringBuilder();
				
				for(int c = parts.length -1; c >= 0; c--) {
					
					sb.append(parts[c]).append(".");
				}
				
				return sb.append(IPV4_ARPA).toString();
			}
		}
		else if(ip.contains(":")) {
			
			InetAddress a = InetAddress.getByName(ip);
			
		    byte[] bytes = a.getAddress();
		    
		    StringBuilder sb = new StringBuilder();
		    
		    for (byte b : bytes) {
				
		    	sb.append(String.format("%02X", b));
			}
		     
		     char[] resArr = sb.toString().toCharArray();
		     
		     StringBuilder resSb = new StringBuilder();
		     
		     for(int c = resArr.length -1; c >= 0; c--) {
		    	
		    	char b = resArr[c];
		    	resSb.append(Character.toLowerCase(b)).append("."); 
		     }
		    
		    return resSb.append(IPV6_ARPA).toString();
		}
		else {
			
			return new StringBuilder(ip).append(".").append(IPV4_ARPA).toString();
		}
		
		return null;
	}
}
