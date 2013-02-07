package it.grid.storm.gridhttps.webapp.data;

import java.io.File;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
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
	boolean allowDirectoryBrowsing = true;

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
			if (!isRoot(path)) {
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
				log.debug("get root resource!");
			}
		} else {
			log.warn("Unable to get a non-local resource!");
		}
		return r;
	}

	private String stripContext(String path) {
		if (getContextPath().isEmpty())
			return path;
		return path.replaceFirst(File.separator + getContextPath(), "");
	}

	public abstract StormResource getDirectoryResource(File directory, StorageArea storageArea);
	
	public abstract StormResource getDirectoryResource(File directory, StorageArea storageArea, SurlInfo surlinfo);

	public abstract StormResource getFileResource(File file, StorageArea storageArea);

	public abstract StormResource getFileResource(File file, StorageArea storageArea, SurlInfo surlinfo);

	private boolean isDirectory(TFileType type) {
		if (type != null) {
			return type.equals(TFileType.DIRECTORY);
		}
		return false;
	}
		
	public StormResource resolveFile(String host, File file, StorageArea storageArea) {
		SurlInfo detail = null;
		try {
			detail = StormResourceHelper.doLimitedLsDetailed(this.getBackendApi(), file).get(0);
			return resolveFile(detail, storageArea);
		} catch (RuntimeApiException e) {
			log.error("retrieving detailed info for '" + file + "': " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.debug(file + " not exists! Got a SRM_FAILURE with reason: " + e.getReason());
		} catch (TooManyResultsException e) {
			log.warn(e.getReason());
		}
		return null;
	}

	public StormResource resolveFile(SurlInfo surlInfo) {
		StorageArea storageArea = StorageAreaManager.getMatchingSA(surlInfo.getStfn());
		return resolveFile(surlInfo, storageArea);
	}
	
	public StormResource resolveFile(SurlInfo surlInfo, StorageArea storageArea) {
		ArrayList<TStatusCode> notSuccessful = new ArrayList<TStatusCode>() {
			private static final long serialVersionUID = 1L;
			{
				add(TStatusCode.SRM_FAILURE);
				add(TStatusCode.SRM_INVALID_PATH);
			}
		};
		StormResource r = null;
		if (surlInfo != null) {
			if (!notSuccessful.contains(surlInfo.getStatus().getStatusCode())) {
				if (surlInfo.getType() != null) {
					if (surlInfo.getStfn().startsWith(storageArea.getStfnRoot())) {
						File file = new File(storageArea.getRealPath(surlInfo.getStfn()));
						if (isDirectory(surlInfo.getType())) {
							r = getDirectoryResource(file, storageArea, surlInfo);
						} else {
							r = getFileResource(file, storageArea, surlInfo);
						}
					} else {
						log.error("surl-info does not match with given storage-area fsRoot!");
					}
				} else {
					log.error(surlInfo.getStfn() + " type is null! Even if status is " + surlInfo.getStatus().toString());
				}
			} else {
				log.warn(surlInfo.getStfn() + " not exists! Got a " + surlInfo.getStatus().getStatusCode() + " with reason: " + surlInfo.getStatus().getExplanation());
			}
		} else {
			log.error("received a null surl-info! Can't resolve resource!");
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

	public boolean isAllowDirectoryBrowsing() {
		return allowDirectoryBrowsing;
	}

	public void setAllowDirectoryBrowsing(boolean allowDirectoryBrowsing) {
		this.allowDirectoryBrowsing = allowDirectoryBrowsing;
	}

}