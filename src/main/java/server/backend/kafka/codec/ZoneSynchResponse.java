package server.backend.kafka.codec;

public class ZoneSynchResponse extends ZoneSynchMessage{
	
	public ZoneSynchResponse()
	{
		setMode(ZoneSynchMessage.MODE_SYNCH_ZONE);
	}
	public ZoneSynchResponse(short len)
	{
		super(len);
		setMode(ZoneSynchMessage.MODE_SYNCH_ZONE);
	}
}
