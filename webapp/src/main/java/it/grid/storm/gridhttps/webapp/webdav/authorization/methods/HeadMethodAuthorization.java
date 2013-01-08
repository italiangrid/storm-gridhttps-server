package it.grid.storm.gridhttps.webapp.webdav.authorization.methods;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;

public class HeadMethodAuthorization extends AbstractMethodAuthorization {

	public HeadMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public AuthorizationStatus isUserAuthorized() {
		return new AuthorizationStatus(true, "");
	}
}
