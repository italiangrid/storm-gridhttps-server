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

public class GetMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(GetMethodAuthorization.class);
	
	public GetMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	private String stripContext(String url) {
		return url.replaceFirst(Configuration.FILETRANSFER_CONTEXTPATH, "");
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
			File resource = new File(path);
			if (resource.exists()) {
				if (resource.isFile()) {
					AuthorizationStatus status = doPrepareToGetStatus(resource);
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
					return new AuthorizationStatus(false, "resource required is not a file"); 
				}
			} else {
				return new AuthorizationStatus(false, "file does not exist"); 
			}
		} else {
			return new AuthorizationStatus(false, "no storage area matched with path = " + path);
		}
	}

	private AuthorizationStatus doPrepareToGetStatus(File resource) {
		log.debug("Check for a prepare-to-get");
		Surl surl = new Surl(resource);
		BackendApi backend;
		SurlArrayRequestOutputData outputSPtG;
		try {
			backend = StormBackendApi.getBackend(Configuration.stormBackendHostname, Configuration.stormBackendPort);
			outputSPtG = StormBackendApi.prepareToGetStatus(backend, surl.asString(), getUser());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		} catch (StormResourceException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		}
		log.info(outputSPtG.getStatus(surl.asString()).toString());
		if (!outputSPtG.isSuccess()) {
			return new AuthorizationStatus(false, "You must do a prepare-to-get on surl '" + surl.asString() + "' before!");
		}
		return new AuthorizationStatus(true, "");
	}
	
}
