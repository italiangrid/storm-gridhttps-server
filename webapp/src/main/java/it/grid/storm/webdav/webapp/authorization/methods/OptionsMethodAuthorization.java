package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.factory.StormResourceHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	public OptionsMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		StormResourceHelper.doPing();
		return true;
	}
}
