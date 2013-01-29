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
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.PutMethodAuthorization;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.storagearea.StorageArea;

import java.util.Arrays;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferAuthorizationFilter extends AuthorizationFilter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferAuthorizationFilter.class);
	
	private String[] allowedMethods = {"GET", "PUT"};	
	private HashMap<String, AbstractMethodAuthorization> METHODS_MAP;
	private StorageArea storageArea;
	private String contextPath;
	
	public FileTransferAuthorizationFilter(HttpHelper httpHelper, String contextPath) throws Exception {
		super(httpHelper);
		this.setContextPath(contextPath);
		initStorageArea();
		doInitMethodMap();
	}

	private boolean isMethodAllowed(String requestMethod) {
		return Arrays.asList(allowedMethods).contains(requestMethod);
	}
	
	private boolean isProtocolAllowed(String protocol) {
		return Arrays.asList(getStorageArea().getProtocolAsStrArray()).contains(protocol);
	}

	private String stripContext() {
		return this.getHTTPHelper().getRequestURI().getPath().replaceFirst(getContextPath(), "");
	}

	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		String method = getHTTPHelper().getRequestMethod();
		String reqProtocol = getHTTPHelper().getRequestProtocol();
		/* check method */
		if (!isMethodAllowed(method)) {
			log.warn("Received a request for a not allowed method : " + method);
			return AuthorizationStatus.NOTAUTHORIZED(405, "Method " + method + " not allowed!");
		}
		/* check protocol */
		if (!isProtocolAllowed(reqProtocol)) {
			log.warn("Received a request-uri with a not allowed protocol: " + reqProtocol);
			return AuthorizationStatus.NOTAUTHORIZED(401, "Unauthorized request protocol: " + reqProtocol);
		}
		return getAuthorizationHandler().isUserAuthorized(user);
	}
	
	private void initStorageArea() throws Exception {
		storageArea = null;
		storageArea = StorageAreaManager.getMatchingSA(stripContext());
		if (storageArea == null) {
			log.error("No matching StorageArea found for path " + this.stripContext() + " Unable to build http(s) relative path");
			throw new Exception("No matching StorageArea found for the provided path");
		}
	}
	
	private StorageArea getStorageArea() {
		return storageArea;
	}
	
	private String getContextPath() {
		return contextPath;
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}
		
	private void doInitMethodMap() {
		METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
		METHODS_MAP.clear();
		METHODS_MAP.put("GET", new GetMethodAuthorization(getHTTPHelper(), getStorageArea()));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(getHTTPHelper(), getStorageArea()));
	}
		
	private AbstractMethodAuthorization getAuthorizationHandler() {
		return METHODS_MAP.get(this.getHTTPHelper().getRequestMethod());
	}
	
}
