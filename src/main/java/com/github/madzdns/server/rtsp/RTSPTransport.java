package com.github.madzdns.server.rtsp;

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

import java.net.URI;
import java.util.Date;

import org.apache.mina.core.session.IoSession;
import org.red5.io.object.UnsignedInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.rtp.ICTransport;

/**
 * Parse the RTSP Transport header field. Reference Grammar:
 * 
 * <pre>
 *         Transport           =    &quot;Transport&quot; &quot;:&quot;
 *                                  1\#transport-spec
 *         transport-spec      =    transport-protocol/profile[/lower-transport]
 *                                  *parameter
 *         transport-protocol  =    &quot;RTP&quot;
 *         profile             =    &quot;AVP&quot;
 *         lower-transport     =    &quot;TCP&quot; | &quot;UDP&quot;
 *         parameter           =    ( &quot;unicast&quot; | &quot;multicast&quot; )
 *                             |    &quot;;&quot; &quot;destination&quot; [ &quot;=&quot; address ]
 *                             |    &quot;;&quot; &quot;interleaved&quot; &quot;=&quot; channel [ &quot;-&quot; channel ]
 *                             |    &quot;;&quot; &quot;append&quot;
 *                             |    &quot;;&quot; &quot;ttl&quot; &quot;=&quot; ttl
 *                             |    &quot;;&quot; &quot;layers&quot; &quot;=&quot; 1*DIGIT
 *                             |    &quot;;&quot; &quot;port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;client_port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;server_port&quot; &quot;=&quot; port [ &quot;-&quot; port ]
 *                             |    &quot;;&quot; &quot;ssrc&quot; &quot;=&quot; ssrc
 *                             |    &quot;;&quot; &quot;mode&quot; = &lt;&quot;&gt; 1\#mode &lt;&quot;&gt;
 *         ttl                 =    1*3(DIGIT)
 *         port                =    1*5(DIGIT)
 *         ssrc                =    8*8(HEX)
 *         channel             =    1*3(DIGIT)
 *         address             =    host
 *         mode                =    &lt;&quot;&gt; *Method &lt;&quot;&gt; | Method
 *      
 *      
 *         Example:
 *           Transport: RTP/AVP;multicast;ttl=127;mode=&quot;PLAY&quot;,
 *                      RTP/AVP;unicast;client_port=3456-3457;mode=&quot;PLAY&quot;
 * </pre>
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public class RTSPTransport implements ICTransport{

	private static Logger log = LoggerFactory.getLogger(RTSPTransport.class);

	public enum TransportProtocol {
		
		None, RTP, RDT, RAW
	}

	public enum Profile {
		
		None, AVP
	}

	public enum LowerTransport {
		
		None, TCP, UDP
	}

	public enum DeliveryType {
		
		None, unicast, multicast
	}
	
	private short sequence;

	TransportProtocol transportProtocol;

	Profile profile;

	LowerTransport lowerTransport;

	DeliveryType deliveryType;

	String dstAddress;

	String interleaved;
	
	IoSession session;

	int layers;

	boolean append;

	int ttl;

	int[] port = new int[2];

	int[] dstPorts = new int[2];

	int[] localPorts = new int[2];

	String ssrc;

	String mode;

	String source;
	
	UnsignedInt int32Ssrc;
	
	long timeStamp;
	
	ITransportHandler transportHandler;
	
	URI url;

	/**
	 * Constructor. Creates a RTSPTransport object from a transport header
	 * string.
	 */
	public RTSPTransport(String transport,URI url) {
		
		this.url = url;
		
		transportProtocol = TransportProtocol.None;
		profile = Profile.None;
		lowerTransport = LowerTransport.None;
		deliveryType = DeliveryType.None;
		dstAddress = null;
		
		interleaved = null;
		
		layers = 0;
		
		append = false;
		
		ttl = 0;
		
		port[0] = 0;
		
		port[1] = 0;
		
		dstPorts[0] = 0;
		
		dstPorts[1] = 0;
		
		localPorts[0] = 0;
		
		localPorts[1] = 0;
		
		ssrc = null;
		
		mode = null;
		
		source = null;
		
		sequence = (short) (new Date().getTime() & 0xFF);
		
		parseTransport(transport);
		
		if (transport.compareToIgnoreCase(this.toString()) != 0) {
			
			log.warn("Transport header incorrectly parsed.");
		}
	}

	private void parseTransport(String transport) {
		
		for (String tok : transport.split(";")) {

			// First check for the transport protocol
			if (tok.startsWith("RTP") || tok.startsWith("RDT")) {
				
				String[] tpl = tok.split("/");
				
				transportProtocol = TransportProtocol.valueOf(tpl[0]);
				
				if (tpl.length > 1)
					
					profile = Profile.valueOf(tpl[1]);
				
				if (tpl.length > 2)
					
					lowerTransport = LowerTransport.valueOf(tpl[2]);
				
				continue;
			}

			if (tok.compareToIgnoreCase("unicast") == 0)
				
				deliveryType = DeliveryType.unicast;
			
			else if (tok.compareToIgnoreCase("multicast") == 0)
				
				deliveryType = DeliveryType.multicast;
			
			else if (tok.startsWith("destination"))
				
				setDstAddress(_getStrValue(tok));
			
			else if (tok.startsWith("interleaved"))
				
				setInterleaved(_getStrValue(tok));
			
			else if (tok.startsWith("append"))
				
				setAppend(true);
			
			else if (tok.startsWith("layers"))
				
				setLayers(Integer.valueOf(_getStrValue(tok)));
			
			else if (tok.startsWith("ttl"))
				
				setTTL(Integer.valueOf(_getStrValue(tok)));
			
			else if (tok.startsWith("port"))
				
				setPort(_getPairValue(tok));
			
			else if (tok.startsWith("client_port"))
				
				setDstPorts(_getPairValue(tok));
			
			else if (tok.startsWith("server_port"))
				
				setLocalPorts(_getPairValue(tok));
			
			else if (tok.startsWith("ssrc"))
				
				setSSRC(_getStrValue(tok));
			
			else if (tok.startsWith("mode"))
				
				setMode(_getStrValue(tok));
			
			else if (tok.startsWith("source"))
				
				setSource(_getStrValue(tok));
		}
	}

	@Override
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		
		sb.append(transportProtocol);
		
		if (profile != Profile.None) {
			
			sb.append("/").append(profile);
			
			if (lowerTransport != LowerTransport.None)
				
				sb.append("/").append(lowerTransport);
		}
		
		if (deliveryType != DeliveryType.None)
			
			sb.append(";").append(deliveryType);
		
		if (dstAddress != null)
			
			sb.append(";destination=").append(dstAddress);
		
		if (interleaved != null)
			
			sb.append(";interleaved=").append(interleaved);
		
		if (append)
			
			sb.append(";append");
		
		if (layers > 0)
			
			sb.append(";layers=").append(layers);
		
		if (ttl > 0)
			
			sb.append(";ttl=").append(ttl);
		
		if(port!= null && port.length > 0 ) {
			
			boolean port_seted = false;
			
			if (port[0] > 0) {
				
				port_seted = true;
				sb.append(";port=").append(port[0]);
			}
			
			if(port.length >1 && port[1] >0) {
				
				if(port_seted) {
					
					sb.append("-").append(port[1]);
				}
				else {
					
					sb.append(";port=").append(port[1]);
				}
			}
		}
		
		if(dstPorts!=null && dstPorts.length>0) {
			
			boolean client_port_seted = false;
			
			if (dstPorts[0] > 0) {
				
				sb.append(";client_port=").append(dstPorts[0]);
				
				client_port_seted = true;
			}
			
			if(dstPorts.length>1 && dstPorts[1] > 0) {
				
				if(client_port_seted) {
					
					sb.append("-")
					.append(dstPorts[1]);
				}
				else {
					
					sb.append(";client_port=").append(dstPorts[1]);
				}
				
			}
		}
			
		
		if(localPorts != null && localPorts.length>0) {
			
			boolean srv_port_seted = false;
			
			if (localPorts[0] > 0) {
				
				srv_port_seted = true;
				
				sb.append(";server_port=").append(localPorts[0]);
			}
			
			if (localPorts.length>1 && localPorts[1] > 0) {
				
				if( srv_port_seted) {
					
					sb.append("-")
					.append(localPorts[1]);
				}
				else {
					
					sb.append(";server_port=").append(localPorts[1]);	
				}
			}	
		}
			
		if (ssrc != null)
			
			sb.append(";ssrc=").append(ssrc);
		
		if (source != null)
			
			sb.append(";source=").append(source);
		
		if (mode != null)
			
			sb.append(";mode=").append(mode);
		
		return sb.toString();
	}

	/**
	 * @return Returns the append.
	 */
	public boolean isAppend() {
		
		return append;
	}

	/**
	 * @param append
	 *        The append to set.
	 */
	public void setAppend(boolean append) {
		
		this.append = append;
	}

	/**
	 * @return Returns the client_port.
	 */
	@Override
	public int[] getDstPorts() {
		
		return dstPorts;
	}

	/**
	 * @param client_port
	 *        The client_port to set.
	 */
	public void setDstPorts(int[] client_port) {
		
		this.dstPorts = client_port;
	}

	/**
	 * @return Returns the deliveryType.
	 */
	public DeliveryType getDeliveryType() {
		
		return deliveryType;
	}

	/**
	 * @param deliveryType
	 *        The deliveryType to set.
	 */
	public void setDeliveryType(DeliveryType deliveryType) {
		
		this.deliveryType = deliveryType;
	}

	/**
	 * @return Returns the destination.
	 */
	@Override
	public String getDstAddress() {
		
		return dstAddress;
	}

	/**
	 * @param destination
	 *        The destination to set.
	 */
	public void setDstAddress(String destination) {
		
		this.dstAddress = destination;
	}

	/**
	 * @return Returns the interleaved.
	 */
	public String getInterleaved() {
		
		return interleaved;
	}

	/**
	 * @param interleaved
	 *        The interleaved to set.
	 */
	public void setInterleaved(String interleaved) {
		
		this.interleaved = interleaved;
	}

	/**
	 * @return Returns the layers.
	 */
	public int getLayers() {
		
		return layers;
	}

	/**
	 * @param layers
	 *        The layers to set.
	 */
	public void setLayers(int layers) {
		
		this.layers = layers;
	}

	/**
	 * @return Returns the lowerTransport.
	 */
	public LowerTransport getLowerTransport() {
		
		return lowerTransport;
	}

	/**
	 * @param lowerTransport
	 *        The lowerTransport to set.
	 */
	public void setLowerTransport(LowerTransport lowerTransport) {
		
		this.lowerTransport = lowerTransport;
	}

	/**
	 * @return Returns the mode.
	 */
	public String getMode() {
		
		return mode;
	}

	/**
	 * @param mode
	 *        The mode to set.
	 */
	public void setMode(String mode) {
		
		this.mode = mode;
	}

	/**
	 * @return Returns the port.
	 */
	public int[] getPort() {
		
		return port;
	}

	/**
	 * @param port
	 *        The port to set.
	 */
	public void setPort(int[] port) {
		
		this.port = port;
	}

	/**
	 * @return Returns the profile.
	 */
	public Profile getProfile() {
		
		return profile;
	}

	/**
	 * @param profile
	 *        The profile to set.
	 */
	public void setProfile(Profile profile) {
		
		this.profile = profile;
	}

	/**
	 * @return Returns the server_port.
	 */
	@Override
	public int[] getLocalPorts() {
		
		return localPorts;
	}

	/**
	 * @param server_port
	 *        The server_port to set.
	 */
	public void setLocalPorts(int[] server_port) {
		
		this.localPorts = server_port;
	}

	/**
	 * @return Returns the ssrc.
	 */
	public String getSSRC() {
		
		return ssrc;
	}
	
	public UnsignedInt getSSRCAsUint() {
		
		if( int32Ssrc != null)
			
			return int32Ssrc;
		
		if(ssrc == null)
			
			return int32Ssrc = new UnsignedInt(0);
		
		return int32Ssrc = new UnsignedInt(Long.parseLong(ssrc, 16));
	}
	
	public int getUint32Ssrc() {
		
		return getSSRCAsUint().intValue();
	}

	/**
	 * @param ssrc
	 *        The ssrc to set.
	 */
	public void setSSRC(String ssrc) {
		
		int32Ssrc = null;
		
		this.ssrc = ssrc;
	}

	/**
	 * @param ssrc
	 *        The ssrc to set.
	 */
	public void setSSRC(long ssrc) {
		
		int32Ssrc = null;
		
		this.ssrc = Long.toHexString(ssrc & 0xFFFFFFFFL).toUpperCase();
	}

	/**
	 * @return Returns the transportProtocol.
	 */
	public TransportProtocol getTransportProtocol() {
		
		return transportProtocol;
	}

	/**
	 * @param transportProtocol
	 *        The transportProtocol to set.
	 */
	public void setTransportProtocol(TransportProtocol transportProtocol) {
		
		this.transportProtocol = transportProtocol;
	}

	/**
	 * @return Returns the ttl.
	 */
	public int getTTL() {
		
		return ttl;
	}

	/**
	 * @param ttl
	 *        The ttl to set.
	 */
	public void setTTL(int ttl) {
		
		this.ttl = ttl;
	}

	public void setSource(String source) {
		
		this.source = source;
	}

	public String getSource() {
		
		return source;
	}

	/**
	 * Get the value part in a string like:
	 * 
	 * <pre>
	 * key = value
	 * </pre>
	 * 
	 * @param str
	 *        the content string
	 * @return a String containing only the value
	 */
	private static String _getStrValue(String str) {
		
		String[] list = str.split("=");
		
		if (list.length != 2)
			
			return null;

		return list[1];
	}

	/**
	 * Get the value part in a string like:
	 * 
	 * <pre>
	 * key = 6344 - 6345
	 * </pre>
	 * 
	 * @param str
	 *        the content string
	 * @return a int[2] containing only the value
	 */
	private static int[] _getPairValue(String str) {
		
		int[] pair = { 0, 0 };
		
		String[] list = str.split("=");
		
		if (list.length != 2)
			
			return pair;

		try {
			
			pair[0] = Integer.parseInt(list[1].split("-")[0]);
			pair[1] = Integer.parseInt(list[1].split("-")[1]);

			// log.debug("Client ports: " + 1);
			// Integers.parse();

		} catch (Exception e) {}
		
		return pair;
	}
	
	public void setSession(IoSession session) {
		
		this.session = session;
	}
	
	public IoSession getSession() {
		
		return this.session;
	}
	
	public void setSequence(short seq) {
		
		this.sequence = seq;
	}
	
	@Override
	public short getSequence() {
		
		return this.sequence;
	}

	@Override
	public long getTimeStamp() {
		
		return timeStamp;
	}

	@Override
	public void setTimeStamp(long timestamp) {
		
		this.timeStamp = timestamp;
	}

	public ITransportHandler getTransportHandler() {
		
		return transportHandler;
	}

	public void setTransportHandler(ITransportHandler transportHandler) {
		
		this.transportHandler = transportHandler;
	}
	
	public String createTransportInfo() {
		
		StringBuilder sb = new StringBuilder();
		return sb.append("url=").append(url).append(";seq=")
		.append(sequence).append(";rtptime=").append(timeStamp).toString();
		
	}
}
