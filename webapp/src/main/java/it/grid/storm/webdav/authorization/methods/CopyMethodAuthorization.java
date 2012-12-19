package it.grid.storm.webdav.authorization.methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.AuthorizationStatus;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

public class CopyMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(CopyMethodAuthorization.class);
	
	public CopyMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	public AuthorizationStatus isUserAuthorized() {
		String destinationURL = getHttpHelper().getDestinationHeader();
		boolean hasDestination = destinationURL != null;
		if (hasDestination) { 
			StorageArea reqStorageArea, destStorageArea;
			try {
				reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
				destStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getDestinationURI());
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage());
				return new AuthorizationStatus(false, e.getMessage());
			} catch (IllegalStateException e) {
				log.error(e.getMessage());
				return new AuthorizationStatus(false, e.getMessage());
			}
			String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
			if (askAuth(Constants.CP_FROM_OPERATION, reqPath)) {
				String destPath = destStorageArea.getRealPath(getHttpHelper().getDestinationURI().getPath());
				String operation = getHttpHelper().isOverwriteRequest() ? Constants.CP_TO_OVERWRITE_OPERATION : Constants.CP_TO_OPERATION;
				if (askAuth(operation, destPath)) {
					return new AuthorizationStatus(true, "");
				} else {
					return new AuthorizationStatus(false, "You are not authorized to access the required resource (destination)");
				}
			} else {
				return new AuthorizationStatus(false, "You are not authorized to access the required resource (source)");
			}
		} else {
			return new AuthorizationStatus(false, "no destination header found");
		}
	}

}