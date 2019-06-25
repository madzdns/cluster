package server.backend;

import java.net.InetAddress;

public class BackendAddress extends StorableSocketAddress {
	
	public BackendAddress(){}

	public BackendAddress(InetAddress addr, int port) {
		
		super(addr,port);
	}
	
	public BackendAddress(String hostname, int port) {
		
		super(hostname,port);
	}
	
	public boolean isPortEndsWith8() {

		return (getPort() % 10) == 0x8;
	}

}