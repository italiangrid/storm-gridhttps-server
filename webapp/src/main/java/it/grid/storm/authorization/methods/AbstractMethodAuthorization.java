package it.grid.storm.authorization.methods;

import it.grid.storm.authorization.StormAuthorizationUtils;
import it.grid.storm.storagearea.StorageAreaManager;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	protected HttpServletRequest HTTPRequest;

	public AbstractMethodAuthorization(HttpServletRequest HTTPRequest) {
		this.HTTPRequest = HTTPRequest;
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
		String stfnRoot = "/" + uri.getPath().replaceFirst("/", "").split("/")[0];
		log.debug("searching for stfnRoot: " + stfnRoot);
		String fsRoot = StorageAreaManager.getInstance().getStfnToFsRoot().get(stfnRoot);
		String path = uri.getPath().replaceFirst(stfnRoot, "").replace("//", "/");
		return fsRoot + path;
	}

}
