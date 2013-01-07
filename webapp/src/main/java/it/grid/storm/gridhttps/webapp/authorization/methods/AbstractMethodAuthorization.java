package it.grid.storm.gridhttps.webapp.authorization.methods;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	private HttpHelper httpHelper;
	private UserCredentials user;

	public AbstractMethodAuthorization(HttpHelper httpHelper) {
		this.setHttpHelper(httpHelper);
		this.setUser(new UserCredentials(getHttpHelper()));
	}

	protected boolean askAuth(String operation, String path) {
		log.debug("Asking authorization for operation " + operation + " on " + path);
		UserCredentials user = new UserCredentials(httpHelper);
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
		log.debug("Response: " + response);
		return response;
	}

	public abstract AuthorizationStatus isUserAuthorized();

	public HttpHelper getHttpHelper() {
		return httpHelper;
	}

	public UserCredentials getUser() {
		return user;
	}

	private void setUser(UserCredentials user) {
		this.user = user;
	}
	
	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

}
