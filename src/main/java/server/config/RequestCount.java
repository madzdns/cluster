package server.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "req-count")
@XmlAccessorType(XmlAccessType.FIELD)
public class RequestCount {

	@XmlAttribute(name="c")
	private long count = -1;
	
	@XmlAttribute(name="cv")
	private long totalUsedValue = 0;
	
	@XmlAttribute(name="inc")
	private int increment = 10;
	
	@XmlTransient
	private int currentUsedValue = 0;
	
	@XmlTransient
	private boolean conditionMet = false;

	public long getCount() {
		
		return count;
	}

	public void setCount(long count) {
		
		this.count = count;
	}

	public synchronized long getCurrentUsedValue() {
		
		return currentUsedValue;
	}

	public synchronized boolean increaseCurrentUsedValueBy(int currentUsedValue, int checkValue) {
		
		if(this.count == -1) {
			
			return false;
		}
		
		this.currentUsedValue += currentUsedValue;
		
		this.totalUsedValue += currentUsedValue;
		
		//make sure currentUsedValue be reachable to checkValue
		if(!this.conditionMet && this.currentUsedValue > 0 
				&& this.currentUsedValue >= checkValue) {

			return this.conditionMet = true;
		}
		else if(this.totalUsedValue >= this.count) {

			return this.conditionMet = true;
		}
		
		return false;
	}
	
	public synchronized long getCurrentUsedValueAndReset() {
		
		this.conditionMet = false;
		long x = this.currentUsedValue;
		this.currentUsedValue = 0;
		
		return x;
	}

	public boolean isRanOUt() {
		
		return this.count != -1 && this.totalUsedValue >= this.count;
	}
	
	public synchronized void makeRanOUt() {
		
		this.totalUsedValue = this.count;
	}
	
	
	@Override
	public String toString() {

		return new StringBuilder("Count=").append(count).append(", CurrentUsed=").
				append(currentUsedValue).append(", TotalUsedValue=").append(totalUsedValue).toString();
	}

	public long getTotalUsedValue() {
		
		return totalUsedValue;
	}

	public synchronized void setTotalUsedValueIfGreater(long totalUsedValue) {
		
		if(this.totalUsedValue < totalUsedValue) {
		
			this.totalUsedValue = totalUsedValue;
		}
	}

	public int getIncrement() {
		
		return increment;
	}

	public void setIncrement(int increment) {
		
		this.increment = increment;
	}
}
