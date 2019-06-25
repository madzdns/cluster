package server.backend;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.jcs.engine.CacheElement;
import org.apache.commons.jcs.engine.behavior.ICacheElement;
import org.apache.commons.jcs.engine.control.CompositeCache;
import org.apache.commons.jcs.engine.control.CompositeCacheManager;

public class JcsResourceCache implements IResourceCache {
	
	private String CACHE_NAME = "DEFAULT";
	
	protected static CompositeCache<Serializable, Serializable> cache;
	
	public JcsResourceCache(String CACHE_NAME, String jcs_conf_path) throws IOException {
		
		if(cache == null) {
			
			InputStream fis = new FileInputStream(new File(jcs_conf_path).getAbsolutePath());
			
			try {
				
				CompositeCacheManager ccm = CompositeCacheManager.getUnconfiguredInstance();
				Properties props = new Properties();
				props.load(fis);
				
				ccm.configure(props);
				
				this.CACHE_NAME = CACHE_NAME;
				
				cache = ccm.getCache(CACHE_NAME);
			}
			finally {
				
				fis.close();
			}
		}
	}

	@Override
	public MetaData get(String key) {
		
		if(cache == null) {
			
			return null;
		}
		
		ICacheElement<Serializable, Serializable> element = cache.get(key);
		
		if(element != null) {
			
			return (MetaData) element.getVal();
		}
		
		return null;
	}

	@Override
	public void update(String key, MetaData resource) {
		
		if(cache == null) {
			
			return;
		}
		
		ICacheElement<Serializable, Serializable> element = new CacheElement<Serializable, Serializable>(CACHE_NAME, key, resource);
		
		try {
			cache.update(element);
		} catch (IOException e) {
		}
	}

	@Override
	public Map<String, MetaData> getResourcesAsMap() {
		
		Map<String, MetaData> dataMap = new HashMap<String, MetaData>();
		
		if(cache == null) {
			
			return dataMap;
		}
		
		for(Iterator<Serializable> it = cache.getKeySet().iterator(); it.hasNext();) {
			
			ICacheElement<Serializable, Serializable> e = cache.get(it.next());
			
			if(e != null) {
				
				if(e.getVal() instanceof MetaData) {
				
					dataMap.put((String)e.getKey(), (MetaData)e.getVal());
				}
			}
		}
		
		return dataMap;
	}
	
	@Override
	public List<MetaData> getResourcesAsList() {
		
		List<MetaData> dataList = new ArrayList<MetaData>();
		
		if(cache == null) {
			
			return dataList;
		}
		
		for(Iterator<Serializable> it = cache.getKeySet().iterator(); it.hasNext();) {
			
			ICacheElement<Serializable, Serializable> e = cache.get(it.next());
			
			if(e != null) {
				
				if(e.getVal() instanceof MetaData) {
				
					dataList.add((MetaData)e.getVal());
				}
			}
		}
		
		return dataList;
	}

}
