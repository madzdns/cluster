package app;

import java.io.File;
import java.io.IOException;

public class AppConfig {

    public final static String APP = "FrFra";
    public final static String APP_CONF = "frfra.xml";
    public final static String SPRING_CONF = "spring-conf";
    public final static String KEY_FILES = "keys";
    public final static String RES_CONF = "res";
    public final static String LIB = "lib";
    public final static String META_FILE = "jcs-cache.cfg";
    public final static String CLUSTER_FILE = "jcs-cache.cfg";
    public final static String KAFKA_CONF_DIR = "kafka";
    public final static String KAFKA_CONF_FILE = "server.properties";
    public final static String KAFKA_ZOOKEEPER_CONF = "zookeeper.properties";

    public static String getRoot() throws IOException {

        String root = System.getProperty(APP + ".root");

        if (root != null)

            return root;

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
