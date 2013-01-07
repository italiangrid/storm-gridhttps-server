package it.grid.storm.gridhttps.webapp.webdav.authorization.methods;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.factory.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
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
		log.info("ping " + Configuration.getBackendHostname() + ":" + Configuration.getBackendPort());
		try {
			UserCredentials user = new UserCredentials(getHttpHelper());
			PingOutputData output = StormResourceHelper.doPing(Configuration.getBackendHostname(), Configuration.getBackendPort(), user);
			log.info(output.getBeOs());
			log.info(output.getBeVersion());
			log.info(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
		return new AuthorizationStatus(true, "");
	}
}
