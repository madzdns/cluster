package com.github.madzdns.cluster.core.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Set;

import com.github.madzdns.cluster.core.config.Config;
import com.github.madzdns.cluster.core.config.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceCtrl extends MetaData{
	
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ResourceCtrl.class);
	
	private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private static final String CACHE_NAME = "MESSAGE";
	private static IResourceCache cache;
	
	/**
	 * We keep last modified meta data to make the process faster
	 */
	private volatile Map<String,MetaData> lastModifiedResources = null;

	/**
	 * Reloading saved meta data or creating a new empty map in the case of 
	 * nothings saved before
	 * @throws IOException 
	 */
	protected ResourceCtrl() throws IOException {

		cache = new JcsResourceCache(CACHE_NAME, Config.META_FILE);
			
		lastModifiedResources = getMetaMapRlock();
	}
	
	/**
	 * This gives a snapshot of last modified meta data and then make it internally null
	 * to catch new modified. Second call to this, returns null or different result.
	 * @return latest modified zones
	 */
	protected Collection<MetaData> getLastModifiedWlock() {
		
		try {
			
			lock.writeLock().lock();
			
			if(lastModifiedResources!=null
					&&lastModifiedResources.size()>0) {
				
				Collection<MetaData> tmpLastModifiedResources = lastModifiedResources.values();
				
				lastModifiedResources = new HashMap<String, MetaData>();
				
				return tmpLastModifiedResources;
			}
			
			return null;
		}
		finally{lock.writeLock().unlock();}
	}
	
	
	/*private MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final GeoResolver resolver,
			final byte state) {*/
	
	private MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final Resource resolver,
			final byte state) {
		
		MetaData meta = new MetaData(name, modified, resolver, state);
		
		try {
			
			lock.writeLock().lock();
			
			lastModifiedResources.put(name, meta);
			
		} finally{lock.writeLock().unlock(); }
		
		cache.update(name, meta);
		
		return meta;
	}
	

	/*protected MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final byte[] data,
			final byte state) throws Exception {
		
		GeoResolver resolver = null;
		
		if(data==null) {
			
			if(state != STATE_DEL)
				
				throw new Exception();
		}
		else {
			
			resolver = GeoResolver.fromBytes(data);
		}
		
		return updateMetaWlock(name, modified, awareIds, resolver, state);
	}*/

	protected MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final byte[] data,
			final byte state) throws Exception {
		
		Resource resource = null;
		
		if(data == null) {
			
			if(state != STATE_DEL)
				
				throw new Exception();
		}
		else {
			
			resource = Resource.fromBytes(data);
		}
		
		return updateMetaWlock(name, modified, awareIds, resource, state);
	}
	
	/*protected MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final byte state,
			final InputStream is) throws Exception {

		GeoResolver resolver = null;
		
		if(is == null) {
			
			if(state != STATE_DEL)
				
				throw new Exception("InputStream is null, but resource was valid");
		}
		else {
			
			resolver = GeoResolver.fromInputStream(is);
		}
		
		return updateMetaWlock(name, modified, awareIds, resolver, state);
	}*/
	
	protected MetaData updateMetaWlock(final String name,
			final long modified,
			Set<Short> awareIds,
			final byte state,
			final InputStream is) throws Exception {

		Resource resource = null;
		
		if(is == null) {
			
			if(state != STATE_DEL)
				
				throw new Exception("InputStream is null, but resource was valid");
		}
		else {
			
			resource = Resource.fromInputStream(is);
		}
		
		return updateMetaWlock(name, modified, awareIds, resource, state);
	}
	
	protected MetaData getMetaData(String name) {
		
		return cache.get(name);
	}
	
	/**
	 * This creates a wrapper Map so other threads could not have access 
	 * to metaResources out of synchronized
	 * @return
	 */
	protected Set<Entry<String,MetaData>> getMetaEntriesSetRlock() {

		Map<String,MetaData> metaMap = cache.getResourcesAsMap();
		
		return metaMap.entrySet();
	}
	
	/**
	 * This returns only those zones that is moved from
	 * modified resources to meta resources. So if there are
	 * modified or new zones and {@link ResourceCtrl#getLastModifiedRlock} 
	 * have not been called, getMetasListRlock is not aware of them.
	 * So if we modify any zone inside returned zone list, we know it
	 * does not affect new received zones
	 * 
	 * @return A list of zone data
	 */
	protected List<MetaData> getMetasListRlock() {
		
		return cache.getResourcesAsList();
	}
	
	private Map<String, MetaData> getMetaMapRlock() {
		
		return cache.getResourcesAsMap();
	}
	
	/**
	 * Returns a List of meta data except keys set and deleted ones
	 * @param keys
	 * @return
	 
	public List<MetaData> getMetaListExceptKeysAndDeletedRlock(final Set<String> keys) {
		
		if(keys==null)
			
			throw new NullPointerException();
		
		try {
			
			lock.readLock().lock();
			
			Set<String> currentKeys = metaResources.keySet();
			
			if(keys.containsAll(currentKeys))
				
				return null;
			
			currentKeys = null;
			
			List<MetaData> metaList = new ArrayList<MetaData>();
			
			for(Iterator<Entry<String,MetaData>> it=metaResources.entrySet().iterator();it.hasNext();)
			{
				
				Entry<String,MetaData> ent =it.next();
				
				if(ent.getValue().isValid()
					&&!keys.contains(ent.getKey()))
					
					metaList.add(ent.getValue());
				ent = null;
			}
			return metaList;
		}
		finally {
			
			if(!lastSave)
				
				save(new HashMap<String, MetaData>(metaResources));
			
			lock.readLock().unlock();
		}
	}
	*/
}
