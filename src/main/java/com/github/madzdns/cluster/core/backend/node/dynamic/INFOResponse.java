package com.github.madzdns.cluster.core.backend.node.dynamic;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.github.madzdns.cluster.core.api.Types;
import com.github.madzdns.cluster.core.backend.BackendAddress;
import com.github.madzdns.cluster.core.backend.BackendNode;

public class INFOResponse extends INFOMessage{
	
	public static final byte NEED_GEOZ = 1;
	public static final byte DOES_NOT_NEED_GEOZ = 0;
	
	public static final byte RSP_TYPE_NORMAL = 0;
	public static final byte RSP_TYPE_ERR = 1;
	
	public static final byte RSP_TYPE_ERR_NOFATTAL = 2;
	
	public static final byte BACKEND_VALID = 1;
	
	private boolean needGeoz;
	
	private byte type = RSP_TYPE_NORMAL;
	
	private String errBody;
	
	private List<BackendNode> edges = null;
	
	public INFOResponse(short len)
	{
		super(len);
	}
	
	public INFOResponse(){}
	
	@Override
	public short getMessageLength()
	{
		//key
		short s = (short)(Types.ShortBytes);

		if(getKey()!=null)
			
			s+=(short)(getKey().getBytes().length);
		
		//clientBackend
		s += Types.Bytes;
		
		//if there is error
		if(getType()==RSP_TYPE_ERR || getType()==RSP_TYPE_ERR_NOFATTAL)
		{
			s += (short)(Types.ShortBytes);
			
			if(getErrBody()!=null)
				
				s+= (short)(getErrBody().getBytes().length);
			
			return s;
		}
		
		//domain
		s += (short)(Types.ShortBytes);
		if(getDomain()!=null)
			s+=(short)(getDomain().getBytes().length);
		
		//nodeKey
		s += (short)(Types.ShortBytes);
		if(getNodeKey()!=null)
			s += (short)(getNodeKey().getBytes().length);
		
		//needGeoz
		s += Types.Bytes;
		
		//backends
		s += Types.Bytes;
		List<BackendNode> edges = getEdges();
		if(edges!=null)
		{
			for(Iterator<BackendNode> eit = edges.iterator();eit.hasNext();)
			{
				BackendNode b = eit.next();
				
				//backend name length
				s += Types.ShortBytes;
				
				if(b.getName() != null) {
				
					s+= b.getName().getBytes().length;
				}
				
				//addrz count
				s += Types.Bytes;
				Set<BackendAddress> addrz = b.getBackendAddrzForDynamicNodes();
				
				if(addrz!=null)
				{
					//status
					s += Types.Bytes;
					//last modified
					s += Types.LongBytes;
					
					for(Iterator<BackendAddress> it = addrz.iterator();it.hasNext();)
					{
						
						BackendAddress bsocket = it.next();
						s += (Types.Bytes + bsocket.getAddress().getAddress().length + Types.ShortBytes);
					}
				}
			}
		}
		
		if(getGeomaps()!=null)
			for(String geo:getGeomaps())
				s += geo.getBytes().length;
		return s;
	}
	
	public boolean isNeedGeoz() {
		
		return needGeoz;
	}
	
	public void setNeedGeoz(final boolean needGeoz) {
		
		this.needGeoz = needGeoz;
	}
	
	public byte getType() {
		
		return type;
	}
	public void setType(final byte type) {
		
		this.type = type;
	}
	public String getErrBody() {
		
		return errBody;
	}
	public void setErrBody(final String errBody) {
		
		this.errBody = errBody;
	}

	public List<BackendNode> getEdges() {
		
		return edges;
	}

	public void setEdges(final List<BackendNode> edges) {
		
		this.edges = edges;
	}
}