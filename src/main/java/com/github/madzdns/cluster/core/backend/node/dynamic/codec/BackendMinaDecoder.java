package com.github.madzdns.cluster.core.backend.node.dynamic.codec;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.github.madzdns.cluster.core.backend.node.dynamic.INFOMessage;
import com.github.madzdns.cluster.core.backend.node.dynamic.ServiceInfo;
import com.github.madzdns.cluster.core.backend.node.dynamic.SystemInfo;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.madzdns.cluster.core.api.Types;
import com.github.madzdns.cluster.core.config.ResolveHint;
import com.github.madzdns.cluster.core.config.ResolveHintField;
import com.github.madzdns.cluster.core.config.Contact;

public class BackendMinaDecoder extends CumulativeProtocolDecoder{
	
	Logger log = LoggerFactory.getLogger(BackendMinaDecoder.class);
	private static final String DECODER_STATE_KEY = BackendMinaDecoder.class.getName() + ".STATE";

	@Override
	public boolean doDecode(IoSession session, IoBuffer in, ProtocolDecoderOutput out)
			throws Exception {

		INFOMessage message = (INFOMessage)session.getAttribute(DECODER_STATE_KEY);
		
		if(in.remaining()>0) {
			
			if(message==null) {
				
				if(in.remaining()<Types.ShortBytes)
					return false;
				message = new INFOMessage(in.getShort());
				message.setSrc(((InetSocketAddress)session.getRemoteAddress()).getAddress().getHostAddress());
			}
			
			int mlen = message.getLength();
			
			if(in.remaining()>=mlen) {
				
				message.setVersion(in.getShort());
				mlen -= Types.ShortBytes;
				
				final int keysize = in.getShort();
				mlen -= Types.ShortBytes;
				
				if(keysize>0) {
					
					final byte[] key = new byte[keysize];
					in.get(key);
					mlen -= keysize;
					message.setKey(new String(key));	
				}
				
				final int service_count = in.getShort();
				mlen -= Types.ShortBytes;
				ServiceInfo srv = null;
				
				for(int i=0;i<service_count;i++) {
					
					srv = new ServiceInfo();
					srv.port = in.getShort();
					srv.type = in.get();
					srv.value = in.get();
					srv.raise = 0;
					final short raise_count = in.getShort();
					
					if(raise_count>0) {
						
						final byte[] raise = new byte[raise_count];
						in.get(raise);
						srv.raise = Double.valueOf(new String(raise));
					}
					
					message.addServiceInfoz(srv);
					mlen -= (Types.ShortBytes + Types.Bytes + Types.Bytes + Types.ShortBytes + raise_count);
				}
				
				final int sys_count = in.getShort();
				mlen -= Types.ShortBytes;
				SystemInfo sys = null;
				
				for(int i=0;i<sys_count;i++) {
					
					sys = new SystemInfo();
					sys.type = in.get();
					sys.value = in.get();
					sys.xthreshold = in.get();
					sys.nthreshold = in.get();
					sys.critical = in.get();
					message.addSysInfoz(sys);
					mlen -= (Types.Bytes + Types.Bytes + Types.Bytes+ Types.Bytes + Types.Bytes);
				}
				
				int contact_count = in.getShort();
				
				mlen -= Types.ShortBytes;
				
				Contact c = null;
				
				short len = 0;
				
				byte[] bytes = null;
				
				List<Contact> cz = new ArrayList<Contact>();
				
				for(int i=0;i<contact_count;i++) {
					
					c = new Contact();
					len = in.get();
					bytes = new byte[len];
					in.get(bytes);
					c.setUsername(new String(bytes));
					
					mlen -= (Types.Bytes+len);
					
					len = in.get();
					bytes = new byte[len];
					in.get(bytes);
					c.setPassword(new String(bytes));
					
					mlen -= (Types.Bytes+len);
					
					len = in.get();
					bytes = new byte[len];
					in.get(bytes);
					c.setType(new String(bytes));
					
					mlen -= (Types.Bytes+len);
					
					len = in.getShort();
					bytes = new byte[len];
					in.get(bytes);
					c.setValue(new String(bytes));
					
					mlen -= (Types.ShortBytes+len);
					
					cz.add(c);
				}
				
				if(cz.size()>0)
					message.setContacts(cz);
				else
					cz = null;
				
				short domain_len = in.getShort();
				mlen -= Types.ShortBytes;
				
				if(domain_len>0) {
					
					final byte[] domain = new byte[domain_len];
					in.get(domain);
					mlen -= domain_len;
					message.setDomain(new String(domain));
				}
				
				//using domain variable to get ndomain. just to not define a new one
				domain_len = in.getShort();
				mlen -= Types.ShortBytes;
				if(domain_len>0) {
					
					final byte[] ndomain = new byte[domain_len];
					in.get(ndomain);
					mlen -= domain_len;
					message.setNdomain(new String(ndomain));
				}
				
				final short interval = in.getShort();
				mlen -= Types.ShortBytes;
				if(interval>0)
					message.setInterval(interval);
				
				byte auto = in.get();
				mlen -= Types.Bytes;
				message.setAuto(auto==INFOMessage.ISAUTO);
				
				//using auto variable to get need backends
				auto = in.get();
				mlen -= Types.Bytes;
				message.setNeedBackends(auto==INFOMessage.NEED_BACKENDS);
				
				message.setNoSys(in.get());
				mlen -= Types.Bytes;
				
				message.setNoGeo(in.get());
				mlen -= Types.Bytes;
				
				//USING auto variable to get hints count
				auto = in.get();
				mlen -= Types.Bytes;
				
				if(auto>0) {
					
					byte hintAttributes;
					byte fieldType;
					
					List<ResolveHint> resolveHints = new ArrayList<ResolveHint>();
					
					ResolveHint hint = null;
					
					for(int i=0; i< auto; i++) {

						hint = new ResolveHint();
						
						//fallback
						hintAttributes = in.get();
						mlen -= Types.Bytes;
						hint.setFallback(hintAttributes==ResolveHint.HAS_FALLBACK?true:false);
						
						//type value
						hintAttributes = in.get();
						mlen -= Types.Bytes;
						hint.setByteType(hintAttributes);
						
						resolveHints.add(hint);
						
						//number of fields
						hintAttributes = in.get();
						mlen -= Types.Bytes;
						
						if(hintAttributes > 0 ) {
							
							final List<ResolveHintField> hintFileds = new ArrayList<ResolveHintField>();
							ResolveHintField hintField = null;
							
							for(int j=0; j < hintAttributes; j++) {
								
								hintField = new ResolveHintField();
								
								fieldType = in.get();
								mlen -= Types.Bytes;
								
								hintField.setByteType(fieldType);
								
								//value lenghth
								fieldType = in.get();
								mlen -= Types.Bytes;
								
								if(fieldType > 0) {
									
									final byte[] hintFiledValue = new byte[fieldType];
									in.get(hintFiledValue);
									mlen -= fieldType;
									hintField.setValue(new String(hintFiledValue));
								}
								
								hintFileds.add(hintField);
							}
							
							hint.setFields(hintFileds);
						}
					}
					
					message.setResolveHints(resolveHints);
				}
				
				if(mlen==0) {
					
					out.write(message);
					session.removeAttribute(DECODER_STATE_KEY);
					message = null;
					cz = null;
					sys = null;
					srv = null;
					c = null;
					return true;
				}
				
				final byte gfactor_len = in.get();
				mlen -= Types.Bytes;
				
				if(gfactor_len>0) {
					
					final byte[] gfactor = new byte[gfactor_len];
					mlen -= gfactor_len;
					in.get(gfactor);
					message.setGfactor(new String(gfactor));
				}
				
				if(mlen==0) {
					
					out.write(message);
					session.removeAttribute(DECODER_STATE_KEY);
					message = null;
					message = null;
					cz = null;
					sys = null;
					srv = null;
					c = null;
					return true;
				}
				
				final byte[] geoz = new byte[mlen];
				in.get(geoz);
				message.setGeomaps(new String(geoz));
				
				out.write(message);
				session.removeAttribute(DECODER_STATE_KEY);
				message = null;
				message = null;
				cz = null;
				sys = null;
				srv = null;
				c = null;
				return true;
			}
		}
		session.setAttribute(DECODER_STATE_KEY, message);
		return false;
	}

	@Override
	public void dispose(IoSession arg0) throws Exception {
		
	}

	@Override
	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
			throws Exception {
		
	}
}
