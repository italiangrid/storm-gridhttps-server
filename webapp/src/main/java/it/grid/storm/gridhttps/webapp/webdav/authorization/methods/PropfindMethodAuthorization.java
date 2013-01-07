package it.grid.storm.gridhttps.webapp.webdav.authorization.methods;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

public class PropfindMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(PropfindMethodAuthorization.class);

	public PropfindMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	public AuthorizationStatus isUserAuthorized() {
		StorageArea reqStorageArea;
		try {
			reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		} catch (IllegalStateException e) {
			log.error(e.getMessage());
			return new AuthorizationStatus(false, e.getMessage());
		}
		String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
		String operation = Constants.LS_OPERATION;
		if (askAuth(operation, reqPath)) {
			return new AuthorizationStatus(true, "");
		} else {
			return new AuthorizationStatus(false, "You are not authorized to access the requested resource");
		}
	}
}