package com.github.madzdns.server.core.backend;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZoneSynch extends ResourceCtrl {

	private static Logger log = LoggerFactory.getLogger(ZoneSynch.class);
	
	private static ZoneSynch ctrl = null;
	
	static {
		
		try {
		
			ctrl = new ZoneSynch();
			
		} catch (Exception e) {
			
			log.error("",e);
			System.exit(1);
		}
	}
	
	private ZoneSynch() throws IOException {
		
		
	}
	
	public static void updateZoneSynch(final String name,
			final long modified,
			final byte[] data,
			final byte state) throws Exception {
		
		ctrl.updateMetaWlock(name, modified, null, data, state);
	}
	
	public static void updateZoneSynch(final String name,
			final long modified,
			final byte state,
			final InputStream is) throws Exception {
		
		ctrl.updateMetaWlock(name, modified, null, state, is);
	}
	
	/**
	 * @see ResourceCtrl#getMetasListRlock
	 * @return 
	 */
	public static List<MetaData> getMetasListSynch() {
		
		return ctrl.getMetasListRlock();
	}
	
	public static MetaData getMetaDataSynch(String name) {
		
		return ctrl.getMetaData(name);
	}
	
	public static Collection<MetaData> getMetaLastModifiedsSynch() {
		
		return ctrl.getLastModifiedWlock();
	}
}
