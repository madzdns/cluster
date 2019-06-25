package server.config;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "mx")
@XmlAccessorType(XmlAccessType.FIELD)
public class Mx {

	@XmlElement(name="record")
	private List<MxField> fileds;
	
	public List<MxField> getFileds() {
		
		return fileds;
	}
	
	public void setFileds(List<MxField> fileds) {
		
		this.fileds = fileds;
	}
	
	
	@Override
	public String toString() {
		
		return String.valueOf(fileds);
	}
	
	public String toXmlString() {
		
		if(fileds == null)
			return "";
		
		StringBuilder sb = new StringBuilder();
		
		for(int i=0;i<fileds.size();i++) {
			
			sb.append("<mx>")
			.append(fileds.get(i).toXmlString())
			.append("</mx>");
		}
		return sb.toString();
	}
}
