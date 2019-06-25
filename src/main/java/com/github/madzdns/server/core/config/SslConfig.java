package com.github.madzdns.server.core.config;

import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@Getter
@Setter
@XmlType(name = "ssl-conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class SslConfig {
    @XmlElement(name="keystore-pass")
    private String keyStorePass;
    @XmlElement(name="truststore-pass")
    private String trustStorePass;
    @XmlElement(name="keystore-pass-2")
    private String keyStorePass2;
    @XmlElement(name="keystore")
    private String keyStore;
    @XmlElement(name="truststore")
    private String trustStore;
    @XmlElement(name="certificate")
    private String certificate;
}
