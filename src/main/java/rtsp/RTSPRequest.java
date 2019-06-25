package rtsp;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rtsp.RTSPMessage;
import rtsp.Verb;
/**
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public class RTSPRequest extends RTSPMessage {
	
	private static Logger log = LoggerFactory.getLogger(RTSPRequest.class);
	
	public static int trackIdFromUrl(URI url) {
		
		String urlString = url.toString();
		
		int x = urlString.indexOf('=');
		
		if( x < 0 || x==urlString.length()-1) {
			
			return -1;
		}
		
		try {
			
			return Integer.parseInt(urlString.substring(x+1));
		}
		catch (Exception e) {
			
			log.debug("",e);
			return -1;
		}
	}

	private Verb verb;

	private URI url;
	
	private int trackId = -1;

	/**
	 * 
	 */
	public RTSPRequest() {
		
		super();
		verb = Verb.None;
	}

	public Type getType() {
		
		return Type.TypeRequest;
	}

	public String getVerbString() {
		
		return verb.toString();
	}

	public void setVerb(Verb verb) {
		
		this.verb = verb;
	}

	public Verb getVerb() {
		
		return verb;
	}

	/**
	 * Sets the verb of the request from a string.
	 * 
	 * @param strVerb
	 *        String containing the the verb
	 */
	public void setVerb(String strVerb) {
		
		try {
			
			this.verb = Verb.valueOf(strVerb);
		} catch (Exception e) {
			
			this.verb = Verb.None;
		}
	}

	public void setUrl(URI url) {
		
		this.url = url;
	}

	public URI getUrl() {
		
		return url;
	}

	/**
	 * Return a serialized version of the RTSP request message that will be sent
	 * over the network. The message is in the form:
	 * 
	 * <pre>
	 * [verb] SP [url] SP "RTSP/1.0" CRLF
	 * [headers] CRLF
	 * CRLF 
	 * [buffer]
	 * </pre>
	 */
	public String toString() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(getVerbString() + " ");
		sb.append(url != null ? url : "*");
		sb.append(" RTSP/1.0\r\n");
		sb.append(getHeadersString());

		// Insert a blank line
		sb.append(CRLF);

		if (getBufferSize() > 0) {
			sb.append(getBuffer());
		}

		return sb.toString();
	}
	
	public int getTrackId() {
		
		if(trackId>-1) {
			
			return trackId;
		}
		
		if(url == null)
			
			return trackId;
		
		return trackId = trackIdFromUrl(url);
		
	}

}