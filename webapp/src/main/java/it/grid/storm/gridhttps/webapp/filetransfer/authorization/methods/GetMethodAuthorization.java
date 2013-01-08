package it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.backendApi.StormBackendApi;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class GetMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(GetMethodAuthorization.class);
	
	public GetMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	private String stripContext(String url) {
		return url.replaceFirst(File.separator + Configuration.getFileTransferContextPath(), "");
	}

	public AuthorizationStatus isUserAuthorized() {
		StorageArea reqStorageArea;
		String path = stripContext(getHttpHelper().getRequestStringURI());
		try {
			reqStorageArea = StorageAreaManager.getMatchingSA(path);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return new AuthorizationStatus(false, e.getMessage());
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return new AuthorizationStatus(false, e.getMessage());
		}
		if (reqStorageArea != null) {
			String reqPath = reqStorageArea.getRealPath(path);
			File resource = new File(reqPath);
			if (resource.exists()) {
				if (resource.isFile()) {
					AuthorizationStatus status = doPrepareToGetStatus(resource, reqStorageArea);
					if (status.isAuthorized()) {
						if (askAuth(Constants.READ_OPERATION, reqPath)) {
							return new AuthorizationStatus(true, "");
						} else {
							return new AuthorizationStatus(false, "You are not authorized to access the required resource"); 
						}
					} else {
						return status;
					}
				} else {
					return new AuthorizationStatus(false, "Resource required is not a file"); 
				}
			} else {
				return new AuthorizationStatus(false, "File does not exist"); 
			}
		} else {
			return new AuthorizationStatus(false, "No storage area matched with path = " + path);
		}
	}

	private AuthorizationStatus doPrepareToGetStatus(File resource, StorageArea reqStorageArea) {
		log.debug("Check for a prepare-to-get");
		Surl surl = new Surl(resource, reqStorageArea);
		BackendApi backend;
		SurlArrayRequestOutputData outputSPtG;
		try {
			backend = StormBackendApi.getBackend(Configuration.getBackendHostname(), Configuration.getBackendPort());
			outputSPtG = StormBackendApi.prepareToGetStatus(backend, surl.asString(), getUser());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		} catch (StormResourceException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		}
		String requestStatus = outputSPtG.getStatus().getStatusCode().getValue();
		log.info("Request-status: " + requestStatus);
		String surlStatus = outputSPtG.getStatus(surl.asString()).getStatusCode().getValue();
		log.info("Surl-status: " + surlStatus);
		if (requestStatus.equals("SRM_SUCCESS") && surlStatus.equals("SRM_FILE_PINNED")) {
			return new AuthorizationStatus(true, "");
		} else {
			return new AuthorizationStatus(false, "You must do a prepare-to-get on surl '" + surl.asString() + "' before!");
		}
	}
	
}