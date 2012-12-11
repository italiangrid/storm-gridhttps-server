package it.grid.storm.filetransfer.authorization;

import it.grid.storm.authorization.AuthorizationFilter;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.filetransfer.authorization.methods.PutMethodAuthorization;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.storagearea.StorageArea;


import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferAuthorizationFilter extends AuthorizationFilter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferAuthorizationFilter.class);
	
	private String[] allowedMethods = {"GET", "PUT"};	
	private HashMap<String, AbstractMethodAuthorization> METHODS_MAP;
	private StorageArea storageArea;
	private HttpServletRequest HTTPRequest;
	private HttpServletResponse HTTPResponse;
	private String contextPath;
	
	public FileTransferAuthorizationFilter(HttpServletRequest HTTPRequest, HttpServletResponse HTTPResponse, String contextPath) throws ServletException {
		super(HTTPRequest.getRequestURI());
		this.setContextPath(contextPath);
		this.setHTTPRequest(HTTPRequest);
		this.setHTTPResponse(HTTPResponse);
		initStorageArea();
		doInitMethodMap();
	}

	public boolean isMethodAllowed(String requestMethod) {
		return Arrays.asList(allowedMethods).contains(requestMethod);
	}
	
	public boolean isProtocolAllowed(String protocol) {
		return Arrays.asList(storageArea.getProtocolAsStrArray()).contains(protocol);
	}

	@Override
	public String stripContext() {
		return getRequestedURI().replaceFirst(getContextPath(), getRequestedURI());
	}

	public boolean isUserAuthorized() throws ServletException {
		if (!this.isMethodAllowed(HTTPRequest.getMethod())) {
			log.warn("Received a request for a not allowed method : " + HTTPRequest.getMethod());
			throw new ServletException("Method " + HTTPRequest.getMethod() + " not allowed!");
		}
		if (!isProtocolAllowed(HTTPRequest.getScheme().toUpperCase())) {
			log.warn("Received a request with a not allowed protocol: " + HTTPRequest.getScheme().toUpperCase());
			throw new ServletException("Protocol " + HTTPRequest.getScheme().toUpperCase() + " not allowed!");
		}
		return getAuthorizationHandler().isUserAuthorized();
	}
	
	private void initStorageArea() throws ServletException {
		storageArea = null;
		try {
			storageArea = StorageAreaManager.getMatchingSAbyURI(this.stripContext());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e);
		}
		if (storageArea == null) {
			log.error("No matching StorageArea found for path " + this.stripContext() + " Unable to build http(s) relative path");
			throw new ServletException("No matching StorageArea found for the provided path");
		}
	}
	
	public StorageArea getStorageArea() {
		return storageArea;
	}
	
	public HttpServletRequest getHTTPRequest() {
		return HTTPRequest;
	}

	private void setHTTPRequest(HttpServletRequest HTTPRequest) {
		this.HTTPRequest = HTTPRequest;
	}

	public HttpServletResponse getHTTPResponse() {
		return HTTPResponse;
	}

	private void setHTTPResponse(HttpServletResponse HTTPResponse) {
		this.HTTPResponse = HTTPResponse;
	}

	public String getContextPath() {
		return contextPath;
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
		
	private void doInitMethodMap() {
		METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
		METHODS_MAP.clear();
		METHODS_MAP.put("GET", new GetMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(HTTPRequest));
	}
		
	public AbstractMethodAuthorization getAuthorizationHandler() {
		return METHODS_MAP.get(HTTPRequest.getMethod().toUpperCase());
	}
	
}
