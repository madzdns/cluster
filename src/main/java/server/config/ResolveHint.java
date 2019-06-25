package server.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "resolve-hint")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResolveHint {
	
	@XmlTransient
	public static byte HAS_FALLBACK = 1;
	
	@XmlTransient
	public static byte HAS_NOT_FALLBACK = 0;
	
	@XmlTransient
	private byte byteType;

	@XmlAttribute(name="clientBackend")
	private String type;
	
	@XmlAttribute(name="fallback")
	private boolean fallback;
	
	@XmlElement(name="field")
	private List<ResolveHintField> fields;

	public String getType() {
		
		return type;
	}

	public void setType(String type) {
		
		this.type = type;
	}

	public List<ResolveHintField> getFields() {
		
		return fields;
	}

	public void setFields(List<ResolveHintField> fields) {
		
		this.fields = fields;
	}

	public boolean isFallback() {
		
		return fallback;
	}

	public void setFallback(boolean fallback) {
		
		this.fallback = fallback;
	}

	public byte getByteType() {
		return byteType;
	}

	public void setByteType(byte byteType) {
		this.byteType = byteType;
	}
	
	@Override
	public String toString() {
		
		return new StringBuilder().append("clientBackend=")
				.append(byteType)
				.append(", fallback=")
				.append(fallback)
				.append(", fields=[")
				.append(fields)
				.append("]")
				.toString();
	}

}
