package it.grid.storm.ftwebapp.authorization.methods;

import it.grid.storm.ftwebapp.authorization.Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class GetMethodAuthorization extends AbstractMethodAuthorization {

	public GetMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String operation = Constants.PREPARE_TO_GET_OPERATION;
		String path = getResourcePath();
		return askAuth(operation, path);
	}

}