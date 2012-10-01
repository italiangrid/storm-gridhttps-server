package it.grid.storm.webdav.webapp.authorization.methods;


import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory
			.getLogger(OptionsMethodAuthorization.class);

	@Override
	public Map<String, String> getOperationsMap(HttpServletRequest HTTPRequest)
			throws IOException, ServletException {

		Map<String, String> operationsMap = new HashMap<String, String>();

		log.debug("For the method OPTIONS no authorization is needed.");

		return operationsMap;
	}
}