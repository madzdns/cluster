package com.github.madzdns.server.core.backend;

import java.util.List;
import java.util.Map;

public interface IResourceCache {

	public MetaData get(String key);
	public void update(String name, MetaData resource);
	public Map<String, MetaData> getResourcesAsMap();
	public List<MetaData> getResourcesAsList();
	
}
