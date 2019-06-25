package com.github.madzdns.server.core.backend.node.dynamic.codec;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.madzdns.server.core.backend.node.dynamic.INFOResponse;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.server.core.api.Types;
import com.github.madzdns.server.core.backend.BackendAddress;
import com.github.madzdns.server.core.backend.BackendNode;

public class BackendMinaEncoder implements ProtocolEncoder {
	
	@SuppressWarnings("unused")
	private Logger log = LoggerFactory.getLogger(BackendMinaEncoder.class);

	@Override
	public void dispose(IoSession arg0) throws Exception {
		
	}

	@Override
	public void encode(IoSession arg0, Object message, ProtocolEncoderOutput out)
			throws Exception {
		
		if(message instanceof INFOResponse) {
			
			final INFOResponse resp = (INFOResponse) message;
			
			final short mlen = resp.getMessageLength();
			
			final IoBuffer rBUF = IoBuffer.allocate(mlen+Types.ShortBytes);
			
			rBUF.putShort(mlen);
			
			if(mlen==0) {
				
				rBUF.flip();
				out.write(rBUF);
				return;
			}
			
			rBUF.put(resp.getType());
			
			if(resp.getKey()!=null) {
				
				rBUF.putShort((short) resp.getKey().getBytes().length);
				rBUF.put(resp.getKey().getBytes());
			}
			else
				rBUF.putShort((short) 0);
			
			if(resp.getType()==INFOResponse.RSP_TYPE_ERR || 
					resp.getType()==INFOResponse.RSP_TYPE_ERR_NOFATTAL) {
				
				if(resp.getErrBody()!=null) {
					
					rBUF.putShort((short) resp.getErrBody().getBytes().length);
					rBUF.put(resp.getErrBody().getBytes());
				}
				else {
					
					rBUF.putShort((short) 0);
				}
				
				rBUF.flip();
				out.write(rBUF);
				return;
			}
			
			if(resp.getDomain()!=null) {
				
				rBUF.putShort((short)resp.getDomain().getBytes().length);
				rBUF.put(resp.getDomain().getBytes());
			}
			else
				rBUF.putShort((short) 0);
			
			if(resp.getNodeKey()!=null) {
				
				rBUF.putShort((short)resp.getNodeKey().getBytes().length);
				
				rBUF.put(resp.getNodeKey().getBytes());
			}
			else
				
				rBUF.putShort((short) 0);
			
			rBUF.put(resp.isNeedGeoz()?INFOResponse.NEED_GEOZ:INFOResponse.DOES_NOT_NEED_GEOZ);
			
			final List<BackendNode> edges = resp.getEdges();
			
			if(edges!=null) {
				
				rBUF.put((byte)resp.getEdges().size());
				
				for(Iterator<BackendNode> eit = edges.iterator();eit.hasNext();) {
					
					final BackendNode b = eit.next();
					
					if(b.getName() != null) {
					
						rBUF.putShort((short)b.getName().getBytes().length);
						rBUF.put(b.getName().getBytes());
					}
					else {
						
						rBUF.putShort((short)0);
					}
					
					final Set<BackendAddress> addrz = b.getBackendAddrzForDynamicNodes();
					
					if(addrz!=null) {
						
						rBUF.put((byte)addrz.size());
						
						rBUF.put(b.isValid()?INFOResponse.BACKEND_VALID:0);
						
						rBUF.putLong(b.getLastModified());
						
						for(Iterator<BackendAddress> it = addrz.iterator();it.hasNext();) {
							
							final BackendAddress clusterAddr = it.next();
							
							rBUF.put((byte)clusterAddr.getAddress().getAddress().length);
							
							rBUF.put(clusterAddr.getAddress().getAddress());
							
							rBUF.putShort((short)clusterAddr.getPort());
						}
					}
					else
						rBUF.put((byte)0);
				}
			}
			else
				rBUF.put((byte)0);
			
			if(resp.getGeomaps()!=null)
				for(String s:resp.getGeomaps())
					rBUF.put(s.getBytes());
			rBUF.flip();
			out.write(rBUF);
		}
	}
	
}
