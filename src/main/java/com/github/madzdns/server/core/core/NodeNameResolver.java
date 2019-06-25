package com.github.madzdns.server.core.core;

import java.util.Date;

import com.github.madzdns.server.core.backend.node.dynamic.impl.Node;
import com.github.madzdns.server.core.config.MyEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.api.XbillDnsService;

public class NodeNameResolver implements Runnable {
	
	private static Logger log = LoggerFactory.getLogger(NodeNameResolver.class);
	
	String server;
	Node.GeoParams gp;
	String ndomain;
	MyEntry entry;
	
	

	public NodeNameResolver(String server, Node.GeoParams gp, String ndomain,
                            MyEntry entry) {

		this.server = server;
		this.gp = gp;
		this.ndomain = ndomain;
		this.entry = entry;
	}

	private void arrangeFailure() {
		
		gp.nameTriedDate = new Date();
		gp.maxNameTriedWaitTime += Node.DOMAIN_FAIL_WAIT_MILISECOND;
		
		log.warn("Refusing to resolve Names of Node {} for {} Milliseconds", server, gp.maxNameTriedWaitTime);
	}
	
	@Override
	public void run() {
		
		log.debug("Getting A record for domain {}..............................",ndomain);
		
		XbillDnsService dnsService = new XbillDnsService();
			
			
		dnsService = new XbillDnsService();
		
		if(dnsService.checkIfIpExists(ndomain,server)) {
			
			gp.setNodeDomain(ndomain);
			gp.nameTriedDate = null;
			gp.maxNameTriedWaitTime = 0;
			log.debug("Domain {} set successfuly for node {}",ndomain,server);
		}
		else {
			
			gp.setNodeDomain(server);
			log.error("Domain {} resolved address {} which is not equals node {}",ndomain,dnsService.getResolvedIPs(),server);
			arrangeFailure();
		}
	}

}
