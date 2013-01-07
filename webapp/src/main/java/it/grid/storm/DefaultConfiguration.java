package it.grid.storm;

import it.grid.storm.checksum.Checksum.ChecksumAlgorithm;

import java.util.HashMap;
import java.util.Set;

public class DefaultConfiguration {
	
	private static final HashMap<String, String> map = new HashMap<String, String>() { 
		private static final long serialVersionUID = -7460222372953081397L;

	{
		put("BACKEND_PORT", "8080");
		put("BACKEND_SERVICE_PORT", "9998");
		put("FRONTEND_PORT", "8444");
		put("WEBDAV_CONTEXTPATH", "");
		put("FILETRANSFER_CONTEXTPATH", "fileTransfer");
		put("GPFS_ROOT_DIRECTORY", "/");
		put("COMPUTE_CHECKSUM", "true");
		put("CHECKSUM_TYPE", ChecksumAlgorithm.ADLER32.name());
	}};

	public static String getValue(String name) {
		return map.get(name);
	}
	
	public static Set<String> getKeys() {
		return map.keySet();
	}
	
}