package rtmpt;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;

import org.apache.commons.codec.binary.Hex;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.red5.server.api.Red5;
import org.red5.server.net.IConnectionManager;
import org.red5.server.net.rtmp.InboundHandshake;
import org.red5.server.net.rtmp.RTMPConnManager;
import org.red5.server.net.rtmp.RTMPConnection;
import org.red5.server.net.rtmp.codec.RTMP;
import org.red5.server.net.rtmp.codec.RTMPProtocolEncoder;
import org.red5.server.net.rtmp.event.Invoke;
import org.red5.server.net.rtmp.message.Constants;
import org.red5.server.net.rtmp.message.Header;
import org.red5.server.net.rtmp.message.Packet;
import org.red5.server.net.rtmp.status.Status;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.net.rtmpt.RTMPTConnection;
import org.red5.server.service.PendingCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import rtmp.RtmpServer;
import rtmpt.handler.RTMPTRedirectHandler;

public class RTMPTConnactionManager {
	
	private final Logger log = LoggerFactory.getLogger(RTMPTConnactionManager.class);

	private static IConnectionManager<RTMPConnection> manager;
	
	private static RTMPTRedirectHandler handler;
	
	private static int targetResponseSize = 32768;
	
	private String sessionId = null;
	
	/*public RTMPTConnactionManager(int targetResponseSize) {
		
		RTMPTConnactionManager.targetResponseSize = targetResponseSize;
	}*/
	
	public byte[] handleOpen(IoSession session, int contentLength) {
		
		if(manager == null) {
			
			manager = RtmpServer.CTX.getBean("red5.core", ApplicationContext.class)
					.getBean("rtmpConnManager", RTMPConnManager.class);
		}
		
		if(handler == null) {
			
			handler = RtmpServer.CTX.getBean("red5.core", ApplicationContext.class)
					.getBean("rtmptRedirectHandler", RTMPTRedirectHandler.class);
		}
		
		RTMPTConnection conn = (RTMPTConnection) manager.createConnection(RTMPTConnection.class);

        if (conn != null) {
            
            // add the connection to the manager
            manager.setConnection(conn);
            // set handler 
            conn.setHandler(handler);
            
            conn.setRemoteAddress(((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress());
            
            //a work around to somehow save local port
            conn.setRemotePort(((InetSocketAddress)session.getLocalAddress()).getPort());
            
            try {
            	
				Field ioSessionField = RTMPTConnection.class.getDeclaredField("ioSession");
				ioSessionField.setAccessible(true);
				ioSessionField.set(conn, session);
				
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				
			}
            
            
            conn.setDecoder(handler.getCodecFactory().getRTMPDecoder());
            conn.setEncoder(handler.getCodecFactory().getRTMPEncoder());
            handler.connectionOpened(conn);
            conn.dataReceived();
            conn.updateReadBytes(contentLength);
            // set thread local reference
            Red5.setConnectionLocal(conn);
            if (conn.getId() != 0) {
          
                // return session id to client
                return (sessionId = conn.getSessionId()).getBytes();
            } else {
                
            	return new byte[] {0};
            }
        } else {
        	
        	return new byte[] {0};
        }
	}
	
	public boolean handleClose() throws IOException {
		
        log.debug("handleClose");

        // get the associated connection
        RTMPTConnection connection = getConnection();
        if (connection != null) {
            log.debug("Pending messges on close: {}", connection.getPendingMessages());
            connection.close();
            
            return true;
            
        } else {
            
        	return false;
        }
	}
	
	public byte[] handleBadRequest(String message) throws IOException {
        log.debug("handleBadRequest {}", message);
        
        try {
        	
        	// create and send a rejected status
            Status status = new Status(StatusCodes.NC_CONNECT_REJECTED, Status.ERROR, message);
            PendingCall call = new PendingCall(null, "onStatus", new Object[] { status });
            Invoke event = new Invoke();
            event.setCall(call);
            Header header = new Header();
            Packet packet = new Packet(header, event);
            header.setDataType(event.getDataType());
            
            RTMPConnection conn = (RTMPConnection) Red5.getConnectionLocal();
            if (conn == null) {
                try {
                    conn = ((RTMPConnManager) manager).createConnectionInstance(RTMPTConnection.class);
                    Red5.setConnectionLocal(conn);
                } catch (Exception e) {
                }
            }
           
            // encode the data
            RTMPProtocolEncoder encoder = new RTMPProtocolEncoder();
            IoBuffer out = encoder.encodePacket(packet);
            
           // byte[] outBytes = new byte[out.remaining()+1];
            
            IoBuffer outBuff = IoBuffer.allocate(out.remaining()+1);
            
            outBuff.put((byte) 0);
            if(out.remaining() > 0) {
            
            	outBuff.put(out);
            }
            
            outBuff.flip();
            
            return out.array();	
        }
        finally {
        	
        	// clear local
            Red5.setConnectionLocal(null);
        }
	}
	
	public byte[] handleIdle(int contentLength) throws IOException {
        log.debug("handleIdle");
        
        // get associated connection
        RTMPTConnection conn = getConnection();
        if (conn != null) {
            conn.dataReceived();
            conn.updateReadBytes(contentLength);
            // return pending
            return returnPendingMessages(conn);
        } else {
            
            return null;
        }
    }
	
	protected byte[] handleSend(int contentLength, byte[] in) {
		
        log.debug("handleSend");
        final RTMPTConnection conn = getConnection();
        if (conn != null) {
            IoSession session = conn.getIoSession();
            // get the handshake from the session
            InboundHandshake handshake = null;
            // put the received data in a ByteBuffer
            int length = contentLength;
            log.trace("Request content length: {}", length);
            
            final IoBuffer message = IoBuffer.wrap(in);

            RTMP rtmp = conn.getState();
            int connectionState = rtmp.getState();
            switch (connectionState) {
                case RTMP.STATE_CONNECT:
                    // we're expecting C0+C1 here
                    //log.trace("C0C1 byte order: {}", message.order());
                    log.debug("decodeHandshakeC0C1 - buffer-size: {} - buffer: {}", message.remaining(), message);
                    // we want 1537 bytes for C0C1
                    if (message.remaining() >= (Constants.HANDSHAKE_SIZE + 1)) {
                        // get the connection type byte, may want to set this on the conn in the future
                        byte connectionType = message.get();
                        log.debug("Incoming C0 connection type: {} - isUnvalidatedConnectionAllowed: {}", connectionType, handler.isUnvalidatedConnectionAllowed());
                        // add the in-bound handshake, defaults to non-encrypted mode
                        handshake = new InboundHandshake(connectionType);
                        handshake.setUnvalidatedConnectionAllowed(handler.isUnvalidatedConnectionAllowed());
                        session.setAttribute(RTMPConnection.RTMP_HANDSHAKE, handshake);
                        // create array for decode
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // copy out 1536 bytes
                        message.get(dst);
                        //log.debug("C1 - buffer: {}", Hex.encodeHexString(dst));
                        // set state to indicate we're waiting for C2
                        rtmp.setState(RTMP.STATE_HANDSHAKE);
                        IoBuffer s1 = handshake.decodeClientRequest1(IoBuffer.wrap(dst));
                        if (s1 != null) {
                            log.trace("S1 byte order: {}", s1.order());
                            conn.writeRaw(s1);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    break;
                case RTMP.STATE_HANDSHAKE:
                    // we're expecting C2 here
                    //log.trace("C2 byte order: {}", message.order());
                    log.debug("decodeHandshakeC2 - buffer-size: {} - buffer: {}", message.remaining(), message);
                    // no connection type byte is supposed to be in C2 data
                    if (message.remaining() >= Constants.HANDSHAKE_SIZE) {
                        // get the handshake
                        handshake = (InboundHandshake) session.getAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        // create array for decode
                        byte[] dst = new byte[Constants.HANDSHAKE_SIZE];
                        // copy
                        message.get(dst);
                        log.trace("Copied {}", Hex.encodeHexString(dst));
                        //if (log.isTraceEnabled()) {
                        //    log.trace("C2 - buffer: {}", Hex.encodeHexString(dst));
                        //}
                        if (handshake.decodeClientRequest2(IoBuffer.wrap(dst))) {
                            log.debug("Connected, removing handshake data and adding rtmp protocol filter");
                            // set state to indicate we're connected
                            rtmp.setState(RTMP.STATE_CONNECTED);
                            
                            if(handshake.useEncryption()) {
                            	
                            	rtmp.setEncrypted(true);
                            	
                            	session.setAttribute(RTMPConnection.RTMPE_CIPHER_IN, handshake.getCipherIn());
                                session.setAttribute(RTMPConnection.RTMPE_CIPHER_OUT, handshake.getCipherOut());
                            }
                            
                            // remove handshake from session now that we are connected
                            session.removeAttribute(RTMPConnection.RTMP_HANDSHAKE);
                        } else {
                            log.warn("Client was rejected due to invalid handshake");
                            conn.close();
                        }
                    }
                    // let the logic flow into connected to catch the remaining bytes that probably contain
                    // the connect call
                case RTMP.STATE_CONNECTED:
                    // decode the objects and pass to received; messages should all be Packet type
                    for (Object obj : conn.decode(message)) {
                        conn.handleMessageReceived(obj);
                    }
                    break;
                case RTMP.STATE_ERROR:
                case RTMP.STATE_DISCONNECTING:
                case RTMP.STATE_DISCONNECTED:
                    // do nothing, really
                    log.debug("Nothing to do, connection state: {}", RTMP.states[connectionState]);
                    break;
                default:
                    throw new IllegalStateException("Invalid RTMP state: " + connectionState);
            }
            
            conn.dataReceived();
            conn.updateReadBytes(length);
            message.clear();
            message.free();
            // return pending messages
            return returnPendingMessages(conn);
            
        } else {
        	
            return null;
        }
    }
	
	protected byte[] returnPendingMessages(RTMPTConnection conn) {
		
        log.debug("returnPendingMessages {}", conn);
        // grab any pending outgoing data
        IoBuffer data = conn.getPendingMessages(targetResponseSize);
        
        if (data != null) {
        	
            try {
            	
            	int data_size = data.remaining();
            	
            	if(data_size > 0) {
            		
            		byte[] data_arr = new byte[data_size+1];
            		
            		data_arr[0] = conn.getPollingDelay();
            		data.get(data_arr, 1, data_size);	
            		return data_arr;
            	}
            	
                return new byte[] {0};
                
            } catch (Exception ex) {
                // using "Exception" is meant to catch any exception that would occur when doing a write
                // this can be an IOException or a container specific one like ClientAbortException from catalina
                log.warn("Exception returning outgoing data", ex);
                conn.close();
                
                return null;
            }
        } else {
            log.debug("No messages to send");
            if (conn.isClosing()) {
                log.debug("Client is closing, send close notification");
                    
                return new byte[] {0};
            } else {
            	
               return new byte[] {conn.getPollingDelay()};
            }
        }
    }
	
	protected RTMPTConnection getConnection() {

        RTMPTConnection conn = (RTMPTConnection) manager.getConnectionBySessionId(sessionId);
        
        if (conn != null) {
            // check for non-connected state
            if (!conn.isDisconnected()) {
                // clear thread local reference
                Red5.setConnectionLocal(conn);
            } else {
                manager.removeConnection(sessionId);
            }
        } else {
            log.warn("Null connection for session id: {}", sessionId);
        }
        
        return conn;
    }

	public String getSessionId() {
		
		return sessionId;
	}
}
