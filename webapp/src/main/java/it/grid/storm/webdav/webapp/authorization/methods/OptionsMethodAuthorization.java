package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.Configuration;
import it.grid.storm.webdav.webapp.factory.StormResourceHelper;
import it.grid.storm.webdav.webapp.factory.exceptions.RuntimeApiException;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(OptionsMethodAuthorization.class);
	
	public OptionsMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		log.info("ping " + Configuration.stormBackendHostname + ":" + Configuration.stormBackendPort);
		PingOutputData output;
		try {
			output = StormResourceHelper.doPing(Configuration.stormBackendHostname, Configuration.stormBackendPort);
			log.info(output.getBeOs());
			log.info(output.getBeVersion());
			log.info(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
		return true;
	}
}
