package com.github.madzdns.server.app;

import java.io.File;

public class AppConfig {

    final static String APP = "FrFra";
    final static String APP_CONF = "frfra.xml";
    final static String SPRING_CONF = "spring-conf";
    final static String KEY_FILES = "keys";
    final static String RES_CONF = "res";
    final static String LIB = "lib";
    final static String META_FILE = "jcs-cache.cfg";
    final static String CLUSTER_FILE = "jcs-cache.cfg";
    final static String KAFKA_CONF_DIR = "kafka";
    final static String KAFKA_CONF_FILE = "core.properties";
    final static String KAFKA_ZOOKEEPER_CONF = "zookeeper.properties";

    public static String getRoot() {
        String root = System.getProperty(APP + ".root");
        if (root != null) {
            return root;
        }
        root = System.getenv(APP + ".root");
        if (root == null || ".".equals(root)) {
            root = System.getProperty("user.dir");
        }
        if (File.separatorChar != '/') {
            root = root.replaceAll("\\\\", "/");
        }
        if (root.charAt(root.length() - 1) == '/') {
            root = root.substring(0, root.length() - 1);
        }
        return root;
    }
}
