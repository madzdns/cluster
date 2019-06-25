package server.backend.node.statics.impl;

import server.backend.node.BaseNodeV1;
import server.backend.node.INode;

public class HostNode extends BaseNodeV1 {

	private boolean needBackends = false;
	
	private INode underliyingNode = null;
	
	public HostNode(long id, short version) {
		super(id, version);
	}

	public boolean isNeedBackends() {
		return needBackends;
	}

	public void setNeedBackends(boolean needBackends) {
		this.needBackends = needBackends;
	}

	public INode getUnderliyingNode() {
		return underliyingNode;
	}

	public void setUnderliyingNode(INode underliyingNode) {
		this.underliyingNode = underliyingNode;
	}

}
