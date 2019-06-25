package com.github.madzdns.server.http;

public enum HTTPCode {
	
	OK(200),
	Temporary_Redirect(307,"Temporary Redirect"),
	BadRequest(400,"Bad Request"),
	NotFound(404,"Not Found"),
	Use_Proxy(305,"Use Proxy"),
	SeeOther(303,"See Other"),
	PermanentRedirect(308,"Permanent Redirect"),
	MovedPermanently(301,"Moved Permanently"),
	InternalServerError(500,"Internal Server Error");
	
	
	private final int value;
	private final String description;
	
	private HTTPCode(int value, String description) {
		this.value = value;
		this.description = description;
	}

	private HTTPCode(int value) {
		this.value = value;
		this.description = null;
	}
	public int value() {
		return this.value;
	}

	public String description() {
		if (description != null)
			return description;
		else
			return name();
	}

	public static HTTPCode fromString(String strCode) {
		int intCode = Integer.valueOf(strCode);
		for (HTTPCode code : HTTPCode.values()) {
			if (code.value() == intCode)
				return code;
		}
		return HTTPCode.BadRequest;
	}

}
