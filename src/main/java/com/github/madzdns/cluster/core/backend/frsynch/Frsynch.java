package com.github.madzdns.cluster.core.backend.frsynch;

import com.frfra.frsynch.SynchContext;

public class Frsynch {

	static SynchContext synchContext;
	
	public static SynchContext getContext() {
		
		return synchContext;
	}
}
