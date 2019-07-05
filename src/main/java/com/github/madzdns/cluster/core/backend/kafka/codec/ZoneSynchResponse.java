package com.github.madzdns.cluster.core.backend.kafka.codec;

public class ZoneSynchResponse extends ZoneSynchMessage{
	
	public ZoneSynchResponse()
	{
		setMode(MODE_SYNCH_ZONE);
	}
	public ZoneSynchResponse(short len)
	{
		super(len);
		setMode(MODE_SYNCH_ZONE);
	}
}
