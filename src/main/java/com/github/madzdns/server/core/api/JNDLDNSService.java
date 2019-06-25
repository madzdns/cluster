package com.github.madzdns.server.core.api;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

public class JNDLDNSService {

	private static Properties env;
    private static final String CNAME_ATTRIB = "CNAME";
    private static String[] CNAME_ATTRIBS = { CNAME_ATTRIB };

    public static String getCnameByJNDI(String host) throws NamingException {
    	
    	if(env == null) {
    		
    		env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
            
            env.put(Context.PROVIDER_URL, "dns://8.8.8.8");
    	}
    	
        return getCNAME(new InitialDirContext(env), host);
    }

    private static String getCNAME(InitialDirContext idc, String host) throws NamingException {
        String cname = host;
        Attributes attrs = idc.getAttributes(host, CNAME_ATTRIBS);
        Attribute attr = attrs.get(CNAME_ATTRIB);

        if (attr != null) {
            int count = attr.size();
            if (count == 1) {
                cname = getCNAME(idc, (String) attr.get(0));
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    sb.append("-> " + attr.get(i) + "\n");
                }

                throw new NamingException("Unexpected count while looking for CNAME of " + host + ". Expected 1. Got " + count + ".\n"
                        + sb.toString());
            }
        }

        return cname;
    }
}
