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
package it.grid.storm.gridhttps.webapp.webdav.authorization;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.CopyMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.DeleteMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.HeadMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.MkcolMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.MoveMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.OptionsMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.PropfindMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.PutMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import java.util.ArrayList;
import java.util.HashMap;

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
		add("HEAD");
	}};	
	
	private ArrayList<String> destinationMethods = new ArrayList<String>() {
		private static final long serialVersionUID = -2207218709330278065L;
	{
		add("MOVE");
		add("COPY");
	}};	
	
	private HashMap<String, AbstractMethodAuthorization> METHODS_MAP;
	private StorageArea reqStorageArea;
	private StorageArea destStorageArea;

	
	public WebDAVAuthorizationFilter(HttpHelper httpHelper) throws Exception {
		super(httpHelper);
		initStorageAreas();
		doInitMethodMap();
		
	}

	private boolean isMethodAllowed(String method) {
		return allowedMethods.contains(method);
	}
	
	private boolean isRequestProtocolAllowed(String protocol) {
		return getRequestStorageArea().getProtocols().contains(protocol);
	}
	
	private StorageArea getRequestStorageArea() {
		return reqStorageArea;
	}

	private boolean isDestinationProtocolAllowed(String protocol) {
		return getDestinationStorageArea().getProtocols().contains(protocol);
	}
	
	private StorageArea getDestinationStorageArea() {
		return destStorageArea;
	}

	private boolean hasDestination(String method) {
		return destinationMethods.contains(method);
	}

	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		String method = getHTTPHelper().getRequestMethod();
		if (!isMethodAllowed(method)) {
			log.warn("Received a request for a not allowed method : " + method);
			return AuthorizationStatus.NOTAUTHORIZED(405, "Method " + method + " not allowed!");
		}
		log.info(method + " " + getHTTPHelper().getRequestURI().getPath());
		String reqProtocol = getHTTPHelper().getRequestProtocol();
		if (!isRequestProtocolAllowed(reqProtocol)) {
			log.warn("Received a request-uri with a not allowed protocol: " + reqProtocol);
			return AuthorizationStatus.NOTAUTHORIZED(401, "Unauthorized request protocol: " + reqProtocol);
		}
		if (hasDestination(method)) {
			if (getHTTPHelper().hasDestinationHeader()) {
				String destinationProtocol = getHTTPHelper().getDestinationProtocol();
				if (isDestinationProtocolAllowed(destinationProtocol)) {
					if (getHTTPHelper().getRequestURI().getPath().equals(getHTTPHelper().getDestinationURI().getPath())) {
						return AuthorizationStatus.NOTAUTHORIZED(403, "The source and destination URIs are the same!");
					} 
				} else {
					log.warn("Received a destination-uri with a not allowed protocol: " + destinationProtocol);
					return AuthorizationStatus.NOTAUTHORIZED(401, "Unauthorized destination protocol: " + destinationProtocol);
				}
			} else {
				return AuthorizationStatus.NOTAUTHORIZED(400, "Missed necessary destination header!");
			}
		}
		return getAuthorizationHandler().isUserAuthorized(user); 
	}

	private void initStorageAreas() throws Exception {
		reqStorageArea = null;
		log.debug("searching storagearea by uri: " + getHTTPHelper().getRequestURI().getPath());
		reqStorageArea = StorageAreaManager.getMatchingSA(getHTTPHelper().getRequestURI());
		if (reqStorageArea == null) {
			log.error("No matching StorageArea found for uri " + getHTTPHelper().getRequestURI().getPath() + " Unable to build http(s) relative path");
			throw new Exception("No matching StorageArea found for the provided path");
		}
		destStorageArea = null;
		if (hasDestination(getHTTPHelper().getRequestMethod())) {
			log.debug("searching storagearea by uri: " + getHTTPHelper().getDestinationURI().getPath());
			destStorageArea = StorageAreaManager.getMatchingSA(getHTTPHelper().getDestinationURI());
			if (destStorageArea == null) {
				log.error("No matching StorageArea found for uri " + getHTTPHelper().getDestinationURI().getPath() + " Unable to build http(s) relative path");
				throw new Exception("No matching StorageArea found for the provided path");
			}
		}
	}
	
	private void doInitMethodMap() {
		METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
		METHODS_MAP.clear();
		METHODS_MAP.put("PROPFIND", new PropfindMethodAuthorization(getHTTPHelper(), reqStorageArea));
		METHODS_MAP.put("OPTIONS", new OptionsMethodAuthorization(getHTTPHelper()));
		METHODS_MAP.put("GET", new GetMethodAuthorization(getHTTPHelper(), reqStorageArea));
		METHODS_MAP.put("DELETE", new DeleteMethodAuthorization(getHTTPHelper(), reqStorageArea));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(getHTTPHelper(), reqStorageArea));
		METHODS_MAP.put("MKCOL", new MkcolMethodAuthorization(getHTTPHelper(), reqStorageArea));
		METHODS_MAP.put("MOVE", new MoveMethodAuthorization(getHTTPHelper(), reqStorageArea, destStorageArea));
		METHODS_MAP.put("COPY", new CopyMethodAuthorization(getHTTPHelper(), reqStorageArea, destStorageArea));
		METHODS_MAP.put("HEAD", new HeadMethodAuthorization(getHTTPHelper()));
	}
		
	public AbstractMethodAuthorization getAuthorizationHandler() {
		return METHODS_MAP.get(HttpHelper.getHelper().getRequestMethod());
	}
		
}