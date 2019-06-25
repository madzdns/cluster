package server.core;

import java.io.IOException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.config.RequestCount;

public class RequestReportServiceCall implements Runnable {
	
	private static Logger log = LoggerFactory.getLogger(RequestReportServiceCall.class);

	private String zone;
	private RequestCount count;
	
	private String servicePath;
	private String host;
	
	public RequestReportServiceCall(String zone, String key, long planZoneId, RequestCount count, 
			String serviceProto, String serviceHost, String servicePath) {

		this.zone = zone;
		this.count = count;
		
		this.servicePath = new StringBuilder(serviceProto).
				append("://").
				append(serviceHost).append(servicePath).
				append("?zone=").
				append(zone).
				append("&key=").
				append(key).
				append("&planZoneId=").
				append(planZoneId).
				append("&count=").
				append(count.getCurrentUsedValueAndReset()).
				append("&totalCount=").append(count.getTotalUsedValue()).toString();
		
		this.host = serviceHost;
	}
	
	@Override
	public void run() {
		
		CloseableHttpClient httpClient = HttpClients.createDefault();
		
		CloseableHttpResponse response = null;
		
		log.debug("Sending request for : {}", servicePath);
		HttpGet httpGet = new HttpGet(servicePath);
				
		try {
			
			httpGet.setHeader(HttpHeaders.HOST, host);
			httpGet.setHeader("User-Agent", "Frfra-HttpClient");
            response = httpClient.execute(httpGet);
            
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {

            	log.error("Reporting request count failed for zone {}", host, zone);
            }
            else {

            	long value = Long.parseLong(EntityUtils.toString(response.getEntity()));
            	
            	log.debug("Returned value for request count of zone {} was {}.",zone, value);
            		
            	count.setTotalUsedValueIfGreater(value);
            }
			
		} catch (Exception e) {
			
			log.error("", e);
		}
		finally {
			
			if(response != null) {
				
				try {
					
					response.close();
					httpGet.releaseConnection();
					
				} catch (IOException e) {}
			}
		}
	}

}
