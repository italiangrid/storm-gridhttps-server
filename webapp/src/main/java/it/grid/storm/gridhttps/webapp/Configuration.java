package it.grid.storm.gridhttps.webapp;

import it.grid.storm.gridhttps.webapp.checksum.Checksum.ChecksumAlgorithm;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumNotSupportedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

	private static final Logger log = LoggerFactory.getLogger(Configuration.class);

	private static HashMap<String, String> map = new HashMap<String, String>();

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
			add("REMOVE_SPACES");
			add("REMOVE_SPACES_WITH");
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
			add("REMOVE_SPACES");
		}
	};
	
	public static void loadDefaultConfiguration() {
		map.clear();
		for (String name : DefaultConfiguration.getKeys()) {
			if (allowed.contains(name))
				map.put(name, DefaultConfiguration.getValue(name));
		}
	}
	
	public static void initFromJSON(Map<String,String> params) {
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
		log.debug(" - REMOVE_SPACES            : " + getRemoveSpaces());
	}
	
	public static String getBackendHostname() {
		return map.containsKey("BACKEND_HOSTNAME") ? map.get("BACKEND_HOSTNAME") : null;
	}
	
	public static int getBackendPort() {
		return map.containsKey("BACKEND_PORT") ? Integer.parseInt(map.get("BACKEND_PORT")) : null;
	}
	
	public static int getBackendServicePort() {
		return map.containsKey("BACKEND_SERVICE_PORT") ? Integer.parseInt(map.get("BACKEND_SERVICE_PORT")) : null;
	}
	
	public static String getFrontendHostname() {
		return map.containsKey("FRONTEND_HOSTNAME") ? map.get("FRONTEND_HOSTNAME") : null;
	}
	
	public static int getFrontendPort() {
		return map.containsKey("FRONTEND_PORT") ? Integer.parseInt(map.get("FRONTEND_PORT")) : null;
	}
	
	public static String getWebdavContextPath() {
		return map.containsKey("WEBDAV_CONTEXTPATH") ? map.get("WEBDAV_CONTEXTPATH") : null;
	}
	
	public static String getFileTransferContextPath() {
		return map.containsKey("FILETRANSFER_CONTEXTPATH") ? map.get("FILETRANSFER_CONTEXTPATH") : null;
	}
	
	public static String getGpfsRootDirectory() {
		return map.containsKey("GPFS_ROOT_DIRECTORY") ? map.get("GPFS_ROOT_DIRECTORY") : null;
	}
	
	public static boolean getComputeChecksum() {
		return map.containsKey("COMPUTE_CHECKSUM") ? Boolean.valueOf(map.get("COMPUTE_CHECKSUM")) : null;
	}
	
	public static ChecksumAlgorithm getChecksumType() {
		ChecksumAlgorithm checksumAlgorithm = null;
		if (map.containsKey("CHECKSUM_TYPE")) {
			checksumAlgorithm = ChecksumAlgorithm.getChecksumAlgorithm(map.get("CHECKSUM_TYPE"));
			if (checksumAlgorithm == null) {
				log.error("checksum algorithm '" + map.get("CHECKSUM_TYPE") + "' is not supported!");
				throw new ChecksumNotSupportedException("Checksum algorithm not supported: " + map.get("CHECKSUM_TYPE"));
			}
		} 
		return checksumAlgorithm;
	}
	
	public static boolean getRemoveSpaces() {
		return map.containsKey("REMOVE_SPACES") ? Boolean.valueOf(map.get("REMOVE_SPACES")) : null;
	}
	
	public static String getRemoveSpacesWith() {
		return map.containsKey("REMOVE_SPACES_WITH") ? map.get("REMOVE_SPACES_WITH") : null;
	}
	
}