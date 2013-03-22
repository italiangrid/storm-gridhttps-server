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
package it.grid.storm.gridhttps.server.data;

import it.grid.storm.gridhttps.server.DefaultConfiguration;
import it.grid.storm.gridhttps.server.exceptions.InitException;

import java.io.File;

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
	
	private int mapActiveThreadsMin;
	private int mapActiveThreadsMax;
	private int mapQueuedThreadsMax;
	
	private int davActiveThreadsMax;
	private int davQueuedThreadsMax;

	public StormGridhttps() {
		this.setHttpPort(DefaultConfiguration.SERVER_WEBAPP_HTTP_PORT);
		this.setHttpsPort(DefaultConfiguration.SERVER_WEBAPP_HTTPS_PORT);
		this.setEnabledHttp(DefaultConfiguration.SERVER_WEBAPP_USE_HTTP);
		MapperServlet mapperServlet = new MapperServlet();
		this.setMapperServlet(mapperServlet);
		this.setWebappsDirectory(DefaultConfiguration.SERVER_WEBAPP_DIRECTORY);
		SSLOptions sslOptions = new SSLOptions();
		sslOptions.setCertificateFile(DefaultConfiguration.SERVER_WEBAPP_HTTPS_CERTIFICATE_FILE);
		sslOptions.setKeyFile(DefaultConfiguration.SERVER_WEBAPP_HTTPS_KEY_FILE);
		sslOptions.setTrustStoreDirectory(DefaultConfiguration.SERVER_WEBAPP_HTTPS_TRUST_STORE_DIRECTORY);
		sslOptions.setNeedClientAuth(DefaultConfiguration.SERVER_WEBAPP_HTTPS_NEED_CLIENT_AUTH);
		sslOptions.setWantClientAuth(DefaultConfiguration.SERVER_WEBAPP_HTTPS_WANT_CLIENT_AUTH);
		this.setSsloptions(sslOptions);
		this.setLogFile(DefaultConfiguration.LOG_FILE);
		this.setWarFile(null);
		this.setFiletransferContextPath(DefaultConfiguration.WEBAPP_FILETRANSFER_CONTEXTPATH);
		this.setWebdavContextPath(DefaultConfiguration.WEBAPP_WEBDAV_CONTEXTPATH);
		this.setRootDirectory(new File(DefaultConfiguration.WEBAPP_GPFS_ROOTDIRECTORY));
		this.setComputeChecksum(DefaultConfiguration.WEBAPP_COMPUTE_CHECKSUM);
		this.setChecksumType(DefaultConfiguration.WEBAPP_CHECKSUM_TYPE);
		this.setDavActiveThreadsMax(DefaultConfiguration.DAV_ACTIVE_THREADS_MAX);
		this.setDavQueuedThreadsMax(DefaultConfiguration.DAV_QUEUED_THREADS_MAX);
		this.setMapActiveThreadsMin(DefaultConfiguration.MAP_ACTIVE_THREADS_MIN);
		this.setMapActiveThreadsMax(DefaultConfiguration.MAP_ACTIVE_THREADS_MAX);
		this.setMapQueuedThreadsMax(DefaultConfiguration.MAP_QUEUED_THREADS_MAX);
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

	public void checkConfiguration() throws InitException {
		mapperServlet.checkConfiguration();
		if (warFile == null)
			throw new InitException("war file is null!");
		if (!warFile.exists())
			throw new InitException("war file does not exist!");
		if (hostname.isEmpty())
			throw new InitException("gridhttps hostname is empty!");
		if (logFile.isEmpty())
			throw new InitException("gridhttps log filename is empty!");
		if (webappsDirectory.isEmpty())
			throw new InitException("gridhttps webapps directory is empty!");
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
		if (mapActiveThreadsMax <= 0)
			throw new InitException("maximum number of threads for mapping-servlet's requests is not valid!");
		if (davActiveThreadsMax <= 0)
			throw new InitException("maximum number of threads for webdav-server's requests is not valid!");
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

	public int getMapQueuedThreadsMax() {
		return mapQueuedThreadsMax;
	}

	public void setMapQueuedThreadsMax(int mapQueuedThreadsMax) {
		this.mapQueuedThreadsMax = mapQueuedThreadsMax;
	}

	public int getDavQueuedThreadsMax() {
		return davQueuedThreadsMax;
	}

	public void setDavQueuedThreadsMax(int davQueuedThreadsMax) {
		this.davQueuedThreadsMax = davQueuedThreadsMax;
	}

	public int getMapActiveThreadsMin() {
		return mapActiveThreadsMin;
	}

	public void setMapActiveThreadsMin(int mapActiveThreadsMin) {
		this.mapActiveThreadsMin = mapActiveThreadsMin;
	}
	
	public int getMapActiveThreadsMax() {
		return mapActiveThreadsMax;
	}

	public void setMapActiveThreadsMax(int mapActiveThreadsMax) {
		this.mapActiveThreadsMax = mapActiveThreadsMax;
	}

	public int getDavActiveThreadsMax() {
		return davActiveThreadsMax;
	}

	public void setDavActiveThreadsMax(int davActiveThreadsMax) {
		this.davActiveThreadsMax = davActiveThreadsMax;
	}

}