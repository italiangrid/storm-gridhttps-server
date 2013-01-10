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
package it.grid.storm.storagearea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormBackendInfo {

	private static final Logger log = LoggerFactory.getLogger(StorageAreaManager.class);

	// Backend configuration

	private String stormBackendHostname = null;
	private Integer stormBackendRestPort = null;
	private String stormBackendIP = null;

	public StormBackendInfo(String hostname, String stormBackendIP, int port) {
		setStormBackendHostname(hostname);
		setStormBackendIP(stormBackendIP);
		setStormBackendRestPort(port);
	}

	/**
	 * @param stormBackendIP
	 *            the stormBackendIP to set
	 */
	private void setStormBackendIP(String stormBackendIP) {
		this.stormBackendIP = new String(stormBackendIP);
	}

	/**
	 * @return the stormBackendIP
	 */
	public String getStormBackendIP() {
		return stormBackendIP;
	}

	/**
	 * @param hostname
	 *            the stormBackendHostname to set
	 */
	private void setStormBackendHostname(String hostname) {
		log.debug("Setting stormBackendHostname to " + hostname);
		this.stormBackendHostname = new String(hostname);
	}

	/**
	 * @return the stormBackendHostname
	 */
	public String getStormBackendHostname() {
		return stormBackendHostname;
	}

	/**
	 * @param stormBackendRestPort
	 *            the stormBackendRestPort to set
	 */
	private void setStormBackendRestPort(Integer port) {
		log.debug("Setting stormBackendRestPort to " + port);
		this.stormBackendRestPort = new Integer(port);
	}

	/**
	 * @return the stormBackendRestPort
	 */
	public Integer getStormBackendRestPort() {
		return stormBackendRestPort;
	}

}