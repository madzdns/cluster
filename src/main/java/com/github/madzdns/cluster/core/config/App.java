package com.github.madzdns.cluster.core.config;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@Getter
@Setter
@XmlRootElement(name="app")
@XmlType(name = "app")
@XmlAccessorType(XmlAccessType.FIELD)
public class App {
	
	@XmlElement(name="version")
	private String version;
	
	@XmlElement(name="geo")
	private String geo;
	
	@XmlElement(name="binds")
	private Binds binds;
	
	@XmlAttribute(name="id")
	private short id = 0;
	
	@XmlAttribute(name="key")
	private String key;
	
	@XmlElement(name="apiprefix")
	private String apiPrefix;
	
	@XmlElement(name="webservice")
	private WebserviceCfg webserviceCfg;
	
	@XmlAttribute(name="verbose")
	private boolean verbose = false;
	
	@XmlElement(name="name")
	private String name;
	
	//in seconds
	@XmlElement(name="cname-ok-lookup-ttl")
	private int cnameOkLookupTTL = 10800;
	
	//in seconds
	@XmlElement(name="cname-fail-lookup-ttl")
	private int cnameFailLookupTTL = 1800;
	
	//in seconds
	@XmlElement(name="ns-ok-lookup-ttl")
	private int nsOkLookupTTL = 43200;
		
	//in seconds
	@XmlElement(name="ns-fail-lookup-ttl")
	private int nsFailLookupTTL = 1800;
	
	@XmlElement(name="ns-check")
	private boolean checkForNsValidity = true;

	@XmlElement(name="serv-ssl")
	private boolean servSSLEnabled = false;
	
	@XmlElement(name="soa-rp")
	private String soaResponsible = "com/github/madzdns/x/dns";
	
	@XmlElement(name="cluster-type")
	private String clusterType = "frsynch";
	
	@XmlElement(name="api-call-proto")
	private String apiCallProto = "com/github/madzdns/x/http";
	
	@XmlElement(name="request-count-api-path")
	private String requestCountApiPath = null;
	
	@XmlElement(name="request-count-inc-cond")
	private int requestCountIncrementCondition = 10;
	
	@XmlElement(name="nameserver-conf-str")
	private String nameserverConfStr = "--NameserverConfig,";

	@XmlElement(name="ssl-config")
	private SslConfig sslConfig;

	public boolean isKafkaCluster() {
		return "kafka".equals(clusterType);
	}
}
