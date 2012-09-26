package it.grid.storm.webdav.authorization.methods;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public abstract class AbstractMethodAuthorization {

	private String stormStorageAreaRootDir;
	private String servletContextPath;

	public void init(String stormStorageAreaRootDir,
			String servletContextPath) {
		this.stormStorageAreaRootDir = stormStorageAreaRootDir;
		this.servletContextPath = servletContextPath;
	}
	
	public abstract Map<String, String> getOperationsMap(
			HttpServletRequest HTTPRequest) throws IOException,
			ServletException;

	protected String getDestinationFromHeader(HttpServletRequest HTTPRequest)
			throws ServletException {
		String destinationHeader = HTTPRequest.getHeader("Destination");
		if (destinationHeader != null)
			return convertToStorageAreaPath(destinationHeader);
		return null;
	}

	protected String getResourcePath(HttpServletRequest HTTPRequest)
			throws ServletException {
		return convertToStorageAreaPath(HTTPRequest.getRequestURI());
	}

	protected boolean getOverwriteFromHeader(HttpServletRequest HTTPRequest) {
		String overwriteHeader = HTTPRequest.getHeader("Overwrite");
		if ((overwriteHeader != null) && (overwriteHeader.contentEquals("F")))
			return false;
		return true;
	}

	private String convertToStorageAreaPath(String uri_string)
			throws ServletException {
		URI uri;
		try {
			uri = new URI(uri_string);
		} catch (URISyntaxException e) {
			throw new ServletException(
					"Unable to create URI object from the string: "
							+ uri_string);
		}
		String path = uri.getPath().replaceFirst(servletContextPath, "")
				.replace("//", "/");
		return stormStorageAreaRootDir + path;
	}

}
