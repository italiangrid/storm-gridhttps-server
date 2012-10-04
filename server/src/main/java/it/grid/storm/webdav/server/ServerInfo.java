package it.grid.storm.webdav.server;

import org.italiangrid.utils.https.SSLOptions;

public class ServerInfo {
	
	private String hostname;
	private int httpPort, httpsPort;
	private boolean httpEnabled;
	private SSLOptions sslOptions;
	
	public ServerInfo(String hostname, int httpPort, int httpsPort, SSLOptions options, boolean useHttp) {
		this.hostname = hostname;
		this.httpPort = httpPort;
		this.httpsPort = httpsPort;
		this.httpEnabled = useHttp;
		this.sslOptions = options;
	}
	
	public int getHttpPort() {
		return httpPort;
	}
	
	public int getHttpsPort() {
		return httpsPort;
	}
	
	public String getHostname() {
		return hostname;
	}
	
	public boolean isHttpEnabled() {
		return httpEnabled;
	}

	public SSLOptions getSslOptions() {
		return sslOptions;
	}	
	
}