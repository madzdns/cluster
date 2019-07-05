package com.github.madzdns.cluster.core.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class NetHelper {

	private static List<InetAddress> addresses = null;
	
	public static List<InetAddress> getAllAddresses() throws SocketException
	{
		if(addresses!=null)
			return addresses;
		addresses = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netint : Collections.list(nets))
        {
        	Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
        	for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                addresses.add(inetAddress);
            }
        }
        if(addresses.size()==0)
        	addresses = null;
        return addresses;
	}
}
