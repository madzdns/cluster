package com.github.madzdns.server.core.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPoolService {
	
	private static ExecutorService executor=null;
	private static Object mutex = new Object();

	public ThreadPoolService() {
		
		synchronized (mutex) {
		
			if(executor == null) {
				
				final int THREADS =  Runtime.getRuntime().availableProcessors();
				executor = Executors.newFixedThreadPool(THREADS);
			}
		}
	}
	
	public void call(Runnable run) {
		
		executor.execute(run);
	}
	
	public void shutDown() {
		
		synchronized (mutex) {
			
			if(executor != null) {
				
				executor.shutdown();
				executor = null;
			}
		}
	}
}
