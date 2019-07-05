package com.github.madzdns.cluster.app;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class Bootstrap {
	
	public final static class JarFileFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			return name.endsWith(".jar");
		}
	}

	public static void main(String[] args) {
		
		JarFileFilter jarFileFilter = new JarFileFilter();
		List<URL> classpathz = new ArrayList<>(10);
		URL[] classpathzArr;
		URLClassLoader loader;
		String home = AppConfig.getRoot();
		String libPath = home + File.separator + AppConfig.LIB + File.separator;
		File libDir = new File(libPath);
		File[] libFiles = libDir.listFiles(jarFileFilter);
		
		for (File lib : Objects.requireNonNull(libFiles)) {
			try {
				classpathz.add(lib.toURI().toURL());
			} catch (MalformedURLException e) {
				System.err.printf("Exception %s\n", e);
			}
		}
		
		try {
			if(classpathz.size()>0) {
				classpathzArr = new URL[classpathz.size()];
				classpathzArr = classpathz.toArray(classpathzArr);
				ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
				
				loader = new URLClassLoader(classpathzArr,currentLoader);
				Thread.currentThread().setContextClassLoader(loader);
			}
			
			Method init = Class
					.forName("com.github.madzdns.cluster.core.config.Config")
					.getMethod("init",
							String.class,
							String.class,
							String.class,
							String.class,
							String.class,
							String.class,
							String.class,
							String.class,
							String.class,
							String.class);

			init.invoke(null, AppConfig.APP,
					AppConfig.APP_CONF,
					AppConfig.SPRING_CONF,
					AppConfig.KEY_FILES,
					AppConfig.RES_CONF,
					AppConfig.META_FILE,
					AppConfig.CLUSTER_FILE,
					AppConfig.KAFKA_CONF_DIR,
					AppConfig.KAFKA_CONF_FILE,
					AppConfig.KAFKA_ZOOKEEPER_CONF);
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
