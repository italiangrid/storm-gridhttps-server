package it.grid.storm.webdav.server;

import org.italiangrid.utils.https.SSLOptions;

public class ServerInfo {
	
	private boolean enabled;
	private String name;
	private int port;
	private boolean ssl;
	private SSLOptions sslOptions;
	private String hostname;
	
	public ServerInfo() {
		this.enabled = false;
	}
	
	public ServerInfo(String name, String hostname, int port) {
		this.enabled = true;
		this.name = name;
		this.hostname = hostname;
		this.port = port;
		ssl = false;
	}

	public ServerInfo(String name, String hostname, int port, SSLOptions options) {
		this(name, hostname, port);
		this.ssl = true;
		this.sslOptions = options; 
	}
	
	public int getPort() {
		return port;
	}

	public String getName() {
		return name;
	}

	public String getHostname() {
		return hostname;
	}
	
	public boolean isSsl() {
		return ssl;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public SSLOptions getSslOptions() {
		return sslOptions;
	}	
	
	public String toString() {
		String output = "(";
		output += "name="+this.name;
		output += ", hostname="+this.hostname;
		output += ", port="+this.port;
		output += ", isSsl="+this.ssl;
		if (this.isSsl()) {
			output += ", sslOptions=(";
			output += "certificate_file="+this.sslOptions.getCertificateFile();
			output += ", key_file="+this.sslOptions.getKeyFile();
			output += ", trust_store="+this.sslOptions.getTrustStoreDirectory();
			output += ")";
		}
		output += ")";
		return output;
	}
}