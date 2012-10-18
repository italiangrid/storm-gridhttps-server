package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.authorization.Constants;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MkcolMethodAuthorization extends AbstractMethodAuthorization {

	public MkcolMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
		this.HTTPRequest = HTTPRequest;
	}

	private static final Logger log = LoggerFactory.getLogger(MkcolMethodAuthorization.class);

	@Override
	public Map<String, String> getOperationsMap() throws IOException, ServletException {

		Map<String, String> operationsMap = new HashMap<String, String>();

		String path = getResourcePath();
		String op = Constants.MKDIR_OPERATION;
		log.debug("Putting operation: '" + op + "' , path: '" + path + "' into the map.");
		operationsMap.put(op, path);

		return operationsMap;
	}
}