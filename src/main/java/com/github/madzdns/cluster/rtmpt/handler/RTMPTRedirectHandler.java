package com.github.madzdns.cluster.rtmpt.handler;

import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmpt.codec.RTMPTCodecFactory;

import com.github.madzdns.cluster.rtmp.handler.RTMPRedirectHandler;

public class RTMPTRedirectHandler extends RTMPRedirectHandler {

	 protected RTMPTCodecFactory codecFactory;

    /**
     * Setter for codec factory
     *
     * @param factory
     *            Codec factory to use
     */
    public void setCodecFactory(RTMPTCodecFactory factory) {
        this.codecFactory = factory;
    }

    /**
     * Getter for codec factory
     *
     * @return Codec factory
     */
    public RTMPTCodecFactory getCodecFactory() {
        return this.codecFactory;
    }
    
    @Override
    public void connectionOpened(RTMPConnection conn) {
    	/*System.out.println("oooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooooo");*/
    	super.connectionOpened(conn);
    }
}
