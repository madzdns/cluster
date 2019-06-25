package com.github.madzdns.server.core.service;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClockWork {
	
	/*
	 * I was about to fix time delay of
	 * RCPT request failure with defining
	 * FAIR_MONITOR_INTERVAL but realized its
	 * not that easy
	 */

	/**
	 * Amount of fair monitor interval in seconds 
	 */
	public static final short FAIR_MONITOR_INTERVAL = 180; 
	
	private Logger log = LoggerFactory.getLogger(ClockWork.class);
	
	private short delay;
	
	private short interval;
	
	private Timer timer;
    
    public ClockWork(final short delay, final short interval, final TimerTask task, final String type) {
    	
    	if(interval > 0 ) {
    		
    		this.delay = delay;
    		
    		this.interval = interval;
    		
    		log.info("Starting {} clockwork [delay:{}, interval:{}]",type, delay, interval);
        	timer = new Timer();
        	timer.scheduleAtFixedRate(task,(delay+1 /*making sure we have always enough delay*/)*1000,interval*1000);	
    	}
    }

	public short getInterval() {
		
		return interval;
	}

	public short getDelay() {
		
		return delay;
	}
	
	public void cancle() {

		if(timer != null) {
		
			timer.cancel();
			
			timer.purge();
		}
	}
}
