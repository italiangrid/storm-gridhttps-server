package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.SimpleFileContentService;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.backendApi.StormBackendApi;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileSystemResourceFactory implements ResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(FileSystemResourceFactory.class);
	private FileContentService contentService;
	private File root;
	SecurityManager securityManager;
	Long maxAgeSeconds;
	String contextPath;
	boolean allowDirectoryBrowsing;
	String defaultPage;
	boolean digestAllowed = true;
	private String ssoPrefix;
	private BackendApi backend;
	
	private String localhostname;

	public FileSystemResourceFactory() throws RuntimeApiException, UnknownHostException {
		log.info("FileSystem Resource factory init");
		setRoot(new File(Configuration.getGpfsRootDirectory()));
		setSecurityManager(new NullSecurityManager());
		setContextPath(Configuration.getFileTransferContextPath());
        setContentService(new SimpleFileContentService());
        setBackend(StormBackendApi.getBackend(Configuration.getBackendHostname(), Configuration.getBackendPort()));
        java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
        setLocalhostname(localMachine.getHostName());
	}
	
	public BackendApi getBackend() {
		return backend;
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
	
	public boolean isLocalResource(String host) {
		String hostNoPort = host.indexOf(':') != -1 ? host.substring(0,host.indexOf(':')) : host;
		return hostNoPort.equals(getLocalhostname());
	}

	private String stripContext(String urlPath) {
		return urlPath.replaceFirst(File.separator + getContextPath(), "");
	}
	
	public Resource getResource(String host, String url) {
		log.debug("getResource: host: " + host + " - url:" + url);
		if (isLocalResource(host)) {
			String stripped = stripContext(url);
			log.debug("context-cleaned: " + stripped);
			StorageArea currentSA = StorageAreaManager.getMatchingSA(stripped);
			if (currentSA != null) {
				String realPath = currentSA.getRealPath(stripped);
				log.debug("real-path: " + realPath);
				File requested = resolvePath(root, realPath);
				return resolveFile(host, requested, currentSA);
			}
		}
		return null;
	}

	public FileResource resolveFile(String host, File file, StorageArea storageArea) {
		FileResource resource = null;
		if (file.exists()) {
			resource = new FileResource(host, this, file, contentService, storageArea);
			resource.ssoPrefix = ssoPrefix;
		} else {
			log.warn("file not found: " + file.getAbsolutePath());
		}
		return resource;
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
	public Long maxAgeSeconds(FileResource stormFileResource) {
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

//	private String stripContext(String url) {
//		if (this.contextPath != null && contextPath.length() > 0) {
//			url = url.replaceFirst('/' + contextPath, "");
//			log.debug("stripped context: " + url);
//		}
//		return url;
//	}

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
	
	public String getLocalhostname() {
		return localhostname;
	}

	private void setLocalhostname(String localhostname) {
		this.localhostname = localhostname;
	}

	private void setBackend(BackendApi backend) {
		this.backend = backend;
	}

}
