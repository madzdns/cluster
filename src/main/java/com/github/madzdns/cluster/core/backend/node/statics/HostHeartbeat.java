package com.github.madzdns.cluster.core.backend.node.statics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.backend.node.dynamic.impl.Node;
import com.github.madzdns.cluster.core.backend.node.dynamic.impl.Node.EntryParam;

public class HostHeartbeat implements Job {
	
	private static Logger log = LoggerFactory.getLogger(HostHeartbeat.class);

	public final static String HOST_KEY = "host";
	public final static String NODE_KEY = "node";
	public final static String NODE_DOMAIN_KEY = "node_domain";
	public final static String PORT_KEY = "port";
	public final static String INTERVAL_KEY = "interval";
	
	private String host;
	private String nodeDomain;
	private int port;
	private Node node;
	
	private String hostString = null;

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		if(host == null) {
			
			JobDataMap dataMap =  context.getJobDetail().getJobDataMap();
			if(dataMap != null) {
			
				node = (Node)dataMap.get(NODE_KEY);
				host = dataMap.getString(HOST_KEY);
				port = dataMap.getInt(PORT_KEY);
				nodeDomain = dataMap.getString(NODE_DOMAIN_KEY);
				
				if(host == null) {
					
					throw new JobExecutionException("Could not find host info");
				}
				
				hostString = new StringBuilder("http://")
				.append(node.getServer()).append(":").append(port)
				.append("/frfra.xml").toString();
			}
			else {
				
				throw new JobExecutionException("Could not find host info");
			}
		}
		
		BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
		
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();//HttpClients.createDefault();
		CloseableHttpResponse response = null;
		
		try {

			HttpGet httpGet = new HttpGet(hostString);
			httpGet.setHeader(HttpHeaders.HOST, host);
			httpGet.setHeader("User-Agent", "Frfra-HttpClient");
            response = httpClient.execute(httpGet);
            
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            
            char[] buffer = new char[1000];
            
            int read = 0;
            int readSum = 0;
            
            while ((read = rd.read(buffer)) != -1) {

            	readSum += read;
            	
            	if(readSum > 5000) {
            		
            		log.error("Heartbeat check failed for {} due to large content size {}, {}KB downloaded", host, 
            				response.getEntity().getContentLength(), readSum/1000);

            		response.close();
            		cm.shutdown();
            		
            		response = null;
            		rd = null;
            		
            		httpGet.releaseConnection();
            		httpGet = null;
            		
            		return;
            	}
            }
            
            if(response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            	
            	EntryParam ep = node.getEntryParam(nodeDomain);
            	
            	if(ep != null) {
            		
            		ep.updateLastReportForHostNode();	
            		log.debug("Heartbeat is ok for host {} with ip {} and domain {}", host, node.getServer(), nodeDomain);
            	}
            	else {
            		
            		log.error("Heartbeat faild for host {} with ip {} du to null entryparam of domain {}", host, node.getServer(), nodeDomain);
            	}
            }
            else {
            	
            	log.debug("Heartbeat failed for host {} with ip {} with status code {}", host, node.getServer(), response.getStatusLine().getStatusCode());
            }
            
		} catch (Exception e) {
			
			log.error(e.getMessage());
		}
		finally {
			
			if(response != null) {
				
				try {
					
					response.close();
				} catch (IOException e) {}
			}
		}
	}
}
