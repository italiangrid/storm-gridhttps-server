package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.Configuration;
import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.AuthorizationStatus;
import it.grid.storm.authorization.UserCredentials;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.webdav.factory.StormResourceHelper;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(OptionsMethodAuthorization.class);

	public OptionsMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public AuthorizationStatus isUserAuthorized() {
		/* ping storm-backend if method = OPTIONS */
		log.info("ping " + Configuration.BACKEND_HOSTNAME + ":" + Configuration.BACKEND_PORT);
		try {
			UserCredentials user = new UserCredentials(getHttpHelper());
			PingOutputData output = StormResourceHelper.doPing(Configuration.BACKEND_HOSTNAME, Configuration.BACKEND_PORT, user);
			log.info(output.getBeOs());
			log.info(output.getBeVersion());
			log.info(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
		return new AuthorizationStatus(true, "");
	}
}
