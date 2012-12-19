package it.grid.storm.authorization.methods;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.StormAuthorizationUtils;
import it.grid.storm.authorization.UnauthorizedException;
import it.grid.storm.authorization.UserCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	protected HttpHelper httpHelper;

	public AbstractMethodAuthorization(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
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

	public abstract boolean isUserAuthorized() throws UnauthorizedException;

	protected HttpHelper getHttpHelper() {
		return httpHelper;
	}

}
