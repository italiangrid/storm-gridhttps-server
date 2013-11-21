package it.grid.storm.gridhttps.webapp.common.factory;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.ResourceFactory;
import io.milton.http.SecurityManager;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileContentService;
import io.milton.http.fs.NullSecurityManager;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.contentservice.StormContentService;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

public abstract class StormFactory implements ResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(StormFactory.class);

	private FileContentService contentService;
	private File root;
	SecurityManager securityManager;
	Long maxAgeSeconds;
	String contextPath;
	boolean allowDirectoryBrowsing = true;

	String defaultPage;
	boolean digestAllowed = true;
	private String localhostname;
	
	public StormFactory(String beHost, int bePort, File root, String contextPath) throws UnknownHostException, ApiException {
		log.debug(this.getClass().getSimpleName() + " constructor");
		setRoot(root);
		setContextPath(contextPath);
		setSecurityManager(new NullSecurityManager());
		setContentService(new StormContentService());
		setLocalhostname(java.net.InetAddress.getLocalHost().getHostName());
		log.debug(this.getClass().getSimpleName() + " created");
	}

	private boolean isRoot(String path) {
		return isRoot(new File(path));
	}

	private boolean isRoot(File file) {
		return file.getAbsolutePath().equals(getRoot().getAbsolutePath());
	}

	public File getRoot() {
		return root;
	}

	public final void setRoot(File root) {
		log.debug("root: " + root.getAbsolutePath());
		this.root = root;
		if (root.exists()) {
			if (!root.isDirectory()) {
				log.warn("Root exists but is not a directory: " + root.getAbsolutePath());
			}
		} else {
			log.warn("Root folder does not exist: " + root.getAbsolutePath());
		}
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

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextPath() {
		return contextPath;
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

	private String stripPortFromHost(String host) {
		if (host == null || host.isEmpty())
			return "";
		return host.indexOf(':') != -1 ? host.substring(0, host.indexOf(':')) : host;
	}

	public boolean isLocalResource(String host) {
		if (host == null || host.isEmpty())
			return true;
		return host.equals(localhostname);
	}

	@Override
	public Resource getResource(String host, String path) throws NotAuthorizedException, BadRequestException {
		Resource r = null;
		String hostNoPort = stripPortFromHost(host);
		path = stripContext(path);
		log.debug("getResource: host: " + hostNoPort + " - url:" + path);
		if (!isRoot(path)) {
		  StorageArea currentSA = StorageAreaManager.getMatchingSA(path);
		  if (currentSA != null) {
				String fsPath = currentSA.getRealPath(path);
				log.debug("real path: " + fsPath);
				File requested = new File(getRoot(), fsPath);
				r = resolveFile(requested);
			} else {
				log.warn("Unable to identify a StorageArea that matches: " + path);
			}
		} else {
			log.debug("get root resource!");
		}
		return r;
	}

	private String stripContext(String path) {
		if (getContextPath().isEmpty())
			return path;
		return path.replaceFirst(File.separator + getContextPath(), "");
	}

	public abstract StormResource getDirectoryResource(File directory);
	
	public abstract StormResource getFileResource(File file);
	
	/**
	 * Returns the StormResource associated to the file, or null if file doesn't exist
	 * 
	 **/
	public StormResource resolveFile(File file) {
		
		if (file.exists()) {
			if (file.isDirectory()) {
				return getDirectoryResource(file);
			} else {
				return getFileResource(file);
			}
		} 
		return null;
	}
	
	/**
	 * Returns the StormResource associated to the file, or null if file doesn't exist
	 * 
	 **/
	public StormResource resolveFile(File parent, String childname) {
		
		return resolveFile(new File(parent, childname));
	}

	public StormResource resolveFile(SurlInfo surlInfo) {
		StorageArea storageArea = StorageAreaManager.getMatchingSA(surlInfo.getStfn());
		return resolveFile(surlInfo, storageArea);
	}
	
	public StormResource resolveFile(SurlInfo surlInfo, StorageArea storageArea) {
		if (surlInfo == null) {
			log.debug("Resolve file error: received a null surl-info!");
			return null;
		}
		if (storageArea == null) {
			log.debug("Resolve file error: received a null storage-area!");
			return null;
		}
		if (!isSuccessful(surlInfo.getStatus().getStatusCode())) {
			log.debug("Resolve file error: surl status is " + surlInfo.getStatus().getStatusCode() + " " + surlInfo.getStatus().getExplanation());
			return null;
		}
		if (surlInfo.getType() == null) {
			log.debug("Resolve file error: surl-info type is null!");
			return null;
		}
		File file = new File(storageArea.getRealPath(surlInfo.getStfn()));
		if (!storageArea.isOwner(file)) {
			log.debug("Resolve file error: storage area " + storageArea.getName() + " isn't the owner of " + surlInfo.getStfn());
			return null;
		}
		return resolveFile(file);
	}

	private boolean isSuccessful(TStatusCode status) {
		return (!status.equals(TStatusCode.SRM_FAILURE) 
			&& !status.equals(TStatusCode.SRM_INVALID_PATH));
	}
	
	public String getRealm(String host) {
		return getSecurityManager().getRealm(host);
	}

	public boolean isDigestAllowed() {
		boolean b = getSecurityManager() != null && getSecurityManager().isDigestAllowed();
		if (log.isTraceEnabled()) {
			log.trace("isDigestAllowed: " + b);
		}
		return b;
	}

	public boolean isAllowDirectoryBrowsing() {
		return allowDirectoryBrowsing;
	}

	public void setAllowDirectoryBrowsing(boolean allowDirectoryBrowsing) {
		this.allowDirectoryBrowsing = allowDirectoryBrowsing;
	}

}