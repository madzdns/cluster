package server.api;

import java.lang.reflect.Method;

import server.config.Config;

public class FRfra {

	public static String lineSeparator;
	
	static {
		
		try {
			
			Method linesep = System.class.getDeclaredMethod("lineSeparator",(Class<?>[])null);
			lineSeparator = (String)linesep.invoke(null, (Object[])null);
			
		} catch (Exception e) {
			
			lineSeparator = System.getProperty("line.separator");
		}
	}
	
	public static final String QS_APIKEY = "api";
	public static final String QS_FWDPORT = "fwpo";
	public static final String QS_FWDPROTO = "fwpr";
	
	public static final String FD_APIKEY = "a";
	public static final String FD_FWDPORT = "p";
	public static final String FD_FWDPROTO = "r";
	public static final String FD_SRVPROTO = "s";
	public static final String FD_FORWARD = "h";
	public static final String FD_HTTPONLYFORWARD = "ho";
	public static final String FD_SSLFORWARD = "fs";
	
	public static final String REFERRER = Config.getApp().getApiPrefix()+"_referrer";
	
	public static final String ORIGIN_REFERRER = Config.getApp().getApiPrefix()+"_origin_referrer";
	
	private static String server_version;
	private static String ststem_version;
	
	public static String getServerVersion() {
		
		if(server_version==null)
			return server_version = Config.getApp().getName()+" "+(Config.getApp().isVerbose()?Config.getApp().getVersion():"");
		return server_version;
	}
	
	public static String getSystemVersion() {
		
		if(ststem_version==null)
			return ststem_version = (Config.getApp().isVerbose()?" ("
					+ System.getProperty("os.name") + " / "
					+ System.getProperty("os.version") + " / "
					+ System.getProperty("os.arch") + ")":"");
		return ststem_version;
	}
}
