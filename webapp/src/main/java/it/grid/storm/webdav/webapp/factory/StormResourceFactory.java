package it.grid.storm.webdav.webapp.factory;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.SimpleFileContentService;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import it.grid.storm.webdav.webapp.Configuration;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StormResourceFactory implements ResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);
	private FileContentService contentService;
	private File root;
	SecurityManager securityManager;
	Long maxAgeSeconds;
	String contextPath;
	boolean allowDirectoryBrowsing;
	String defaultPage;
	boolean digestAllowed = true;
	private String ssoPrefix;
	
	private BackendApi backendApi;
	
	public StormResourceFactory() {
		setRoot(new File(Configuration.storageAreaRootDir));
		setSecurityManager(new NullSecurityManager());
		setContextPath(Configuration.storageAreaName);
        contentService = new SimpleFileContentService();
        try {
			this.backendApi = new BackendApi(Configuration.stormBackendHostname, new Long(Configuration.stormBackendPort));
		} catch (ApiException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
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
	
	public boolean isLocalResource(String host) throws UnknownHostException {
		String hostNoPort = host.indexOf(':') != -1 ? host.substring(0,host.indexOf(':')) : host;
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		log.debug("localhost: " + localMachine.getHostName());
		log.debug("host: " + hostNoPort);
		return localMachine.getHostName().equals(hostNoPort);
	}

	public Resource getResource(String host, String url) {
		log.debug("getResource: host: " + host + " - url:" + url);
		boolean isLocal;
		try {
			isLocal = (isLocalResource(host));
		} catch (UnknownHostException e) {
			log.error(e.getMessage());
			return null;
		}
		if (isLocal) {
			url = stripContext(url);
			File requested = resolvePath(root, url);
			return resolveFile(host, requested);
		}
		return null;
	}

	public StormResource resolveFile(String host, File file) {
		StormResource r;
		if (!file.exists()) {
			log.warn("file not found: " + file.getAbsolutePath());
			return null;
		} else if (file.isDirectory()) {
			r = new StormDirectoryResource(host, this, file);
		} else {
			r = new StormFileResource(host, this, file);
		}
		if (r != null) {
			r.ssoPrefix = ssoPrefix;
		}
		return r;
	}

	public File resolvePath(File root, String url) {
		log.debug("resolve path url: " + url);
		Path path = Path.path(url);
		File f = root;
		for (String s : path.getParts()) {
			f = new File(f, s);
		}
		log.debug("resolve path return file name: " + f.getName());
		log.debug("resolve path return file path: " + f.getPath());
		return f;
	}

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

	private String stripContext(String url) {
		if (this.contextPath != null && contextPath.length() > 0) {
			url = url.replaceFirst('/' + contextPath, "");
			log.debug("stripped context: " + url);
		}
		return url;
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

	public void setSsoPrefix(String ssoPrefix) {
		this.ssoPrefix = ssoPrefix;
	}

	public String getSsoPrefix() {
		return ssoPrefix;
	}

	public FileContentService getContentService() {
		return contentService;
	}

	public void setContentService(FileContentService contentService) {
		this.contentService = contentService;
	}
	
	public BackendApi getBackendApi() {
		return backendApi;
	}
}
