package it.grid.storm.authorization.methods;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.StormAuthorizationUtils;
import it.grid.storm.authorization.UserCredentials;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	protected HttpHelper httpHelper;

	public AbstractMethodAuthorization(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

	protected boolean askAuth(String operation, String path) {
		log.debug("Asking authorization for operation " + operation + " on " + path);
		UserCredentials user = new UserCredentials(httpHelper);
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
		log.debug("Response: " + response);
		return response;
	}

	public abstract boolean isUserAuthorized() throws ServletException;

	protected HttpHelper getHttpHelper() {
		return httpHelper;
	}
	
//	protected String getResourcePath() throws ServletException {
//		return convertToStorageAreaPath(httpHelper.getRequestURI());
//	}
//
//	protected URI fromStringToURI(String uriStr) throws ServletException {
//		URI uri;
//		try {
//			uri = new URI(uriStr);
//		} catch (URISyntaxException e) {
//			throw new ServletException("Unable to create URI object from the string: " + uriStr);
//		}
//		return uri;
//	}
//
//	protected String convertToStorageAreaPath(String uriStr) throws ServletException {
//		URI uri = fromStringToURI(uriStr);
//		String stfnRoot = "/" + uri.getPath().replaceFirst("/", "").split("/")[0];
//		log.debug("searching for stfnRoot: " + stfnRoot);
//		String fsRoot = StorageAreaManager.getInstance().getFsRootFromStfn().get(stfnRoot);
//		String path = uri.getPath().replaceFirst(stfnRoot, "").replace("//", "/");
//		return fsRoot + path;
//	}

}
