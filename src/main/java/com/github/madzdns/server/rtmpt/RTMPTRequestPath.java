package com.github.madzdns.server.rtmpt;

public enum RTMPTRequestPath {

	FCS_IDENT('f'),
	OPEN1('o'),
	IDLE('i'),
	SEND('s'),
	CLOSE('c');
	
	
	private char path;
	
	private RTMPTRequestPath(char path) {
		
		this.path = path;
	}
	
	public char getPath() {
		
		return this.path;
	}
}
