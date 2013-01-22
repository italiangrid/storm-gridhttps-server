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
package it.grid.storm.gridhttps.webapp.webdav.factory;

import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileContentService;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import io.milton.servlet.Config;
import io.milton.servlet.Initable;
import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.contentservice.StormContentService;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StormResourceFactory implements Initable, ResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);
	private FileContentService contentService;
	private File root;
	SecurityManager securityManager;
	Long maxAgeSeconds;
	String contextPath;
	boolean allowDirectoryBrowsing;
	String defaultPage;
	boolean digestAllowed = true;
//	private String ssoPrefix;
	private String localhostname;
	private BackendApi backendApi;
	
	public StormResourceFactory() throws UnknownHostException {
		setRoot(new File(Configuration.getGpfsRootDirectory()));
		setSecurityManager(new NullSecurityManager());
		setContextPath(Configuration.getWebdavContextPath());
        contentService = new StormContentService();
        try {
			this.backendApi = new BackendApi(Configuration.getBackendHostname(), new Long(Configuration.getBackendPort()));
		} catch (ApiException e) {
			log.error(e.getMessage());
		}
        java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
        setLocalhostname(localMachine.getHostName());
	}

	public File getRoot() {
		return root;
	}

	public final void setRoot(File root) {
		log.debug("root: " + root.getAbsolutePath());
		this.root = root;
		if (!root.exists()) {
			log.warn("Root folder does not exist: " + root.getAbsolutePath());
		}
		if (!root.isDirectory()) {
			log.warn("Root exists but is not a directory: " + root.getAbsolutePath());
		}
	}
	
	private String stripPortFromHost(String host) {
		if (host == null || host.isEmpty()) return "";
		return host.indexOf(':') != -1 ? host.substring(0,host.indexOf(':')) : host;
	}
	
	public boolean isLocalResource(String host) {
		if (host == null || host.isEmpty()) return true;
		return host.equals(localhostname);
	}

	public Resource getResource(String host, String uriPath) throws RuntimeApiException, StormResourceException {
		String hostNoPort = stripPortFromHost(host);
//		if (Configuration.getRemoveSpaces())
//			uriPath.replaceAll(" ", Configuration.getRemoveSpacesWith());
		log.debug("getResource: host: " + hostNoPort + " - url:" + uriPath);
		if (isLocalResource(hostNoPort)) {
			StorageArea currentSA = StorageAreaManager.getMatchingSA(uriPath);
			if (currentSA != null) {
				String fsPath = currentSA.getRealPath(uriPath);
				log.debug("real path: " + fsPath);
				File requested = new File(getRoot(), fsPath);
				return resolveFile(host, requested, currentSA);
			}
		}
		return null;
	}

	public StormResource resolveFile(String host, File file, StorageArea storageArea) {
		StormResource r;
		if (!file.exists()) {
			log.debug("file not found: " + file.getAbsolutePath());
			return null;
		} else if (file.isDirectory()) {
			r = new StormDirectoryResource(this, file, storageArea);
		} else {
			r = new StormFileResource(this, file, storageArea);
		}
//		if (r != null) {
//			r.ssoPrefix = ssoPrefix;
//		}
		return r;
	}

//	public File resolvePath(File root, String url) {
//		log.debug("resolve path url: " + url);
//		Path path = Path.path(url);
//		File f = root;
//		for (String s : path.getParts()) {
//			f = new File(f, s);
//		}
//		log.debug("resolve path return file name: " + f.getName());
//		log.debug("resolve path return file path: " + f.getPath());
//		return f;
//	}

	public String getRealm(String host) {
		return securityManager.getRealm(host);
	}

	/**
	 * 
	 * @return - the caching time for files
	 */
	public Long maxAgeSeconds(StormFileResource stormFileResource) {
		return maxAgeSeconds;
	}

	public void setSecurityManager(SecurityManager securityManager) {
		if (securityManager != null) {
			log.debug("securityManager: " + securityManager.getClass());
		} else {
			log.warn("Setting null FsSecurityManager. This WILL cause null pointer exceptions");
		}
		this.securityManager = securityManager;
	}

	public SecurityManager getSecurityManager() {
		return securityManager;
	}

	public void setMaxAgeSeconds(Long maxAgeSeconds) {
		this.maxAgeSeconds = maxAgeSeconds;
	}

	public Long getMaxAgeSeconds() {
		return maxAgeSeconds;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextPath() {
		return contextPath;
	}

	/**
	 * Whether to generate an index page.
	 * 
	 * @return
	 */
	public boolean isAllowDirectoryBrowsing() {
		return allowDirectoryBrowsing;
	}

	public void setAllowDirectoryBrowsing(boolean allowDirectoryBrowsing) {
		this.allowDirectoryBrowsing = allowDirectoryBrowsing;
	}

	/**
	 * if provided GET requests to a folder will redirect to a page of this name
	 * within the folder
	 * 
	 * @return - E.g. index.html
	 */
	public String getDefaultPage() {
		return defaultPage;
	}

	public void setDefaultPage(String defaultPage) {
		this.defaultPage = defaultPage;
	}

	boolean isDigestAllowed() {
		boolean b = digestAllowed && securityManager != null && securityManager.isDigestAllowed();
		if (log.isTraceEnabled()) {
			log.trace("isDigestAllowed: " + b);
		}
		return b;
	}

	public void setDigestAllowed(boolean digestAllowed) {
		this.digestAllowed = digestAllowed;
	}

//	public void setSsoPrefix(String ssoPrefix) {
//		this.ssoPrefix = ssoPrefix;
//	}
//
//	public String getSsoPrefix() {
//		return ssoPrefix;
//	}

	public FileContentService getContentService() {
		return contentService;
	}

	public void setContentService(FileContentService contentService) {
		this.contentService = contentService;
	}
	
	public BackendApi getBackendApi() {
		return backendApi;
	}
	
	public String getLocalhostname() {
		return localhostname;
	}

	private void setLocalhostname(String localhostname) {
		this.localhostname = localhostname;
	}

	@Override
	public void init(Config config, HttpManager manager) {
        manager.setEnableExpectContinue(false);		
	}

	@Override
	public void destroy(HttpManager manager) {
		// TODO Auto-generated method stub
	}
}