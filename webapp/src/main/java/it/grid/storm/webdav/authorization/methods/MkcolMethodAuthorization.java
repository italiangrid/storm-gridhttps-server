package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class MkcolMethodAuthorization extends AbstractMethodAuthorization {
	
	public MkcolMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String operation = Constants.MKDIR_OPERATION;
		String path = getResourcePath();
		return askAuth(operation, path);
	}
}