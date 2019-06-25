package com.github.madzdns.server.core.backend.node.statics;

import java.util.Iterator;
import java.util.Set;

import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.backend.node.dynamic.impl.Node;

public class HeartbeatWorker {
	
	private static Logger log = LoggerFactory.getLogger(HeartbeatWorker.class);
	private static Scheduler scheduler = null;
	
	
	static {
		
		try {
			
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();
			
		} catch (Exception e) {
			
			log.error("",e);
			System.exit(1);
		}
	}

	public static void addJob(Node node, String domain, String ndomain, int port, int interval) {
		
		try {
			
			JobKey jobKey = new JobKey(node.getServer(), domain);
			
			TriggerKey triggerKey = new TriggerKey(node.getServer());
			
			if(scheduler.checkExists(triggerKey)) {
				
				Trigger t = scheduler.getTrigger(triggerKey);
				
				SimpleTrigger st = (SimpleTrigger) t;
				
				if(st.getRepeatInterval() <= interval) {
					
					log.warn("Heart beat request for exsisting job for {} with repeat intervel {} whitch is greater or equal than {}. So we dont need to replace.",
							node.getServer(), st.getRepeatInterval(), interval);
					return;
				}

				scheduler.deleteJob(jobKey);
			}
			
			log.info("Starting schedule service for core {} for domain {} and host {} with interval {}",
					node.getServer(), domain, ndomain, interval);
			
			JobDataMap dataMap = new JobDataMap();
			dataMap.put(HostHeartbeat.NODE_KEY, node);
			dataMap.put(HostHeartbeat.HOST_KEY, ndomain);
			dataMap.put(HostHeartbeat.PORT_KEY, port);
			dataMap.put(HostHeartbeat.INTERVAL_KEY, interval);
			dataMap.put(HostHeartbeat.NODE_DOMAIN_KEY, domain);
			
			JobDetail jobDetails = JobBuilder.newJob(HostHeartbeat.class)
					.withIdentity(jobKey)
					.usingJobData(dataMap)
					.build();
			
			Trigger trigger = TriggerBuilder.newTrigger()
					.withIdentity(triggerKey)
					.withSchedule(SimpleScheduleBuilder.repeatSecondlyForever(interval))
					.build();
		
			scheduler.scheduleJob(jobDetails, trigger);
		
		} catch (SchedulerException e) {
		
			log.error("",e);
		}
		
	}
	
	public static void removeJobsForDomain(String domain) {
		
		try {
			
			log.debug("Removing all jobs for domain {}", domain);
			
			Set<JobKey> jobs = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(domain));
			for(Iterator<JobKey> it = jobs.iterator(); it.hasNext();) {
				
				JobKey jobKey = it.next();
				scheduler.deleteJob(jobKey);
			}
			
		} catch (SchedulerException e) {
			
			log.error("",e);
		}
	}
}
