package it.grid.storm.webdav.server.data;

import java.io.File;

import it.grid.storm.webdav.DefaultConfiguration;

import org.italiangrid.utils.https.SSLOptions;

public class StormGridhttps {

	private String hostname;
	private int httpPort;
	private int httpsPort;
	private boolean enabledHttp;
	private MapperServlet mapperServlet;
	private String webappsDirectory;
	private SSLOptions ssloptions;
	private String logFile;
	private File warFile;
	
	private File rootDirectory;
	private String webdavContextPath;
	private String filetransferContextPath;
	
	private boolean computeChecksum;
	private String checksumType;

	public StormGridhttps() {
		this.setHttpPort(DefaultConfiguration.STORM_GHTTPS_HTTP_PORT);
		this.setHttpsPort(DefaultConfiguration.STORM_GHTTPS_HTTPS_PORT);
		this.setEnabledHttp(DefaultConfiguration.STORM_GHTTPS_USE_HTTP);
		MapperServlet mapperServlet = new MapperServlet(DefaultConfiguration.MAPPER_SERVLET_CONTEXT_PATH,
				DefaultConfiguration.MAPPER_SERVLET_CONTEXT_SPEC);
		this.setMapperServlet(mapperServlet);
		this.setWebappsDirectory(DefaultConfiguration.STORM_GHTTPS_WEBAPPS_DIRECTORY);
		SSLOptions sslOptions = new SSLOptions();
		sslOptions.setCertificateFile(DefaultConfiguration.STORM_GHTTPS_HTTPS_CERTIFICATE_FILE);
		sslOptions.setKeyFile(DefaultConfiguration.STORM_GHTTPS_HTTPS_KEY_FILE);
		sslOptions.setTrustStoreDirectory(DefaultConfiguration.STORM_GHTTPS_HTTPS_TRUST_STORE_DIRECTORY);
		sslOptions.setNeedClientAuth(DefaultConfiguration.STORM_GHTTPS_HTTPS_NEED_CLIENT_AUTH);
		sslOptions.setWantClientAuth(DefaultConfiguration.STORM_GHTTPS_HTTPS_WANT_CLIENT_AUTH);
		this.setSsloptions(sslOptions);
		this.setLogFile(DefaultConfiguration.STORM_GHTTPS_LOG_FILE);
		this.warFile = null;
		this.setFiletransferContextPath(DefaultConfiguration.FILETRANSFER_CONTEXTPATH);
		this.setWebdavContextPath(DefaultConfiguration.WEBAPP_CONTEXTPATH);
		this.setRootDirectory(new File(DefaultConfiguration.ROOTDIRECTORY));
		this.setComputeChecksum(DefaultConfiguration.COMPUTE_CHECKSUM);
		this.setChecksumType(DefaultConfiguration.CHECKSUM_TYPE);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getHttpPort() {
		return httpPort;
	}

	public void setHttpPort(int httpPort) {
		this.httpPort = httpPort;
	}

	public int getHttpsPort() {
		return httpsPort;
	}

	public void setHttpsPort(int httpsPort) {
		this.httpsPort = httpsPort;
	}

	public boolean isEnabledHttp() {
		return enabledHttp;
	}

	public void setEnabledHttp(boolean enabledHttp) {
		this.enabledHttp = enabledHttp;
	}

	public MapperServlet getMapperServlet() {
		return mapperServlet;
	}

	public void setMapperServlet(MapperServlet mapperServlet) {
		this.mapperServlet = mapperServlet;
	}

	public String getWebappsDirectory() {
		return webappsDirectory;
	}

	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}

	public SSLOptions getSsloptions() {
		return ssloptions;
	}

	public void setSsloptions(SSLOptions ssloptions) {
		this.ssloptions = ssloptions;
	}

	public String getLogFile() {
		return logFile;
	}

	public void setLogFile(String logFile) {
		this.logFile = logFile;
	}

	public String toString() {
		return "{'" + hostname + "', " + httpPort + ", " + httpsPort + ", " + enabledHttp + ", " + mapperServlet + ", '" + webappsDirectory
				+ "', " + ssloptions + ", '" + logFile + "', '" + warFile + "'}";
	}

	public void checkConfiguration() throws Exception {
		mapperServlet.checkConfiguration();
		if (warFile == null)
			throw new Exception("war file is null!");
		if (!warFile.exists())
			throw new Exception("war file does not exist!");
		if (hostname.isEmpty())
			throw new Exception("gridhttps hostname is empty!");
		if (logFile.isEmpty())
			throw new Exception("gridhttps log filename is empty!");
		if (webappsDirectory.isEmpty())
			throw new Exception("gridhttps webapps directory is empty!");
		if (httpPort <= 0)
			throw new Exception("gridhttps http port is " + httpPort + "!");
		if (httpsPort == 0)
			throw new Exception("gridhttps https port is " + httpsPort + "!");
		if (httpsPort == httpPort)
			throw new Exception("gridhttps http and https port are equal!");
		if (ssloptions.getCertificateFile().isEmpty())
			throw new Exception("gridhttps ssloptions host certificate file is empty!");
		if (ssloptions.getKeyFile().isEmpty())
			throw new Exception("gridhttps ssloptions host key file is empty!");
		if (ssloptions.getTrustStoreDirectory().isEmpty())
			throw new Exception("gridhttps ssloptions trust store directory is empty!");
	}

	public File getWarFile() {
		return warFile;
	}

	public void setWarFile(File warFile) {
		this.warFile = warFile;
	}
	
	public File getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public String getWebdavContextPath() {
		return webdavContextPath;
	}

	public void setWebdavContextPath(String webdavContextPath) {
		this.webdavContextPath = webdavContextPath;
	}

	public String getFiletransferContextPath() {
		return filetransferContextPath;
	}

	public void setFiletransferContextPath(String filetransferContextPath) {
		this.filetransferContextPath = filetransferContextPath;
	}

	public boolean isComputeChecksum() {
		return computeChecksum;
	}

	public void setComputeChecksum(boolean computeChecksum) {
		this.computeChecksum = computeChecksum;
	}

	public String getChecksumType() {
		return checksumType;
	}

	public void setChecksumType(String checksumType) {
		this.checksumType = checksumType;
	}

}