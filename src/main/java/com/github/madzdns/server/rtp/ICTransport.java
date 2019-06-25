package com.github.madzdns.server.rtp;

public interface ICTransport {

	public String getDstAddress();
	
	public int[] getDstPorts();
	
	public int[] getLocalPorts();
	
	public void setLocalPorts(int[] ports);
	
	public void setTimeStamp(long timeStamp);
	
	public short getSequence();
	
	public int getUint32Ssrc();
	
	public long getTimeStamp();
	
}
