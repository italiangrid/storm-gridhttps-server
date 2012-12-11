package it.grid.storm.webdav.factory;

import io.milton.common.Path;
import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.SimpleFileContentService;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import it.grid.storm.Configuration;
import it.grid.storm.storagearea.StorageAreaManager;
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
	
	private String localhostname;
	private BackendApi backendApi;
	
	public StormResourceFactory() throws UnknownHostException {
		setRoot(new File("/"));
		setSecurityManager(new NullSecurityManager());
		setContextPath("");
        contentService = new SimpleFileContentService();
        try {
			this.backendApi = new BackendApi(Configuration.stormBackendHostname, new Long(Configuration.stormBackendPort));
		} catch (ApiException e) {
			log.error(e.getMessage());
		}
        java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
        localhostname = localMachine.getHostName();
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

	public Resource getResource(String host, String url) {
		host = stripPortFromHost(host);
		log.debug("getResource: host: " + host + " - url:" + url);
		if (isLocalResource(host)) {
			String stfnRoot = "/" + url.replaceFirst("/", "").split("/")[0];
			log.debug("searching for stfnRoot: " + stfnRoot);
			String fsRoot = StorageAreaManager.getInstance().getStfnToFsRoot().get(stfnRoot);
			if (fsRoot != null) {
				url = url.replaceFirst(stfnRoot, fsRoot);
				log.debug("stripped context: " + url);				
				File requested = resolvePath(root, url);
				return resolveFile(host, requested);
			}
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