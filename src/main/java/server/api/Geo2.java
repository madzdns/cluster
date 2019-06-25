package server.api;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.IspResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Geo2 {
	
	public final static String UNKNOWN = null;
	private DatabaseReader reader = null;
	private Logger log = LoggerFactory.getLogger(Geo2.class);
	
	public void initGeo(String db_location) throws IOException {
		
		log.info("Loading GEO DATABASE {} ...",db_location);
		File database = new File(db_location);
		reader = new DatabaseReader.Builder(database).build();
	}
	
	public Location getGeo(String ip) {
		
		CityResponse loc;
		IspResponse isp_loc;
		Location location = new Location();
		
		if(reader!=null) {
			
			try {
				
				loc = reader.city(InetAddress.getByName(ip));
				
				if(loc == null) {
					
					return location;	
				}
				
				if(loc.getContinent() == null || loc.getContinent().getName() == null) {
					
					return location;
				}
				
				location.continent = loc.getContinent().getName().toLowerCase();
				
				if(loc.getCountry() == null || loc.getCountry().getIsoCode() == null) {
					
					return location;
				}
				
				location.country = loc.getCountry().getIsoCode().toLowerCase();
				
				if(loc.getMostSpecificSubdivision() != null && loc.getMostSpecificSubdivision().getIsoCode() != null ) {
					
					location.region = new StringBuilder(location.country)
					.append("-")
					.append(loc.getMostSpecificSubdivision().getIsoCode())
					.toString().toLowerCase();	
				}
				
				location.city = (loc.getCity() != null&&loc.getCity().getName()!=null ? loc.getCity().getName().toLowerCase():UNKNOWN);
				
				isp_loc = reader.isp(InetAddress.getByName(ip));
				location.isp = ((isp_loc != null&&isp_loc.getIsp()!=null) ? isp_loc.getIsp().toLowerCase():UNKNOWN);			
				
			} catch (Exception e){
				
				log.error(e.getMessage());
				location.continent = UNKNOWN;
				location.country = UNKNOWN;
				location.region = UNKNOWN;
				location.city = UNKNOWN;
				location.isp = UNKNOWN;
			}
		}
		
		return location;
	}
	
	public static class Location {
		
		public String continent = UNKNOWN;
		public String country = UNKNOWN;
		public String region = UNKNOWN;
		public String city = UNKNOWN;
		public String isp = UNKNOWN;
		public double lang = 0;
		public double lat = 0;
		public String asn = UNKNOWN;
		
		@Override
		public String toString() {
			
			return new StringBuilder(String.valueOf(continent)).append(",").
					append(String.valueOf(country)).append(",").append(String.valueOf(region)).
					append(",").append(String.valueOf(city)).append(",").append(String.valueOf(isp)).toString();
		}
	}

}
