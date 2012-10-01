package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.authorization.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory
			.getLogger(PutMethodAuthorization.class);

	@Override
	public Map<String, String> getOperationsMap(HttpServletRequest HTTPRequest)
			throws IOException, ServletException {

		Map<String, String> operationsMap = new HashMap<String, String>();
		String path = null;
		String op = null;

		path = getResourcePath(HTTPRequest);
		if (getOverwriteFromHeader(HTTPRequest))
			op = Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION;
		else
			op = Constants.PREPARE_TO_PUT_OPERATION;
		log.debug("Putting operation: '" + op + "' , path: '" + path
				+ "' into the map.");
		operationsMap.put(op, path);

		return operationsMap;
	}
}