package it.grid.storm.webdav.storagearea;

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