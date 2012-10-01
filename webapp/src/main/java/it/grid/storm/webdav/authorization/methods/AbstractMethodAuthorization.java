package it.grid.storm.webdav.authorization.methods;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public abstract class AbstractMethodAuthorization {

	public abstract Map<String, String> getOperationsMap(HttpServletRequest HTTPRequest) throws IOException, ServletException;

	protected String getDestinationFromHeader(HttpServletRequest HTTPRequest) throws ServletException {
		String destinationHeader = HTTPRequest.getHeader("Destination");
		String contextPath = (String) HTTPRequest.getAttribute("STORAGE_AREA_NAME");
		String rootDir = (String) HTTPRequest.getAttribute("STORAGE_AREA_ROOT");
		if (destinationHeader != null)
			return convertToStorageAreaPath(destinationHeader, contextPath, rootDir);
		return null;
	}

	protected String getResourcePath(HttpServletRequest HTTPRequest) throws ServletException {
		String contextPath = (String) HTTPRequest.getAttribute("STORAGE_AREA_NAME");
		String rootDir = (String) HTTPRequest.getAttribute("STORAGE_AREA_ROOT");
		return convertToStorageAreaPath(HTTPRequest.getRequestURI(), contextPath, rootDir);
	}

	protected boolean getOverwriteFromHeader(HttpServletRequest HTTPRequest) {
		String overwriteHeader = HTTPRequest.getHeader("Overwrite");
		if ((overwriteHeader != null) && (overwriteHeader.contentEquals("F")))
			return false;
		return true;
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
