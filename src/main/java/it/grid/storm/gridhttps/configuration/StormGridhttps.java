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
package it.grid.storm.gridhttps.configuration;

import it.grid.storm.gridhttps.configuration.exceptions.InitException;

import org.italiangrid.utils.https.SSLOptions;

public class StormGridhttps {

	private String hostname;
	private int httpPort;
	private int httpsPort;
	private boolean enabledHttp;
	private MapperServlet mapperServlet;
	private SSLOptions ssloptions;
	private String logFile;
	
	private String webappContextPath;
	private String webdavContextPath;
	private String filetransferContextPath;
	
	private boolean computeChecksum;
	private String checksumType;
	
	private int serverActiveThreadsMax;
	private int serverQueuedThreadsMax;
	
	private boolean vomsCachingEnabled;
	
	public StormGridhttps() {
		setHttpPort(DefaultConfiguration.SERVER_WEBAPP_HTTP_PORT);
		setHttpsPort(DefaultConfiguration.SERVER_WEBAPP_HTTPS_PORT);
		setEnabledHttp(DefaultConfiguration.SERVER_WEBAPP_USE_HTTP);
		setMapperServlet(new MapperServlet());
		SSLOptions sslOptions = new SSLOptions();
		sslOptions.setCertificateFile(DefaultConfiguration.SERVER_WEBAPP_HTTPS_CERTIFICATE_FILE);
		sslOptions.setKeyFile(DefaultConfiguration.SERVER_WEBAPP_HTTPS_KEY_FILE);
		sslOptions.setTrustStoreDirectory(DefaultConfiguration.SERVER_WEBAPP_HTTPS_TRUST_STORE_DIRECTORY);
		sslOptions.setNeedClientAuth(DefaultConfiguration.SERVER_WEBAPP_HTTPS_NEED_CLIENT_AUTH);
		sslOptions.setWantClientAuth(DefaultConfiguration.SERVER_WEBAPP_HTTPS_WANT_CLIENT_AUTH);
		sslOptions.setTrustStoreRefreshIntervalInMsec(DefaultConfiguration.SERVER_WEBAPP_TRUST_STORE_REFRESH_INTERVAL);
		setSsloptions(sslOptions);
		setLogFile(DefaultConfiguration.LOG_FILE);
		setFiletransferContextPath(DefaultConfiguration.WEBAPP_FILETRANSFER_CONTEXTPATH);
		setWebappContextPath(DefaultConfiguration.WEBAPP_CONTEXTPATH);
		setWebdavContextPath(DefaultConfiguration.WEBAPP_WEBDAV_CONTEXTPATH);
		setComputeChecksum(DefaultConfiguration.WEBAPP_COMPUTE_CHECKSUM);
		setChecksumType(DefaultConfiguration.WEBAPP_CHECKSUM_TYPE);
		setServerActiveThreadsMax(DefaultConfiguration.SERVER_ACTIVE_THREADS_MAX);
		setServerQueuedThreadsMax(DefaultConfiguration.SERVER_QUEUED_THREADS_MAX);
		setVomsCachingEnabled(true);
		setHostname(null);
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

	public boolean isHTTPEnabled() {
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

	public void checkConfiguration() throws InitException {
		mapperServlet.checkConfiguration();
		if (logFile.isEmpty())
			throw new InitException("gridhttps log filename is empty!");
		if (httpPort <= 0)
			throw new InitException("gridhttps http port is " + httpPort + "!");
		if (httpsPort == 0)
			throw new InitException("gridhttps https port is " + httpsPort + "!");
		if (httpsPort == httpPort)
			throw new InitException("gridhttps http and https port are equal!");
		if (ssloptions.getCertificateFile().isEmpty())
			throw new InitException("gridhttps ssloptions host certificate file is empty!");
		if (ssloptions.getKeyFile().isEmpty())
			throw new InitException("gridhttps ssloptions host key file is empty!");
		if (ssloptions.getTrustStoreDirectory().isEmpty())
			throw new InitException("gridhttps ssloptions trust store directory is empty!");
		if (serverActiveThreadsMax <= 0)
			throw new InitException("maximum number of threads for webdav-server's requests is not valid!");
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
	
	public int getServerQueuedThreadsMax() {
		return serverQueuedThreadsMax;
	}

	public void setServerQueuedThreadsMax(int serverQueuedThreadsMax) {
		this.serverQueuedThreadsMax = serverQueuedThreadsMax;
	}

	public int getServerActiveThreadsMax() {
		return serverActiveThreadsMax;
	}

	public void setServerActiveThreadsMax(int serverActiveThreadsMax) {
		this.serverActiveThreadsMax = serverActiveThreadsMax;
	}

	public String getWebappContextPath() {
		return webappContextPath;
	}

	public void setWebappContextPath(String webappContextPath) {
		this.webappContextPath = webappContextPath;
	}

  public boolean isVomsCachingEnabled() {
    return vomsCachingEnabled;
  }
  
  public void setVomsCachingEnabled(boolean vomsCachingEnabled) {
    this.vomsCachingEnabled = vomsCachingEnabled;
  }

}