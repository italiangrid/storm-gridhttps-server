package it.grid.storm.webdav.server.data;

import it.grid.storm.webdav.DefaultConfiguration;

public class StormBackend {
	private String hostname;
	private int port;
	private int servicePort;

	public StormBackend(String hostname, int port, int servicePort) {
		this();
		this.setHostname(hostname);
		this.setPort(port);
		this.setServicePort(servicePort);
	}
	
	public StormBackend() {
		this.setPort(DefaultConfiguration.STORM_BE_PORT);
		this.setServicePort(DefaultConfiguration.STORM_BE_SERVICE_PORT);
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

	public int getServicePort() {
		return servicePort;
	}

	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}
	
	public String toString() {
		return "{'"+hostname+"', "+port+", "+servicePort+"}";	
	}
	
	public void checkConfiguration() throws Exception {
		if (hostname.isEmpty())
			throw new Exception("backend hostname is empty!");
		if (port <= 0)
			throw new Exception("backend port is "+port+"!");
		if (servicePort <= 0)
			throw new Exception("backend service port is "+servicePort+"!");
		if (servicePort == port)
			throw new Exception("backend port is equal to service port!");
	}

}