package server.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import server.api.FRfra;
import server.backend.node.dynamic.ServiceInfo;
import server.config.Config;

public class DomainHelper {
	
	private final static Pattern fwdPortPattern = Pattern.compile(".*?-"+FRfra.FD_FWDPORT+"([0-9]+).*");
	
	private final static Pattern apiKeyPattern = Pattern.compile(".*?-"+FRfra.FD_APIKEY+"([^\\-]+).*");
	
	private final static Pattern fwdProtoPattern = Pattern.compile(".*?-"+FRfra.FD_FWDPROTO+"([^\\-]+).*");
	
	private final static Pattern fwdSrvProtoPattern = Pattern.compile(".*?-"+FRfra.FD_SRVPROTO+"([^\\-]+).*");
	
	private final static Pattern httpOnlyRnameForwardPattern = Pattern.compile(".*?-"+FRfra.FD_HTTPONLYFORWARD+".*");
	
	private final static Pattern forwardPattern = Pattern.compile(".*?-"+FRfra.FD_FORWARD+".*");
	
	private final static Pattern sslForwardPattern = Pattern.compile(".*?-"+FRfra.FD_SSLFORWARD+".*");
	
	private static String API_PREFIX = null;
	
	private static String getApiPrefix() {
		
		if(API_PREFIX == null) {
			
			return API_PREFIX = Config.getApp().getApiPrefix()+"-";
		}
		
		return API_PREFIX;
	}
	
	private static Logger log = LoggerFactory.getLogger(DomainHelper.class);
	
	private String pureDomain = null;
	
	private String apiPart = null;
	
	private boolean fullDomain = false;
	
	private String originalDomain = null;
	
	public DomainHelper(final Name name) {
		
		if(name==null) {
			
			return;
		}
		
		originalDomain = name.toString();	
		
		if(name.labels()<3) {
			
			return;
		}
			
		if(name.getLabelString(0).startsWith(getApiPrefix())) {
			
			final String[] labels = originalDomain.split("\\.", 2);	
			this.pureDomain = labels[1];
			this.fullDomain = true;
			this.apiPart = name.getLabelString(0);
		}
	}
	
	public DomainHelper(final String name) {
		
		if(name == null) {
			
			return;
		}

		originalDomain = name;
		
		final String[] parts = name.split("\\.");
		
		if(parts==null||parts.length<3) {
			
			return;
		}
			
		if(name.startsWith(getApiPrefix())) {
			
			final String[] labels = name.split("\\.", 2);	
			this.pureDomain = labels[1];
			this.fullDomain = true;
			this.apiPart = parts[0];
		}
	}
	
	public boolean IsFullDomain() {
		
		return this.fullDomain;
	}
	
	public Name GetPureDomainToName() {
			
		if(this.pureDomain != null) {
			
			try {
				
				return Name.fromString(pureDomain);
				
			} catch (TextParseException e) {
				
				log.error("",e);
			}
		}
		
		try {
			
			return Name.fromString(originalDomain);
			
		} catch (TextParseException e) {
			
			log.error("",e);
			return null;
		}
	}
	
	public String GetPureDomain() {
		
		if(this.pureDomain != null) {
			
			return pureDomain;
		}
			
		return originalDomain;
	}
	
	public String GetFwdPort() {
		
		if(this.apiPart != null) {

			Matcher m = fwdPortPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				return m.group(1);
			}
		}
		
		return null;
	}
	
	public String GetApiKey() {
		
		if(this.apiPart != null) {
			
			Matcher m = apiKeyPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				return m.group(1);
			}
				
		}
		
		return null;
	}
	
	public String GetFwdProto() {
		
		if(this.apiPart != null) {

			Matcher m = fwdProtoPattern.matcher(this.apiPart);
			
			if(m.matches()) {
			
				return m.group(1);
			}
		}
		
		return null;
	}
	
	public short GetSrvPort() {
		
		if(this.apiPart != null) {
			
			Matcher m = fwdPortPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				String port = m.group(1);
				
				try {
					
					return Short.parseShort(port);
					
				} catch(Exception e) {
					
					return 0;
				}
			}
				
		}
		
		return 0;
	}
	
	public byte GetSrvProto() {
		
		if(this.apiPart != null) {

			Matcher m = fwdSrvProtoPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				String proto = m.group(1);
				
				return ServiceInfo.typeFromString(proto);
			}
		}
		
		return 0;
	}
	
	public boolean isHttpOnlyForward() {

		if(this.apiPart != null) {

			Matcher m = httpOnlyRnameForwardPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isForward() {

		if(this.apiPart != null) {

			Matcher m = forwardPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isSslForward() {

		if(this.apiPart != null) {

			Matcher m = sslForwardPattern.matcher(this.apiPart);
			
			if(m.matches()) {
				
				return true;
			}
		}
		
		return false;
	}

	public String getApiPart() {
		
		return apiPart;
	}
}
