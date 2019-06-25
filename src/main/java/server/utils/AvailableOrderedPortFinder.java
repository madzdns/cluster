package server.utils;

import java.util.Random;

import org.apache.mina.util.AvailablePortFinder;

public class AvailableOrderedPortFinder {
	
	public enum ORDER {
		
		EVEN(0),
		ODD(1);
		
		private final int value;
		
		private ORDER(int value) {
			
			this.value = value;
		}
		
		public int getValue() {
			
			return this.value;
		}
	}
	
	public static final int MAX_PORT_NUMBER = 65535;
	
	public static int findNextPort(int port, ORDER firstOrder, int[] ports) {
		
		if(ports == null) {
			
			return 0;
		}
		
		if(port == 0) {
			
			port = new Random().nextInt(20000) + 1025;
		}
		
		for (int i=0;i<ports.length; i++) {
			
			if(port >= MAX_PORT_NUMBER) {

				return i;
			}
			
			int rp = port % 2;
			
			int ri = i % 2;
			
			if (ri == 0 && rp != firstOrder.getValue()) {
				
				++ port; 
			}
			else if (ri != 0 && rp == firstOrder.getValue()) {
				
				++ port;
			}
			
			for( ; port <= MAX_PORT_NUMBER; port +=2) {
				
				if(AvailablePortFinder.available(port)) {
					
					ports[i] = port;
					
					break;
				}
			}
		}
		
		return ports.length;
	}
}
