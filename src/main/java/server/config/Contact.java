package server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name = "contact")
@XmlAccessorType(XmlAccessType.FIELD)
public class Contact {

	@XmlAttribute(name="t")
	private String type="";

	@XmlAttribute(name="v")
	private String value="";
	
	@XmlValue
	private int count;
	
	@XmlAttribute(name="u")
	private String username="";
	
	@XmlAttribute(name="p")
	private String password="";
	
	public String getUsername() {
		
		return username;
	}

	public void setUsername(String username) {
		
		this.username = username;
	}

	public String getPassword() {
		
		return password;
	}

	public void setPassword(String password) {
		
		this.password = password;
	}

	@XmlTransient
	private int takedCount;
	
	public int getTakedCount() {
		
		return this.takedCount;
	}
	
	public int incTakedCount() {
		
		return ++ this.takedCount;
	}
	
	public int decTakedCount() {
		
		if(takedCount == 0) {
			
			return 0;
		}
		return -- this.takedCount;
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof Contact) {
			
			Contact c = (Contact) obj;
			
			try{
				
				if(c.getValue().equals(this.getValue())
						&&c.getType().equals(this.getType()))
					return true;
			}
			catch(Exception e) {
				
				return false;
			}
		}
		return false;
	}
	
	@Override
	public String toString() {
		return getType()+":"+getValue();
	}
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
}
