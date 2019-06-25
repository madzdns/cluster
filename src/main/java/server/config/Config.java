package server.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import server.Server;
import server.api.FRfra;
import server.api.Resolver;
import server.backend.MetaData;
import server.backend.ZoneSynch;
import server.config.entry.dns.ETxt;
import server.config.entry.dns.EntryDns;
import server.service.ClockWork;


public class Config {

    private static class OtherResz {

        public volatile Map<String, Map<String, MyEntry>> resourses_key = null;

        public volatile List<MyEntry> resources_fixed = null;

        public volatile Map<String, MyEntry> entries = null;

        public volatile Map<String, GeoResolver> georesolvers_map = null;

        public OtherResz() {

            resources_fixed = new ArrayList<MyEntry>();

            resourses_key = new HashMap<String, Map<String, MyEntry>>();

            entries = new HashMap<>();

            georesolvers_map = new HashMap<>();
        }
    }

    private static String APP = "FrFra";

    public static String APP_CONF = "frfra.xml";

    public static String SPRING_CONF = "spring-conf";

    public static String KEY_FILES = "keys";

    public static String RES_CONF = "res";

    public static String META_FILE = "res.metadata";

    public static String CLUSTER_FILE = "cluster.metadata";

    public static String KAFKA_CONFIG_DIR = "kafka";

    public static String KAFKA_CONFIG_PATH = "server.properties";

    public static String KAFKA_ZOOKEEPER_CONF = "";

    public static String MAIN_DOMAIN_STR;

    public static String MAIN_DOMAIN_STR_WITHOUT_TRAILING_DOT;

    public static Name MAIN_DOMAIN;

    public static Name MAIN_DOMAIN_NAME_WITH_CLUSTER_PREFIX;

    public static NameserverConfig MainDomainNameserverConfig = null;

    //private static Map<String,GeoResolver> resourses_zone = null;

    private static Map<String, Resource> resourses_zone = null;

    private static volatile OtherResz others = null;

    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static App app_conf = null;

    public static String APPHOME;

    public static String RESPATH;

    public static String SPRINGCONFPATH;

    public static String KEY_STORES;

    private static ClockWork monitor;

    private static ClockWork report;

    private static Logger log;

    private static Soa rootSoa;

    public static Soa getRootSoa() {

        return rootSoa;
    }

    private static OtherResz getOtherResz() {

        if (others != null)

            return others;

        if (resourses_zone == null
                || resourses_zone.size() == 0)

            return null;

        OtherResz tmpOthers = new OtherResz();

        for (Iterator<Entry<String, Resource>> it = resourses_zone.entrySet().iterator();
             it.hasNext(); ) {

            Resource r = it.next().getValue();

            if (r.getGeoResolvers() == null) {

                continue;
            }

            for (Iterator<GeoResolver> git = r.getGeoResolvers().iterator(); git.hasNext(); ) {

                GeoResolver gr = git.next();

                gr.setResource(r);
                tmpOthers.georesolvers_map.put(gr.getZone(), gr);

                List<MyEntry> myEntries = gr.getEntries();

                if (myEntries == null) continue;

                for (int j = 0; j < myEntries.size(); j++) {

                    MyEntry g = myEntries.get(j);

                    g.setResolver(gr);

                    if (g.getFullAName() == null) {

                        String fullAName = new StringBuilder(g.getA())
                                .append(".").append(gr.getZone()).append(".")
                                .append(MAIN_DOMAIN_STR).toString();

                        g.setFullAName(fullAName);
                    }

                    tmpOthers.entries.put(g.getFullAName(), g);

                    if (tmpOthers.resourses_key.containsKey(g.getKey())) {

                        tmpOthers.resourses_key.get(g.getKey()).put(g.getFullAName(), g);
                    } else {

                        Map<String, MyEntry> ag = new HashMap<String, MyEntry>();

                        ag.put(g.getFullAName(), g);

                        tmpOthers.resourses_key.put(g.getKey(), ag);
                    }

                    if (!g.isJustHaDnsMode() && g.hasFixedGeoMappers()) {

                        tmpOthers.resources_fixed.add(g);
                    }
                }
            }

            if (r.getDnsList() != null) {

                for (Iterator<EntryDns> edIt = r.getDnsList().iterator(); edIt.hasNext(); ) {

                    EntryDns eDns = edIt.next();

                    if (eDns.getaNameZoneName() != null) {

                        eDns.setEntry(tmpOthers.entries.get(new StringBuilder(eDns.getaNameZoneName()).append(".").append(Config.MAIN_DOMAIN_STR).toString()));
                    }
                }
            }
        }

        return others = tmpOthers;
    }

    private static void invalidateOthers() {

        if (others == null)

            return;

        others.resources_fixed = null;

        others.resourses_key = null;

        others.entries = null;

        others.georesolvers_map = null;

        others = null;
    }


    public static void reloadFixeds() {

        List<MyEntry> f = getFixedResz();

        if (f == null)

            return;

        Resolver.setupFixedEntries(f);
    }

    public static Map<String, Resource> getZoneResz(final List<MetaData> changedResz) {

        List<MetaData> changedReszList = null;

        if (changedResz == null
                || changedResz.size() == 0) {

            if (resourses_zone != null)

                return new HashMap<String, Resource>(resourses_zone);

            changedReszList = ZoneSynch.getMetasListSynch();
        } else {

            changedReszList = changedResz;
        }

        if (changedReszList == null
                || changedReszList.size() == 0)

            return null;

        int size_of_resz = changedReszList.size();

        try {

            lock.writeLock().lock();

            if (resourses_zone == null) {

                resourses_zone = new HashMap<String, Resource>();
            }

            for (int i = 0; i < size_of_resz; i++) {

                MetaData meta = changedReszList.get(i);

                if (meta.isDeleted()) {

                    log.debug("Deleted resource <{}>", meta);

                    resourses_zone.remove(meta.getName());
                    continue;
                }

                Resource r = meta.getResolver();

                for (Iterator<EntryDns> dnsIt = r.getDnsList().iterator(); dnsIt.hasNext(); ) {

                    EntryDns dns = dnsIt.next();

                    if (dns.isRoot()) {

                        NameserverConfig nsc = new NameserverConfig();
                        nsc.setCount(2);
                        nsc.setTtl(3600);
                        nsc.setName("ns");

                        try {

                            MAIN_DOMAIN_STR = dns.getName();

                            MAIN_DOMAIN = Name.fromString(MAIN_DOMAIN_STR);

                            MAIN_DOMAIN_STR_WITHOUT_TRAILING_DOT = MAIN_DOMAIN.toString(true);

                            rootSoa = dns.getSoaRecord();

                            if (dns.getTxtRecords() != null) {

                                for (Iterator<ETxt> dnsFieldIt = dns.getTxtRecords().iterator(); dnsFieldIt.hasNext(); ) {

                                    ETxt rec = dnsFieldIt.next();

                                    if (rec.getTarget().startsWith(getApp().getNameserverConfStr())) {

                                        String[] nscs = rec.getTarget().split(",");

                                        if (nscs.length >= 3) {

                                            nsc = new NameserverConfig();
                                            nsc.setCount(Integer.parseInt(nscs[2]));
                                            nsc.setTtl(Integer.parseInt(nscs[1]));
                                            nsc.setName(rec.getName());
                                        }

                                        break;
                                    }
                                }
                            }

                            MainDomainNameserverConfig = nsc;

                        } catch (TextParseException e) {

                            log.error("", e);
                        }

                        break;
                    }
                }

                resourses_zone.put(meta.getName(), r);
            }

            invalidateOthers();
            getOtherResz();

            return resourses_zone;

        } finally {
            lock.writeLock().unlock();
        }
    }

    private static Map<String, Map<String, MyEntry>> getKeyResz() {

        try {

            lock.readLock().lock();

            OtherResz other = getOtherResz();

            if (other == null)

                return null;

            return other.resourses_key;

        } finally {
            lock.readLock().unlock();
        }
    }

    public static List<MyEntry> getFixedResz() {

        try {

            lock.readLock().lock();

            OtherResz other = getOtherResz();

            if (other == null)

                return null;

            return other.resources_fixed;

        } finally {
            lock.readLock().unlock();
        }
    }

    public static Resource getResource(String resId) {

        try {

            lock.readLock().lock();

            return resourses_zone == null ? null : resourses_zone.get(resId);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static GeoResolver getGeoResolver(String zone) {

        try {

            lock.readLock().lock();

            OtherResz or = getOtherResz();

            return or == null || or.georesolvers_map == null ? null : or.georesolvers_map.get(zone);
        } finally {
            lock.readLock().unlock();
        }
    }

    public static Map<String, MyEntry> getMyEntry(String key) {

        Map<String, Map<String, MyEntry>> tmpRk = getKeyResz();

        if (tmpRk == null) {

            return null;
        }

        return tmpRk.get(key);
    }

    public static MyEntry getMyEntryWithFullAname(String fullAname) {

        try {

            lock.readLock().lock();

            OtherResz other = getOtherResz();

            if (other == null)

                return null;

            return other.entries != null ? other.entries.get(fullAname) : null;

        } finally {
            lock.readLock().unlock();
        }
    }

    private static String getRoot() throws IOException {

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

        System.setProperty(APP + ".root", root);
        System.setProperty(APP + ".res", root + File.separator + RES_CONF + File.separator);
        System.setProperty(APP + ".keys", root + File.separator + KEY_FILES + File.separator);
        System.setProperty(APP + ".spring_config_root", root + File.separator + SPRING_CONF + File.separator);
        System.setProperty(APP + ".kafka.dir", root + File.separator + KAFKA_CONFIG_DIR + File.separator);
        System.setProperty(APP + ".kafka", root + File.separator + KAFKA_CONFIG_DIR + File.separator + KAFKA_CONFIG_PATH);
        System.setProperty(APP + ".kafka.z.conf", root + File.separator + KAFKA_CONFIG_DIR + File.separator + KAFKA_ZOOKEEPER_CONF);

        System.setProperty("log4j.configuration", new File(root + File.separator + "log4j.properties").toURI().toURL().toString());

        System.setProperty("logback.configurationFile", new File(root + File.separator + "logback.xml").toURI().toURL().toString());

        log = LoggerFactory.getLogger(Config.class);

        System.setProperty("red5.root", root);

        System.out.println(APP + " root: " + root + FRfra.lineSeparator);

        System.out.println("log4j configuration: " + System.getProperty("log4j.configuration") + FRfra.lineSeparator);

        System.out.println("logback configuration: " + System.getProperty("logback.configurationFile") + FRfra.lineSeparator);

        APPHOME = System.getProperty(APP + ".root");
        RESPATH = System.getProperty(APP + ".res");
        SPRINGCONFPATH = System.getProperty(APP + ".spring_config_root");
        KEY_STORES = System.getProperty(APP + ".keys");

        Config.KAFKA_CONFIG_PATH = System.getProperty(APP + ".kafka");

        Config.KAFKA_ZOOKEEPER_CONF = System.getProperty(APP + ".kafka.z.conf");

        return root;
    }

    public static void init(String app,
                            String app_conf,
                            String spring_conf,
                            String key_files,
                            String res_conf,
                            String metadata,
                            String cluster_meta,
                            String kafka_dir,
                            String kafka_conf,
                            String kafka_zookeeper_conf) {

        Config.APP = app;
        Config.APP_CONF = app_conf;
        Config.SPRING_CONF = spring_conf;
        Config.KEY_FILES = key_files;
        Config.RES_CONF = res_conf;
        Config.META_FILE = metadata;
        Config.CLUSTER_FILE = cluster_meta;
        Config.KAFKA_CONFIG_DIR = kafka_dir;
        Config.KAFKA_CONFIG_PATH = kafka_conf;
        Config.KAFKA_ZOOKEEPER_CONF = kafka_zookeeper_conf;

        try {

            getRoot();

            if (Config.getApp().getId() == 0) {

                log.error("You must define a unique id for each cluster heads");
                System.err.println("You must define a unique id for each cluster heads");
                //XXX exit point
                System.exit(1);
            }

            Server server = new Server();
            server.start();

        } catch (Exception e) {

            log.error("", e);
            //XXX exit point
            System.exit(1);
        }
    }

    public static URL getSpringConf() {

        try {

            getRoot();
            return new File(SPRINGCONFPATH).toURI().toURL();

        } catch (Exception e) {

            e.printStackTrace();
            //XXX exit point
            System.exit(1);
        }
        return null;
    }

    public static App getApp() {

        if (app_conf != null)

            return app_conf;

        String confPath = null;

        try {

            confPath = getRoot() + File.separator + APP_CONF;
        } catch (IOException e1) {

            e1.printStackTrace();
            System.exit(1);
        }

        JAXBContext jc = null;
        try {

            jc = JAXBContext.newInstance(App.class);
        } catch (Exception e) {

            e.printStackTrace();
        }

        try {

            app_conf = (App) jc.createUnmarshaller().unmarshal(new FileInputStream(confPath));

        } catch (Exception e) {

            e.printStackTrace();
            System.exit(1);
        }

        return app_conf;
    }

    public static ClockWork getMonitor() {

        return monitor;
    }

    public static void setMonitor(ClockWork monitor) {

        Config.monitor = monitor;
    }

    public static ClockWork getReport() {

        return report;
    }

    public static void setReport(ClockWork report) {

        Config.report = report;
    }
}
