package com.github.madzdns.server.core.core.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxy {

	private static Logger log = LoggerFactory.getLogger(HttpProxy.class);
	
	/*public static final int SC_BAD_REQUEST = 1;
	public static final int SC_FAILED = 2;
	public static final int SC_OK = 200;*/
	
	public static final int METHOD_GET = 1;
	public static final int METHOD_POST = 2;
	public static final int METHOD_HEAD = 3;
	public static final int METHOD_PUT = 4;
	public static final int METHOD_DELETE = 5;
	
	String host;
	HttpRequestBase request;
	
	public HttpProxy(int httpMethod, String uri, String host, Properties headers, byte[] content, String contentType) throws UnsupportedEncodingException {
		
		this.host = host;
		createRequest(httpMethod, uri, headers, content, contentType);
	}
	
	public HttpProxy(int httpMethod, String schema, String host, Short port, String path, 
			Properties headers, byte[] content, String contentType) throws UnsupportedEncodingException {
		
		this.host = host;
		
		StringBuilder servicePathBuilder = new StringBuilder(schema).append("://").
				append(host).append(':').append(port);
		
		if(path != null) {
			
			if(path.charAt(0) == '/') {
				
				servicePathBuilder.append(path);
			}
			else {
				
				servicePathBuilder.append('/').append(path);
			}
		}
		
		createRequest(httpMethod, servicePathBuilder.toString(), headers, content, contentType);
	}
	
	private void createRequest(int httpMethod, String uri, Properties headers, byte[] content, String contentType) throws UnsupportedEncodingException {
		
		boolean removeContentLen = false;
		ByteArrayEntity params = null;
		
		if(content != null) {
			
			if(contentType != null) {
			
				params = new ByteArrayEntity(content, ContentType.parse(contentType));
			}
			else {
				
				params = new ByteArrayEntity(content);
			}
		}
		
		switch (httpMethod) {
		
		case METHOD_GET:
			
			request = new HttpGet(uri);
			break;
			
		case METHOD_POST:
			
			request = new HttpPost(uri);
			
			((HttpPost)request).setEntity(params);
			
			removeContentLen = true;
			break;
			
		case METHOD_HEAD:
			
			request = new HttpHead(uri);
			
			break;
		case METHOD_PUT:
			
			request = new HttpPut(uri);

			((HttpPut)request).setEntity(params);
			removeContentLen = true;
			break;
			
		case METHOD_DELETE:
			
			request = new HttpDelete(uri);
			break;

		default:
			
			throw new UnsupportedOperationException("Http Method was not supported");
		}
		
		if(headers != null && headers.entrySet() != null) {

			for(Iterator<Entry<Object, Object>> it = headers.entrySet().iterator(); it.hasNext(); ) {
				
				Entry<Object, Object> entry = it.next();
				
				if(entry.getKey() != HttpHeaders.HOST) {
					
					request.setHeader((String)entry.getKey(), (String)entry.getValue());	
				}
			}
		}
		
		if(removeContentLen) {
			
			request.removeHeaders("Content-Length");
		}
		
		request.setHeader(HttpHeaders.HOST, host);
		request.setHeader("User-Agent", "Frfra-HttpProxy");
	}
	
	public ProxyResponse request() {
		
		BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
		
		CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(cm).build();//HttpClients.createDefault();
		
        CloseableHttpResponse response = null;
        
		try {
            
			response = httpClient.execute(request);
			
            ProxyResponse proxyResponse = new ProxyResponse();
            
            HttpEntity entity = response.getEntity();
            BufferedReader rd = new BufferedReader(new InputStreamReader(entity.getContent()));
            
            char[] buffer = new char[1000];
            
            int read = 0;
            int readSum = 0;
            
            StringBuilder sb = new StringBuilder();
            
            while ((read = rd.read(buffer)) != -1) {

            	readSum += read;
            	
            	if(readSum > 500000) {
            		
            		proxyResponse.setResponseCode(HttpStatus.SC_BAD_REQUEST);
            		proxyResponse.setContent(null);
            		log.error("Proxy request failed for {} due to large content size {}, {}KB downloaded", host, 
            				response.getEntity().getContentLength(), readSum/1000);

            		response.close();
            		cm.shutdown();
            		
            		response = null;
            		rd = null;
            		
            		request.releaseConnection();
            		request = null;
            		
            		return proxyResponse;
            	}
            	
            	sb.append(Arrays.copyOfRange(buffer, 0, read));
            }

            proxyResponse.setContent(sb.toString());
            
            proxyResponse.setResponseCode(response.getStatusLine().getStatusCode());
            
            Header[] headers = response.getAllHeaders();
            
            if(headers != null) {
            	
            	for(int i =0; i < headers.length; i++) {
            		
            		Header header = headers[i];
            		proxyResponse.setHeader(header.getName(), header.getValue());
            	}
            }
            
            if(response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {

            	log.error("Proxy request failed for {}", host);
            }
            else {
            	
            	log.debug("Content retrived via proxy from {}.", host);
            }
            
            return proxyResponse;
			
		} catch (Exception e) {
			
			log.error("", e);
			
			return null;
		}
		finally {
			
			if(response != null) {
				
				try {
					
					response.close();
					request.releaseConnection();
				} catch (IOException e) {}
			}
		}
	}
}
