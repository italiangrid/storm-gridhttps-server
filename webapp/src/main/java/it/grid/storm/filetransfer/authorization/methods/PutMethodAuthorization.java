package it.grid.storm.filetransfer.authorization.methods;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.Configuration;
import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.AuthorizationStatus;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.backendApi.StormBackendApi;
import it.grid.storm.data.Surl;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(PutMethodAuthorization.class);
	
	public PutMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	private String stripContext(String url) {
		return url.replaceFirst(File.separator + Configuration.FILETRANSFER_CONTEXTPATH, "");
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
					AuthorizationStatus status = doPrepareToPutStatus(resource, reqStorageArea);
					if (status.isAuthorized()) {
						if (askAuth(Constants.WRITE_OPERATION, reqPath)) {
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
				return new AuthorizationStatus(false, "File not exist! You must do a prepare-to-put on surl '" + (new Surl(resource, reqStorageArea)).asString() + "' before!"); 
			}
		} else {
			return new AuthorizationStatus(false, "No storage area matched with path = " + path);
		}
	}

	private AuthorizationStatus doPrepareToPutStatus(File resource, StorageArea reqStorageArea) {
		log.debug("Check for a prepare-to-put");
		Surl surl = new Surl(resource, reqStorageArea);
		BackendApi backend;
		SurlArrayRequestOutputData outputSPtP;
		try {
			backend = StormBackendApi.getBackend(Configuration.BACKEND_HOSTNAME, Configuration.BACKEND_PORT);
			outputSPtP = StormBackendApi.prepareToPutStatus(backend, surl.asString(), getUser());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		} catch (StormResourceException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		}
		String requestStatus = outputSPtP.getStatus().getStatusCode().getValue();
		log.info("Request-status: " + requestStatus);
		String surlStatus = outputSPtP.getStatus(surl.asString()).getStatusCode().getValue();
		log.info("Surl-status: " + surlStatus);
		if (requestStatus.equals("SRM_SUCCESS") && surlStatus.equals("SRM_SPACE_AVAILABLE")) {
			return new AuthorizationStatus(true, "");
		} else {
			return new AuthorizationStatus(false, "You must do a prepare-to-put on surl '" + surl.asString() + "' before!");
		}
	}
}
