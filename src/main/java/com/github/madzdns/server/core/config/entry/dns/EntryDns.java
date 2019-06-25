package com.github.madzdns.server.core.config.entry.dns;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import com.github.madzdns.server.core.config.MyEntry;
import com.github.madzdns.server.core.config.Soa;

@XmlType(name = "edns")
@XmlAccessorType(XmlAccessType.FIELD)
public class EntryDns {
	
	@XmlTransient
	private MyEntry entry = null;

	@XmlAttribute(name="n")
	private String name;
	
	@XmlAttribute(name="ds")
	private byte dnsSecEnabled = 0;
	
	@XmlAttribute(name="r")
	private byte root = 0;
	
	@XmlAttribute(name="a")
	private String aNameZoneName = null;
	
	/*@XmlAttribute(name="tt")
	private long ttl = 20;*/
	
	@XmlElement(name="ens")
	private List<ENs> nsRecords;
	
	@XmlElement(name="ecn")
	private List<ECn> cnameRecords;
	
	@XmlElement(name="ea")
	private List<EA> aRecords;
	
	@XmlElement(name="eaa")
	private List<EAa> aaaaRecords;
	
	@XmlElement(name="eptr")
	private List<EPtr> ptrRecords;
	
	@XmlElement(name="emx")
	private List<EMx> mxRecords;
	
	@XmlElement(name="etxt")
	private List<ETxt> txtRecords;
	
	@XmlElement(name="esrv")
	private List<ESrv> srvRecords;
	
	@XmlElement(name="ena")
	private List<ENaPtr> naptrRecords;
	
	@XmlElement(name="soa")
	private Soa soaRecord;
	
	@XmlElement(name="edn")
	private EDn dnameRecord;
	
	@XmlElement(name="eacn")
	private EAcn acnameRecord;
	
	@XmlElement(name="ebcn")
	private EBcn bcnameRecord;
	
	@XmlElement(name="ercn")
	private ERcn rcnameRecord;
	
	@XmlElement(name="edy")
	private List<EDy> selfResolveRecords;
	
	public boolean isRoot() {
		
		return this.root == 1? true: false;
	}
	
	public String getName() {
		
		return name;
	}

	public void setName(String name) {
		
		this.name = name;
	}

	public Soa getSoaRecord() {
		
		return soaRecord;
	}

	public void setSoaRecord(Soa soaRecord) {
		
		this.soaRecord = soaRecord;
	}
	
	public List<ECn> getCnameRecords() {
		
		return cnameRecords;
	}

	public void setCnameRecords(List<ECn> cnameRecords) {
		
		this.cnameRecords = cnameRecords;
	}

	public List<EA> getaRecords() {
		
		return aRecords;
	}

	public void setaRecords(List<EA> aRecords) {
		
		this.aRecords = aRecords;
	}

	public List<EAa> getAaaaRecords() {
		
		return aaaaRecords;
	}

	public void setAaaaRecords(List<EAa> aaaaRecords) {
		
		this.aaaaRecords = aaaaRecords;
	}

	public List<EPtr> getPtrRecords() {
		
		return ptrRecords;
	}

	public void setPtrRecords(List<EPtr> ptrRecords) {
		
		this.ptrRecords = ptrRecords;
	}

	public List<EMx> getMxRecords() {
		
		return mxRecords;
	}

	public void setMxRecords(List<EMx> mxRecords) {
		
		this.mxRecords = mxRecords;
	}

	public List<ETxt> getTxtRecords() {
		
		return txtRecords;
	}

	public void setTxtRecords(List<ETxt> txtRecords) {
		
		this.txtRecords = txtRecords;
	}

	public List<ESrv> getSrvRecords() {
		
		return srvRecords;
	}

	public void setSrvRecords(List<ESrv> srvRecords) {
		
		this.srvRecords = srvRecords;
	}

	public List<ENaPtr> getNaptrRecords() {
		
		return naptrRecords;
	}

	public void setNaptrRecords(List<ENaPtr> naptrRecords) {
		
		this.naptrRecords = naptrRecords;
	}

	public List<ENs> getNsRecords() {
		
		return nsRecords;
	}

	public void setNsRecords(List<ENs> nsRecords) {
		
		this.nsRecords = nsRecords;
	}

	public EDn getDnameRecord() {
		
		return dnameRecord;
	}

	public void setDnameRecord(EDn dnameRecord) {
		
		this.dnameRecord = dnameRecord;
	}

	public boolean isDnsSecEnabled() {
		
		return dnsSecEnabled==1?true:false;
	}

	public void setDnsSecEnabled(boolean dnsSecEnabled) {
		
		this.dnsSecEnabled = (byte)(dnsSecEnabled?1:0);
	}

	public EAcn getAcnameRecord() {
		
		return acnameRecord;
	}

	public void setAcnameRecord(EAcn acnameRecord) {
		
		this.acnameRecord = acnameRecord;
	}

	public EBcn getBcnameRecord() {
		
		return bcnameRecord;
	}

	public void setBcnameRecord(EBcn bcnameRecord) {
		
		this.bcnameRecord = bcnameRecord;
	}

	public ERcn getRcnameRecord() {
		
		return rcnameRecord;
	}

	public void setRcnameRecord(ERcn rcnameRecord) {
		
		this.rcnameRecord = rcnameRecord;
	}

	public List<EDy> getSelfResolveRecords() {
		return selfResolveRecords;
	}

	public void setSelfResolveRecords(List<EDy> selfResolveRecords) {
		this.selfResolveRecords = selfResolveRecords;
	}

	public String getaNameZoneName() {
		return aNameZoneName;
	}

	public void setaNameZoneName(String aNameZoneName) {
		this.aNameZoneName = aNameZoneName;
	}

	public MyEntry getEntry() {
		
		return entry;
	}

	public void setEntry(MyEntry entry) {
		
		this.entry = entry;
	}

	/*public long getTtl() {
		return ttl;
	}

	public void setTtl(long ttl) {
		this.ttl = ttl;
	}*/
}
