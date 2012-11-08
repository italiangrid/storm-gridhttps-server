package it.grid.storm.ftwebapp.authorization.methods;

import it.grid.storm.ftwebapp.authorization.Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	public PutMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String operation = isOverwriteRequest() ? Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION : Constants.PREPARE_TO_PUT_OPERATION;
		String path = getResourcePath();
		return askAuth(operation, path);
	}

	public String getOverwriteHeader() {
		return this.HTTPRequest.getHeader("Overwrite");
	}
	
	public boolean isOverwriteRequest() {
		String overwrite = getOverwriteHeader();
		return ((overwrite == null) || (overwrite.equals("T")));		
	}
	
}