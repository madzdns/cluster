package server.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class ServerCodecFactory implements ProtocolCodecFactory{

	private ProtocolDecoder decoder;
	private ProtocolEncoder encoder;
	
	public ServerCodecFactory(ProtocolDecoder decoder,ProtocolEncoder encoder)
	{
		this.decoder = decoder;
		this.encoder = encoder;
	}
	
	@Override
	public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
		
		return decoder;
	}

	@Override
	public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
		
		return encoder;
	}

}
