package it.grid.storm.gridhttps.webapp.data;

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
import it.grid.storm.gridhttps.webapp.contentservice.StormContentService;
import it.grid.storm.gridhttps.webapp.data.StormResource;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.srm.types.TFileType;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

public abstract class StormFactory implements ResourceFactory {

	private static final Logger log = LoggerFactory.getLogger(StormFactory.class);
	
	private FileContentService contentService;
	private File root;
	SecurityManager securityManager;
	Long maxAgeSeconds;
	String contextPath;
	boolean allowDirectoryBrowsing;
	String defaultPage;
	boolean digestAllowed = true;
	private String localhostname;
	private BackendApi backendApi;
	
	public StormFactory(String beHost, int bePort, File root, String contextPath) throws UnknownHostException, ApiException {
		setRoot(root);
		setContextPath(contextPath);
		setBackendApi(new BackendApi(beHost, new Long(bePort)));
		setSecurityManager(new NullSecurityManager());
		setContentService(new StormContentService());
		setLocalhostname(java.net.InetAddress.getLocalHost().getHostName());
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

	public BackendApi getBackendApi() {
		return backendApi;
	}

	private void setBackendApi(BackendApi backendApi) {
		this.backendApi = backendApi;
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
		if (isLocalResource(hostNoPort)) {
			StorageArea currentSA = StorageAreaManager.getMatchingSA(path);
			if (currentSA != null) {
				String fsPath = currentSA.getRealPath(path);
				log.debug("real path: " + fsPath);
				File requested = new File(getRoot(), fsPath);
				r = resolveFile(host, requested, currentSA);
			} else {
				log.warn("Unable to identify a StorageArea that matches: " + path);
			}
		} else {
			log.warn("Unable to get a non-local resource!");
		}
		return r;
	}

	private String stripContext(String path) {
		return path.replaceFirst(this.getContextPath(), "");
	}

	public abstract StormResource getDirectoryResource(File directory, StorageArea storageArea);
	public abstract StormResource getFileResource(File file, StorageArea storageArea);
	
	public StormResource resolveFile(String host, File file, StorageArea storageArea) {
		SurlInfo detail;
		try {
			detail = StormResourceHelper.doLs(this.getBackendApi(), file).get(0);
			if (!(detail.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) && detail.getStatus().getStatusCode().equals(TStatusCode.SRM_FAILURE))) {
				return resolveFile(detail);
			} else {
				log.warn(detail.getStfn() + " status is " + detail.getStatus().getStatusCode().getValue());
			}
		} catch (RuntimeApiException e) {
			log.error(e.getMessage() + ": " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.debug(e.getReason());
			log.debug(file + " does not exist!");
		}
		return null;
	}
	
	public StormResource resolveFile(SurlInfo surlInfo) {
		StormResource r = null;
		if (surlInfo != null) {
			StorageArea storageArea = StorageAreaManager.getMatchingSA(surlInfo.getStfn());
			File file = new File(storageArea.getRealPath(surlInfo.getStfn()));
			if (!(surlInfo.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) && surlInfo.getStatus().getStatusCode()
					.equals(TStatusCode.SRM_FAILURE))) {
				if (surlInfo.getType() != null) {
					if (surlInfo.getType().equals(TFileType.DIRECTORY)) {
						r = getDirectoryResource(file, storageArea);
					} else {
						r = getFileResource(file, storageArea);
					}
				} else {
					log.warn("resource type is null!");
				}
			} else {
				log.warn(surlInfo.getStfn() + " status is: " + surlInfo.getStatus().getStatusCode().getValue());
			}
		} else {
			log.warn("surl-info is null");
		}
		return r;
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
	
}