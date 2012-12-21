package it.grid.storm.webdav.factory;

import io.milton.http.ResourceFactory;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.SimpleFileContentService;
import io.milton.http.SecurityManager;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import it.grid.storm.Configuration;
import it.grid.storm.srm.types.TFileType;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

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
		setRoot(new File(Configuration.GPFS_ROOT_DIRECTORY));
		setSecurityManager(new NullSecurityManager());
		setContextPath(Configuration.WEBDAV_CONTEXT_PATH);
        contentService = new SimpleFileContentService();
        try {
			this.backendApi = new BackendApi(Configuration.BACKEND_HOSTNAME, new Long(Configuration.BACKEND_PORT));
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
		log.debug("getResource: host: " + hostNoPort + " - url:" + uriPath);
		if (isLocalResource(hostNoPort)) {
			StorageArea currentSA = StorageAreaManager.getMatchingSA(uriPath);
			if (currentSA != null) {
				String fsPath = currentSA.getRealPath(uriPath);
				log.debug("real path: " + fsPath);
//				Surl surl = new Surl(uriPath);
//				HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
//				UserCredentials user = new UserCredentials(httpHelper);
//				SurlInfo surlInfo;
//				surlInfo = StormBackendApi.getSurlInfo(getBackendApi(), surl.asString(), user, new RecursionLevel(Recursion.NONE));
//				return resolveSurlInfo(host, surlInfo, currentSA);				
				
//				File requested = resolvePath(root, fsPath);
				
				File requested = new File(getRoot(), fsPath);
				return resolveFile(host, requested, currentSA);
			}
		}
		return null;
	}

	public StormResource resolveFile(String host, File file, StorageArea storageArea) {
		StormResource r;
		if (!file.exists()) {
			log.warn("file not found: " + file.getAbsolutePath());
			return null;
		} else if (file.isDirectory()) {
			r = new StormDirectoryResource(this, file, storageArea);
		} else {
			r = new StormFileResource(this, file, storageArea);
		}
		if (r != null) {
			r.ssoPrefix = ssoPrefix;
		}
		return r;
	}
	
	public StormResource resolveSurlInfo(String host, SurlInfo surlInfo, StorageArea storageArea) {
		StormResource resource = null;
		if (surlInfo != null) {
			if (surlInfo.getType().equals(TFileType.DIRECTORY)) {
				resource = new StormDirectoryResource(this, new File(surlInfo.getStfn()), storageArea);
			} else if (surlInfo.getType().equals(TFileType.FILE)) {
				resource = new StormFileResource(this, new File(surlInfo.getStfn()), storageArea);
			}
		} else {
			log.warn("Null surl-info! Impossible to return a StormResource!");
		}
		if (resource != null) {
			resource.ssoPrefix = ssoPrefix;
		}
		return resource;
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
	
	public String getLocalhostname() {
		return localhostname;
	}

	private void setLocalhostname(String localhostname) {
		this.localhostname = localhostname;
	}
}