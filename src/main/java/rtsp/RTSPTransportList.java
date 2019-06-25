package rtsp;

/*
 * RED5 Open Source Flash Server - http://www.osflash.org/red5
 * 
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   Copyright (C) 2005 - Matteo Merli - matteo.merli@gmail.com            *
 *                                                                         *
 ***************************************************************************/

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represent a list of transport headers.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public class RTSPTransportList {

	public static Map<String,RTSPTransport> transportList = new ConcurrentHashMap<String,RTSPTransport>();

	/**
	 * Constructor. Creates a list of transport clientBackend.
	 */
	/*
	public RTSPTransportList()
	{
		transportList = new ArrayList<RTSPTransport>();
	}
	public RTSPTransportList(String transportHeader) {
		transportList = new ArrayList<RTSPTransport>();

		for (String transport : transportHeader.split(",")) {
			transportList.add(new RTSPTransport(transport));
		}
	}
*/
	public static Collection<RTSPTransport> getList() {
		
		return transportList.values();
	}

	public static RTSPTransport get(int index) {
		
		return transportList.get(index);
	}
	
	public static RTSPTransport get(String sessionid) {
		
		return transportList.get( sessionid);
	}
	
	public static RTSPTransport get(RTSPSession session) {
		
		return transportList.get(session.getId());
	}

	/**
	 * @return The number of transports defined.
	 */
	public static int count() {
		
		return transportList.size();
	}

	public String toString() {
		
		StringBuilder buf = new StringBuilder();
		int i = 0;
		for (RTSPTransport t : transportList.values()) {
			if (i++ != 0)
				buf.append(",");
			buf.append(t.toString());
		}
		return buf.toString();
	}
	
	public static void append(String sessionid,RTSPTransport rtsptransport) {
		
		transportList.put(sessionid, rtsptransport);
	}
	
	public static void append(RTSPSession session,RTSPTransport rtsptransport) {
		
		transportList.put(session.getId(), rtsptransport);
	}
	
	public static RTSPTransport suspend(String sessid) {
		
		return transportList.remove(sessid);
	}
	
	public static RTSPTransport suspend(RTSPSession session) {
		
		return transportList.remove(session.getId());
	}

}
