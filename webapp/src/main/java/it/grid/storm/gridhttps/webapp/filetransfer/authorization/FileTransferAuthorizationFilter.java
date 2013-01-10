/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.filetransfer.authorization;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.PutMethodAuthorization;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.storagearea.StorageArea;

import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferAuthorizationFilter extends AuthorizationFilter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferAuthorizationFilter.class);
	
	private String[] allowedMethods = {"GET", "PUT"};	
	private HashMap<String, AbstractMethodAuthorization> METHODS_MAP;
	private StorageArea storageArea;
	private HttpHelper httpHelper;
	private String contextPath;
	
	public FileTransferAuthorizationFilter(HttpHelper httpHelper, String contextPath) throws ServletException {
		super();
		this.setContextPath(contextPath);
		this.setHttpHelper(httpHelper);
		initStorageArea();
		doInitMethodMap();
	}

	public boolean isMethodAllowed(String requestMethod) {
		return Arrays.asList(allowedMethods).contains(requestMethod);
	}
	
	public boolean isProtocolAllowed(String protocol) {
		return Arrays.asList(storageArea.getProtocolAsStrArray()).contains(protocol);
	}

	public String stripContext() {
		return httpHelper.getRequestStringURI().replaceFirst(getContextPath(), "");
	}

	public AuthorizationStatus isUserAuthorized() {
		String method = httpHelper.getRequestMethod();
		if (!isMethodAllowed(method)) {
			log.warn("Received a request for a not allowed method : " + method);
			return new AuthorizationStatus(false, "Method " + method + " not allowed!");
		}
		String reqProtocol = httpHelper.getRequestProtocol();
		if (!isProtocolAllowed(reqProtocol)) {
			log.warn("Received a request-uri with a not allowed protocol: " + reqProtocol);
			return new AuthorizationStatus(false, "Protocol " + reqProtocol + " not allowed!");
		}
		return getAuthorizationHandler().isUserAuthorized();
	}
	
	private void initStorageArea() throws ServletException {
		storageArea = null;
		try {
			storageArea = StorageAreaManager.getMatchingSA(this.stripContext());
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
	
	public HttpHelper getHttpHelper() {
		return httpHelper;
	}

	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

//	public HttpServletResponse getHTTPResponse() {
//		return getHttpHelper().getResponse();
//	}
//	
//	public HttpServletRequest getHTTPRequest() {
//		return getHttpHelper().getRequest();
//	}

	public String getContextPath() {
		return contextPath;
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
		
	private void doInitMethodMap() {
		METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
		METHODS_MAP.clear();
		METHODS_MAP.put("GET", new GetMethodAuthorization(httpHelper));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(httpHelper));
	}
		
	public AbstractMethodAuthorization getAuthorizationHandler() {
		return METHODS_MAP.get(httpHelper.getRequestMethod());
	}
	
}
