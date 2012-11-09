package it.grid.storm.ftwebapp.authorization.methods;

import it.grid.storm.ftwebapp.Configuration;
import it.grid.storm.ftwebapp.authorization.StormAuthorizationUtils;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	protected HttpServletRequest HTTPRequest;
	protected String contextPath;
	protected String rootDir;

	public AbstractMethodAuthorization(HttpServletRequest HTTPRequest) {
		this.HTTPRequest = HTTPRequest;
		this.contextPath = "filetransfer/" + Configuration.storageAreaName;
		this.rootDir = Configuration.storageAreaRootDir;
	}

	protected boolean askAuth(String operation, String path) {
		log.debug("Asking authorization for operation " + operation + " on " + path);
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(operation, path);
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
		log.debug("Response: " + response);
		return response;
	}

	public abstract boolean isUserAuthorized() throws ServletException;

	protected String getResourcePath() throws ServletException {
		return convertToStorageAreaPath(this.HTTPRequest.getRequestURI());
	}

	protected URI fromStringToURI(String uriStr) throws ServletException {
		URI uri;
		try {
			uri = new URI(uriStr);
		} catch (URISyntaxException e) {
			throw new ServletException("Unable to create URI object from the string: " + uriStr);
		}
		return uri;
	}

	protected String convertToStorageAreaPath(String uriStr) throws ServletException {
		URI uri = fromStringToURI(uriStr);
		String path = uri.getPath().replaceFirst(contextPath, "").replace("//", "/");
		return rootDir + path;
	}

}
