package server.backend;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.frfra.frsynch.ClusterNode.ClusterAddress;

public class BackendNode {
	
	private short id;
	
	private String name;
	
	private Set<BackendAddress> backendAddresses;
	
	private Set<BackendAddress> backendAddrzForDynamicNodes = null;
	
	private Set<BackendAddress> backendAddrzForStaticNodes = null;
	
	private boolean valid;
	
	private long lastModified;
	
	public BackendNode(short id, String name, Set<ClusterAddress> backendAddresses, boolean isValid, long lastModified) {
		
		this.id = id;
		
		this.name = name;
		
		this.valid = isValid;
		
		this.lastModified = lastModified;
		
		if(backendAddresses != null) {
			
			this.backendAddresses = new HashSet<BackendAddress>();
			
			for(Iterator<ClusterAddress> it = backendAddresses.iterator(); it.hasNext(); ) {
				
				ClusterAddress ca = it.next();
				
				this.backendAddresses.add(new BackendAddress(ca.getAddress(), ca.getPort()));
			}
		}
		
		createDynamicAndStaticBakckendsList(this.backendAddresses);
	}

	private void createDynamicAndStaticBakckendsList(final Set<BackendAddress> backendAddrz) {
		
		if(backendAddrz != null) {
			
			Set<BackendAddress> backendAddrzForDynamicNodesTmp = new HashSet<BackendAddress>();
			
			Set<BackendAddress> backendAddrzForStaticNodesTmp = new HashSet<BackendAddress>();
			
			for(Iterator<BackendAddress> it = backendAddrz.iterator(); it.hasNext();) {
				
				BackendAddress ca = it.next();
				
				if(ca.isPortEndsWith8()) {
					
					backendAddrzForStaticNodesTmp.add(ca);
				}
				else {
					
					backendAddrzForDynamicNodesTmp.add(ca);
				}
			}
			
			backendAddrzForDynamicNodes = backendAddrzForDynamicNodesTmp;
			backendAddrzForStaticNodes = backendAddrzForStaticNodesTmp;
		}
	}
	
	public short getId() {
		
		return this.id;
	}
	
	protected void setBackendAddresses(Set<ClusterAddress> backendAddresses) {
		
		if(backendAddresses != null) {
			
			this.backendAddresses = new HashSet<BackendAddress>();
			
			for(Iterator<ClusterAddress> it = backendAddresses.iterator(); it.hasNext(); ) {
				
				ClusterAddress ca = it.next();
				
				this.backendAddresses.add(new BackendAddress(ca.getAddress(), ca.getPort()));
			}
			
			createDynamicAndStaticBakckendsList(this.backendAddresses);
		}
	}
	
	public Set<BackendAddress> getBackendAddrzForDynamicNodes() {
		
		return backendAddrzForDynamicNodes;
	}
	
	public Set<BackendAddress> getBackendAddrzForStaticNodes() {
		
		return backendAddrzForStaticNodes;
	}

	public String getName() {
		
		return name;
	}

	public boolean isValid() {
		
		return valid;
	}

	public long getLastModified() {
		return lastModified;
	}
}
