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

	public PutMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
		this.HTTPRequest = HTTPRequest;
	}

	private static final Logger log = LoggerFactory.getLogger(PutMethodAuthorization.class);
	
	protected boolean getOverwriteFromHeader() {
		String overwriteHeader = HTTPRequest.getHeader("Overwrite");
		if ((overwriteHeader != null) && (overwriteHeader.contentEquals("F")))
			return false;
		return true;
	}

	@Override
	public Map<String, String> getOperationsMap() throws IOException, ServletException {
		Map<String, String> operationsMap = new HashMap<String, String>();
		String path = getResourcePath();
		if (getOverwriteFromHeader()) {
			operationsMap.put(Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION, path);
			log.debug("operation '" + Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION + "' on path '" + path + "'");
		} else {
			operationsMap.put(Constants.PREPARE_TO_PUT_OPERATION, path);
			log.debug("operation '" + Constants.PREPARE_TO_PUT_OPERATION + "' on path '" + path + "'");
		}
		return operationsMap;
	}
	
}