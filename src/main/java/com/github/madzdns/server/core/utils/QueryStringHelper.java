package com.github.madzdns.server.core.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.madzdns.server.core.config.Config;
import com.github.madzdns.server.core.api.FRfra;

public class QueryStringHelper {
	
	public static class ParsedQuery {
		
		public String apiKey = null;
		public String queryString = "";
		public String fwdPort = null;
		public String fwdProto = null;
	}
	
	private static String API_PREFIX = null;
	
	private static String getApiPrefix() {
		
		if(API_PREFIX == null) {
			
			return API_PREFIX = Config.getApp().getApiPrefix()+"_";
		}
		
		return API_PREFIX;
	}

	private final static Pattern APIKEY_PATTERN = Pattern.compile(".*?"+getApiPrefix()+FRfra.QS_APIKEY+"=(.*?)(&.*|$)");
	private final static Pattern PORT_PATTERN = Pattern.compile(".*?"+getApiPrefix()+FRfra.QS_FWDPORT+"=(.*?)(&.*|$)");
	private final static Pattern PROTO_PATTERN = Pattern.compile(".*?"+getApiPrefix()+FRfra.QS_FWDPROTO+"=(.*?)(&.*|$)");
	
	private final static String APIKEY_REMOVE_PREFIX_REGX = "&?"+FRfra.QS_APIKEY+"=";
	
	private final static String FWPORT_REMOVE_PREFIX_REGX = "&?"+FRfra.QS_FWDPORT+"=";
	
	private final static String FWPROTO_REMOVE_PREFIX_REGX = "&?"+FRfra.QS_FWDPROTO+"=";
	
	public ParsedQuery parseQs(String qstring) {
		
		ParsedQuery pq = new ParsedQuery();
		
		if(qstring == null || qstring.equals("")) {
		
			return pq;
		}
		
		if(qstring.indexOf(getApiPrefix()) == -1) {
			
			pq.queryString = qstring;
			return pq;
		}
			
		Matcher m = APIKEY_PATTERN.matcher(qstring);
		
		if(m.matches()) {
			
			pq.apiKey = m.group(1);
			
			pq.queryString = qstring.replaceAll(new StringBuilder(APIKEY_REMOVE_PREFIX_REGX).append(pq.apiKey).toString()
					,"");
			pq.queryString = pq.queryString.replaceAll("&&","&");
			pq.queryString = pq.queryString.replaceAll("\\?&","\\?");
			
			if(pq.queryString.equals("?")) {
				
				pq.queryString = "";
				return pq;
			}
		}
		
		m = PORT_PATTERN.matcher(pq.queryString);
		
		if(m.matches()) {
			
			pq.fwdPort = m.group(1);
			pq.queryString = pq.queryString.replaceAll(new StringBuilder(FWPORT_REMOVE_PREFIX_REGX).append(pq.fwdPort).toString()
					,"");
			pq.queryString = pq.queryString.replaceAll("&&","&");
			pq.queryString = pq.queryString.replaceAll("\\?&","\\?");
			
			if(pq.queryString.equals("?")) {
				
				pq.queryString = "";
				return pq;
			}
		}
		
		m = PROTO_PATTERN.matcher(pq.queryString);
		
		if(m.matches()) {
			
			pq.fwdProto = m.group(1);
			pq.queryString = pq.queryString.replaceAll(new StringBuilder(FWPROTO_REMOVE_PREFIX_REGX).append(pq.fwdProto).toString(),"");
			pq.queryString = pq.queryString.replaceAll("&&","&");
			pq.queryString = pq.queryString.replaceAll("\\?&","\\?");
			
			if(pq.queryString.equals("?")) {
				
				pq.queryString = "";
				return pq;
			}
		}
		
		return pq;
	}
}
