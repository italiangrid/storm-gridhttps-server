package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.factory.StormResourceHelper;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(OptionsMethodAuthorization.class);

	@Override
	public Map<String, String> getOperationsMap(HttpServletRequest HTTPRequest) throws IOException, ServletException {

		Map<String, String> operationsMap = new HashMap<String, String>();

		log.debug("For the method OPTIONS no authorization is needed.");

		StormResourceHelper helper = new StormResourceHelper(HTTPRequest);

		// ping
		BackendApi be = helper.createBackend();

		log.debug("ping:");

		try {
			PingOutputData pud = be.ping(helper.getUserDN(), helper.getUserFQANS());
			log.debug("ping output:\n" + pud.toString());
		} catch (Exception e) {
			log.warn(e.getMessage());
		}

		return operationsMap;
	}
}
