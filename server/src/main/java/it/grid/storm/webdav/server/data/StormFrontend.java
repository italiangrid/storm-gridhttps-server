package it.grid.storm.webdav.server.data;

import it.grid.storm.webdav.DefaultConfiguration;

public class StormFrontend {
	private String hostname;
	private int port;

	public StormFrontend(String hostname, int port, int servicePort) {
		this();
		this.setHostname(hostname);
		this.setPort(port);
	}

	public StormFrontend() {
		this.setPort(DefaultConfiguration.STORM_FE_PORT);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String toString() {
		return "{'" + hostname + "', " + port + "}";
	}

	public void checkConfiguration() throws Exception {
		if (hostname.isEmpty())
			throw new Exception("backend hostname is empty!");
		if (port <= 0)
			throw new Exception("backend port is " + port + "!");
	}
}