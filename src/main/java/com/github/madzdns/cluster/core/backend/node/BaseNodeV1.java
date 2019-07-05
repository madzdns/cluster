package com.github.madzdns.cluster.core.backend.node;

import com.github.madzdns.cluster.core.backend.node.dynamic.INFOResponse;

abstract public class BaseNodeV1 implements INode {
	
	private byte status = INFOResponse.RSP_TYPE_NORMAL;
	
	private String errBody = null;
	
	private short version;
	
	private long id = 0;
	
	public BaseNodeV1(long id, short version) {
		
		this.id = id;
		this.version = version;
	}

	@Override
	public short getVersion() {
		// TODO Auto-generated method stub
		return version;
	}

	@Override
	public void setVersion(short version) {
		
		this.version = version;
	}
	
	public byte getStatus() {
		
		return status;
	}
	
	public void setStatus(byte status) {
		
		this.status = status;
	}
	
	public long getId() {
		
		return id;
	}
	
	public void setId(long id) {
		
		this.id = id;
	}
	
	public String getErrBody() {
		
		return errBody;
	}
	
	public void setErrBody(final String errBody) {
		
		this.errBody = errBody;
	}

}
