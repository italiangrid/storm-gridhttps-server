package it.grid.storm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import it.grid.storm.checksum.ChecksumNotSupportedException;
import it.grid.storm.checksum.Checksum.ChecksumAlgorithm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private static final Logger log = LoggerFactory.getLogger(Configuration.class);

	private static HashMap<String, Object> map = new HashMap<String, Object>();

	private static final ArrayList<String> allowed = new ArrayList<String>() {
		private static final long serialVersionUID = -1898442175212161454L;
		{
			add("BACKEND_HOSTNAME");
			add("BACKEND_PORT");
			add("BACKEND_SERVICE_PORT");
			add("FRONTEND_HOSTNAME");
			add("FRONTEND_PORT");
			add("WEBDAV_CONTEXTPATH");
			add("FILETRANSFER_CONTEXTPATH");
			add("GPFS_ROOT_DIRECTORY");
			add("COMPUTE_CHECKSUM");
			add("CHECKSUM_TYPE");
		}
	};
	
	private static final ArrayList<String> needed = new ArrayList<String>() {
		private static final long serialVersionUID = 2808068467669277898L;
		{
			add("BACKEND_HOSTNAME");
			add("BACKEND_PORT");
			add("BACKEND_SERVICE_PORT");
			add("FRONTEND_HOSTNAME");
			add("FRONTEND_PORT");
			add("WEBDAV_CONTEXTPATH");
			add("FILETRANSFER_CONTEXTPATH");
			add("GPFS_ROOT_DIRECTORY");
			add("COMPUTE_CHECKSUM");
		}
	};
	
	public static void loadDefaultConfiguration() {
		map.clear();
		for (String name : DefaultConfiguration.keys()) {
			if (allowed.contains(name))
				map.put(name, DefaultConfiguration.get(name));
		}
	}
	
	public static void initFromJSON(Map<String,Object> params) {
		for (String name : params.keySet())
			if (allowed.contains(name))
				map.put(name, params.get(name));
	}

	public static boolean isValid() {
		if (!map.keySet().containsAll(needed)) {
			Collection<String> result = new ArrayList<String>(needed);
		    result.removeAll(map.keySet());
		    log.warn(result.toString() + " not initialized!");
		    return false;
		}
		if (getBackendHostname().isEmpty()) {
			log.warn("BACKEND_HOSTNAME is empty!");
			return false;
		}
		if (getFrontendHostname().isEmpty()) {
			log.warn("FRONTEND_HOSTNAME is empty!");
			return false;
		}
		if (getGpfsRootDirectory().isEmpty()) {
			log.warn("GPFS_ROOT_DIRECTORY is empty!");
			return false;
		}
		if (getWebdavContextPath().isEmpty()) {
			log.warn("WEBDAV_CONTEXTPATH is empty!");
			return false;
		}
		if (getFileTransferContextPath().isEmpty()) {
			log.warn("FILETRANSFER_CONTEXTPATH is empty!");
			return false;
		}
		if (getBackendPort() <= 1024) {
			log.warn("BACKEND_PORT = " + getBackendPort() + " is not valid!");
			return false;
		}
		if (getBackendServicePort() <= 1024) {
			log.warn("BACKEND_SERVICE_PORT = " + getBackendServicePort() + " is not valid!");
			return false;
		}
		if (getFrontendPort() <= 1024) {
			log.warn("FRONTEND_PORT = " + getFrontendPort() + " is not valid!");
			return false;
		}
		if (getComputeChecksum()) {
			if (getChecksumType() == null) {
				log.warn("CHECKSUM_TYPE not initialized!");
				return false;
			}
		}
		return true;
	}

	public static void print() {
		log.debug("Configuration values:");
		log.debug(" - BACKEND_HOSTNAME         : " + getBackendHostname());
		log.debug(" - BACKEND_PORT             : " + getBackendPort());
		log.debug(" - BACKEND_SERVICE_PORT     : " + getBackendServicePort());
		log.debug(" - FRONTEND_HOSTNAME        : " + getFrontendHostname());
		log.debug(" - FRONTEND_PORT            : " + getFrontendPort());
		log.debug(" - WEBDAV_CONTEXTPATH       : " + getWebdavContextPath());
		log.debug(" - FILETRANSFER_CONTEXTPATH : " + getFileTransferContextPath());
		log.debug(" - GPFS_ROOT_DIRECTORY      : " + getGpfsRootDirectory());
		log.debug(" - COMPUTE_CHECKSUM         : " + getComputeChecksum());
		log.debug(" - CHECKSUM_TYPE            : " + getChecksumType());
	}
	
	public static String getBackendHostname() {
		return map.containsKey("BACKEND_HOSTNAME") ? (String) map.get("BACKEND_HOSTNAME") : null;
	}
	
	public static int getBackendPort() {
		return map.containsKey("BACKEND_PORT") ? (Integer) map.get("BACKEND_PORT") : null;
	}
	
	public static int getBackendServicePort() {
		return map.containsKey("BACKEND_SERVICE_PORT") ? (Integer) map.get("BACKEND_SERVICE_PORT") : null;
	}
	
	public static String getFrontendHostname() {
		return map.containsKey("FRONTEND_HOSTNAME") ? (String) map.get("FRONTEND_HOSTNAME") : null;
	}
	
	public static int getFrontendPort() {
		return map.containsKey("FRONTEND_PORT") ? (Integer) map.get("FRONTEND_PORT") : null;
	}
	
	public static String getWebdavContextPath() {
		return map.containsKey("WEBDAV_CONTEXTPATH") ? (String) map.get("WEBDAV_CONTEXTPATH") : null;
	}
	
	public static String getFileTransferContextPath() {
		return map.containsKey("FILETRANSFER_CONTEXTPATH") ? (String) map.get("FILETRANSFER_CONTEXTPATH") : null;
	}
	
	public static String getGpfsRootDirectory() {
		return map.containsKey("GPFS_ROOT_DIRECTORY") ? (String) map.get("GPFS_ROOT_DIRECTORY") : null;
	}
	
	public static boolean getComputeChecksum() {
		return map.containsKey("COMPUTE_CHECKSUM") ? (Boolean) map.get("COMPUTE_CHECKSUM") : null;
	}
	
	public static ChecksumAlgorithm getChecksumType() {
		ChecksumAlgorithm checksumAlgorithm = null;
		if (map.containsKey("CHECKSUM_TYPE")) {
			String algorithm = (String) map.get("CHECKSUM_TYPE");
			checksumAlgorithm = ChecksumAlgorithm.getChecksumAlgorithm(algorithm);
			if (checksumAlgorithm == null) {
				log.error("checksum algorithm '" + algorithm + "' is not supported!");
				throw new ChecksumNotSupportedException("Checksum algorithm not supported: " + algorithm);
			}
		} 
		return checksumAlgorithm;
	}
	
}