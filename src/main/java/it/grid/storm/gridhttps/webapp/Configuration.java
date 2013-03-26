/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.webapp;

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
	
	public static String getChecksumType() {
		return map.containsKey("CHECKSUM_TYPE") ? map.get("CHECKSUM_TYPE") : null;
	}
	
}