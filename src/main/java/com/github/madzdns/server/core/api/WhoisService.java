package com.github.madzdns.server.core.api;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.net.whois.WhoisClient;

public class WhoisService {

	private final static Pattern asn = Pattern.compile(".*origin:\\s+([a-zA-Z0-9]*).*",Pattern.DOTALL);
	
	public String getASN(String ip) throws Exception{

		StringBuilder result = new StringBuilder("");

		WhoisClient whois = new WhoisClient();

		//default is internic.net
		//WhoisClient.DEFAULT_HOST
		whois.connect("riswhois.ripe.net");
		String whoisData1 = whois.query(ip);
		result.append(whoisData1);
		whois.disconnect();
		
		Matcher m = asn.matcher(result.toString());
		
		if(m.matches()) {
			
			return m.group(1);
		}

		return null;
	}
}
