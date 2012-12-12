package it.grid.storm.webdav.authorization;

import it.grid.storm.authorization.AuthorizationFilter;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.HttpHelper;
import it.grid.storm.webdav.authorization.methods.*;

import java.util.ArrayList;
import java.util.HashMap;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDAVAuthorizationFilter extends AuthorizationFilter {
		
	private static final Logger log = LoggerFactory.getLogger(WebDAVAuthorizationFilter.class);

	private ArrayList<String> allowedMethods = new ArrayList<String>() {
		private static final long serialVersionUID = -4755430833502795659L;
	{
		add("PROPFIND");
		add("OPTIONS");
		add("GET");
		add("PUT");
		add("DELETE");
		add("MOVE");
		add("MKCOL");
		add("COPY");
	}};	
	
	private ArrayList<String> destinationMethods = new ArrayList<String>() {
		private static final long serialVersionUID = -2207218709330278065L;
	{
		add("MOVE");
		add("COPY");
	}};	
	
	private HttpHelper httpHelper;
	private HashMap<String, AbstractMethodAuthorization> METHODS_MAP;
	private StorageArea reqStorageArea;
	private StorageArea destStorageArea;

	
	public WebDAVAuthorizationFilter(HttpHelper httpHelper) throws ServletException {
		super();
		this.httpHelper = httpHelper;
		doInitMethodMap();
		initStorageAreas();
	}

	private boolean isMethodAllowed(String method) {
		return allowedMethods.contains(method);
	}
	
	private boolean isRequestProtocolAllowed(String protocol) {
		return getRequestStorageArea().getProtocolAsList().contains(protocol);
	}
	
	private StorageArea getRequestStorageArea() {
		return reqStorageArea;
	}

	private boolean isDestinationProtocolAllowed(String protocol) {
		return getDestinationStorageArea().getProtocolAsList().contains(protocol);
	}
	
	private StorageArea getDestinationStorageArea() {
		return destStorageArea;
	}

	private boolean hasDestination(String method) {
		return destinationMethods.contains(method);
	}

	public boolean isUserAuthorized() throws ServletException {
		String method = httpHelper.getRequestMethod();
		String reqProtocol = httpHelper.getRequestProtocol();
		
		if (!isMethodAllowed(method)) {
			log.warn("Received a request for a not allowed method : " + method);
			throw new ServletException("Method " + method + " not allowed!");
		}
		if (!isRequestProtocolAllowed(reqProtocol)) {
			log.warn("Received a request-uri with a not allowed protocol: " + reqProtocol);
			throw new ServletException("Protocol " + reqProtocol + " not allowed!");
		}
		if (hasDestination(method)) {
			String destProtocol = httpHelper.getDestinationProtocol();
			if (!isDestinationProtocolAllowed(destProtocol)) {
				log.warn("Received a destination-uri with a not allowed protocol: " + destProtocol);
				throw new ServletException("Protocol " + destProtocol + " not allowed!");
			}
		}
		return getAuthorizationHandler().isUserAuthorized();
	}
	
	private void initStorageAreas() throws ServletException {
		reqStorageArea = null;
		log.debug("searching storagearea by uri: " + httpHelper.getRequestStringURI());
		try {
			reqStorageArea = StorageAreaManager.getMatchingSAbyURI(httpHelper.getRequestStringURI());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e);
		}
		if (reqStorageArea == null) {
			log.error("No matching StorageArea found for uri " + httpHelper.getRequestStringURI() + " Unable to build http(s) relative path");
			throw new ServletException("No matching StorageArea found for the provided path");
		}
		destStorageArea = null;
		if (hasDestination(httpHelper.getRequestMethod())) {
			log.debug("searching storagearea by uri: " + httpHelper.getDestinationURI().getPath());
			try {
				destStorageArea = StorageAreaManager.getMatchingSAbyURI(httpHelper.getDestinationURI().getPath());
			} catch (Exception e) {
				log.error(e.getMessage());
				throw new ServletException(e);
			}
			if (destStorageArea == null) {
				log.error("No matching StorageArea found for uri " + httpHelper.getDestinationURI().getPath() + " Unable to build http(s) relative path");
				throw new ServletException("No matching StorageArea found for the provided path");
			}
		}
	}
	
	private void doInitMethodMap() {
		METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
		METHODS_MAP.clear();
		METHODS_MAP.put("PROPFIND", new PropfindMethodAuthorization(httpHelper));
		METHODS_MAP.put("OPTIONS", new OptionsMethodAuthorization(httpHelper));
		METHODS_MAP.put("GET", new GetMethodAuthorization(httpHelper));
		METHODS_MAP.put("DELETE", new DeleteMethodAuthorization(httpHelper));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(httpHelper));
		METHODS_MAP.put("MKCOL", new MkcolMethodAuthorization(httpHelper));
		METHODS_MAP.put("MOVE", new MoveMethodAuthorization(httpHelper));
		METHODS_MAP.put("COPY", new CopyMethodAuthorization(httpHelper));
	}
		
	public AbstractMethodAuthorization getAuthorizationHandler() {
		return METHODS_MAP.get(httpHelper.getRequestMethod());
	}
	
}