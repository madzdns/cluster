package server;

import http.HttpServer;

import java.io.IOException;
import java.util.TimerTask;

import com.frfra.frsynch.ClusterNode;

import server.config.Bind;
import server.config.Config;
import rtmp.RtmpServer;
import rtsp.RtspServer;
import server.backend.frsynch.Frsynch;
import server.backend.frsynch.SynchServer;
import server.backend.kafka.KafkaServer;
import server.backend.node.BackendService;
import server.backend.node.dynamic.metrics.BackendMetricServer;
import server.service.ClockWork;
import server.service.Monitor;
import server.wsi.Admin;
import server.wsi.WebService;
import dns.DnsServer;

public class Server implements IServer {

	public void start() throws IOException {
		
		Config.getZoneResz(null);
		
		Bind synchBindings = null;
		Bind backendBindings = null;
		
		for(Bind bind:Config.getApp().getBinds().getBinds())
			
			if(bind.getService().equalsIgnoreCase(Bind.DNSSERVICE)) {
				
				new DnsServer(bind).start();
				
			}else if(bind.getService().equalsIgnoreCase(Bind.RTMPSERVICE)) {
				
				new RtmpServer(bind).start();
				
			}else if(bind.getService().equalsIgnoreCase(Bind.RTSPSERVICE)) {
				
				new RtspServer(bind).start();
				
			}else if(bind.getService().equalsIgnoreCase(Bind.HTTPSERVICE)) {
				
				new HttpServer(bind).start();
			}
			else if(bind.getService().equalsIgnoreCase(Bind.BACKENDSERVICE)) {
				
				backendBindings = bind;
				
				if(synchBindings != null && backendBindings != null) {
					
					new SynchServer(synchBindings, backendBindings).start();
				}

				new BackendService(bind).start();
				new BackendMetricServer(bind).start();
			}
			else if(bind.getService().equalsIgnoreCase(Bind.WEBSERVICE)) {
				
				WebService service = new WebService(bind);
				service.addController(Admin.class);
				service.start();
			}
			else if(bind.getService().equalsIgnoreCase(Bind.SYNCHSERVICE))
				
				if(Config.getApp().isKafkaCluster()) {
					
					new KafkaServer(bind).start();
				}
				else {
					
					synchBindings = bind;
					
					if(synchBindings != null && backendBindings != null) {
						
						new SynchServer(synchBindings, backendBindings).start();
					}
				
					//new SynchServer(bind).start();
				}
		
		ClusterNode me = Frsynch.getContext().getMyInfo();
		
		//Config.reloadFixeds();
		//ZoneManager.setupZones();
		
		Config.setMonitor(new ClockWork(me.getMonitorDelay(),me.getMonitorInterval(),new TimerTask() {
			
			@Override
			public void run() {

				Monitor.doMonitor();
			}
		},"monitor"));
		
		Config.setReport(new ClockWork(me.getReportDelay(),me.getReportInterval(),new TimerTask() {
			
			@Override
			public void run() {
				
				Monitor.healthCheck();
			}
		},"report"));
		
	}
}
