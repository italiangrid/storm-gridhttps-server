package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.webdav.webapp.authorization.StormAuthorizationFilter;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public abstract class AbstractMethodAuthorization {

	HttpServletRequest HTTPRequest;
	
	public AbstractMethodAuthorization(HttpServletRequest HTTPRequest) {
		this.HTTPRequest = HTTPRequest;
	}
	
	public abstract Map<String, String> getOperationsMap() throws IOException, ServletException;

	protected String getDestinationFromHeader() throws ServletException {
		String destinationHeader = this.HTTPRequest.getHeader("Destination");
		String contextPath = StormAuthorizationUtils.storageAreaName;
		String rootDir = StormAuthorizationUtils.storageAreaRootDir;
		if (destinationHeader != null)
			return convertToStorageAreaPath(destinationHeader, contextPath, rootDir);
		return null;
	}

	protected String getResourcePath() throws ServletException {
		String contextPath = StormAuthorizationUtils.storageAreaName;
		String rootDir = StormAuthorizationUtils.storageAreaRootDir;
		return convertToStorageAreaPath(this.HTTPRequest.getRequestURI(), contextPath, rootDir);
	}

	private String convertToStorageAreaPath(String uriStr, String contextPath, String rootDir) throws ServletException {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			throw new ServletException("Unable to create URI object from the string: " + uriStr);
		}
		String path = uri.getPath().replaceFirst(contextPath, "").replace("//", "/");
		return rootDir + path;
	}

}
