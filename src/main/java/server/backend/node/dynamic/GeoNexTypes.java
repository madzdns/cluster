package server.backend.node.dynamic;

public class GeoNexTypes {

	public final static String CONTINENT = "continent";
	public final static String COUNTRY = "country";
	public final static String REGION = "region";
	public final static String CITY = "city";
	public final static String ISP = "isp";
	public final static String DEFAULT = "*";
	
	public final static byte default_value = 1;
	public final static byte no_value = 0;
	//this is used when no geo-nex is defined on client and a cron policy updated geomaps to null
	public final static byte update_alert_value = (byte)257;
}
