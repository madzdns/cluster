package http.web.helper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryString {

	public Map<String,List<String>> parsQuery(String path) 
			throws MalformedURLException, UnsupportedEncodingException {
		
		//TODO handle exceptions
		
		Map<String,List<String>> params = new HashMap<String, List<String>>();
		
		if(path==null||!path.startsWith("?"))
			
			/*
			 * TODO
			 * This was not null and was returning an empty params. I changed it to null.
			 * Check if it does not make a problem
			 */
			return null;
		
		String queryWithoutQuestionMark = path.replaceFirst("\\?","&");
		
		if(queryWithoutQuestionMark==null)
			
			return null;
		
		final String[] pairs = queryWithoutQuestionMark.split("&");
		
		for (String pair : pairs) 
		{
			if(pair==null||pair.equals(""))
				
				continue;
			
			final int idx = pair.indexOf("=");
			
			String key = null;
			
			String value = null;
			
			if(idx>0 && pair.length() > idx + 1)
			{
				
				key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
				
				value = URLDecoder.decode(pair.substring(idx+1), "UTF-8");
				
				final int bracketIdx = key.indexOf("[");
				
				final int dotIdx = key.indexOf(".");
				
				if(dotIdx==bracketIdx)
				{
					ArrayList<String> values = new ArrayList<String>();
					
					if(value==null)
					{
						value = "";
					}
					
					StringBuffer sb = new StringBuffer()
					.append('\'').append(value).append('\'');
					values.add(sb.toString());
					
					params.put(key,values);
				}
				else if(bracketIdx>dotIdx&&dotIdx>0)
				{
					
					if(value==null)
						
						value = "";
					
					StringBuffer sb = new StringBuffer()
					.append(key.substring(dotIdx+1))
					.append("='")
					.append(value).append('\'');
					
					key = key.substring(0, dotIdx);
					value = sb.toString();
					
					ArrayList<String> values = new ArrayList<String>();
					values.add(value);
					
					params.put(key,values);
				}
				else
				{
					
					if(value==null)
						
						value = "";
					
					StringBuffer sb = new StringBuffer()
					.append(key.substring(bracketIdx))
					.append("='")
					.append(value).append('\'');
					
					key = key.substring(0, bracketIdx);
					value = sb.toString();
					
					List<String> values = params.get(key);
					
					if(values!=null)
					{
						values.add(value);
					}
					else
					{
						values = new ArrayList<String>();
						values.add(value);
						params.put(key,values);
					}
				}
				
			}
			else
			{
				
				key = pair;
				ArrayList<String> values = new ArrayList<String>();
				values.add(value);
				
				params.put(key,values);
			}
			
			
		}
		return params;
	}
	/*public Map<String,String> parsQuery(URL url) throws MalformedURLException, UnsupportedEncodingException
	{
		Map<String,String> params = new HashMap<String, String>();
		final String[] pairs = url.getQuery().split("&");
		for (String pair : pairs) 
		{
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			params.put(key,value);
		  }
		return params;
	}*/
}
