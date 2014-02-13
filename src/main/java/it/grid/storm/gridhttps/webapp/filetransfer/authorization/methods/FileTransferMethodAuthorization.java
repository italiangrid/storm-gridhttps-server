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
package it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods;

import java.io.File;
import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.common.exceptions.InvalidRequestException;

public abstract class FileTransferMethodAuthorization extends AbstractMethodAuthorization {
	
	private static final Logger log = LoggerFactory.getLogger(FileTransferMethodAuthorization.class);
	
	public FileTransferMethodAuthorization() {		
		super(Configuration.getGridhttpsInfo().getFiletransferContextPath());
	}
	
	protected AuthorizationStatus askBEAuth(UserCredentials user,
		String operation, String path) {

		boolean response = false;
		try {
			response = StormAuthorizationUtils
				.isUserAuthorized(user, operation, path);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				String.format("Internal Server Error: %s", e.getMessage()));
		}
		if (response) {
			return AuthorizationStatus.AUTHORIZED();
		}
		return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_FORBIDDEN,
			"You are not authorized to access the requested resource");
	}
	
	protected StorageArea getMatchingStorageArea(String uriPath) 
		throws InvalidRequestException {
		
		uriPath = stripContext(uriPath);
		log.debug("context stripped: {}" , uriPath);
		return StorageAreaManager.getMatchingSA(uriPath);
	}

	protected StorageArea checkTURL(String protocol, String uriPath) {
		
		String ftContextPath = File.separator + 
			Configuration.getGridhttpsInfo().getFiletransferContextPath();
		
		if (!uriPath.startsWith(ftContextPath)) {
			log.debug("TURL '{}' doesn't start with {}!", uriPath, ftContextPath);
			throw new InvalidRequestException(HttpServletResponse.SC_CONFLICT, 
				"Invalid TURL!");
		}
		
		if (uriPath.contains("..")) {
			log.debug("TURL '{}' contains a dotted segment!", uriPath);
			throw new InvalidRequestException(HttpServletResponse.SC_CONFLICT, 
				"Invalid TURL!");
		}
		
		StorageArea sa = getMatchingStorageArea(uriPath);
		if (sa == null) {
			log.debug("Unable to resolve a Storage Area from {}", uriPath);
			throw new InvalidRequestException(HttpServletResponse.SC_CONFLICT, 
				"Invalid TURL!");
		}
		
		log.debug("path {} matches storage area {}", uriPath, sa.getName());
		if (!sa.hasProtocol(protocol)) {
			log.debug("{} doesn't support {} as transfer protocol", sa.getName(),
				protocol);
			throw new InvalidRequestException(HttpServletResponse.SC_CONFLICT, 
				"Invalid TURL!");
		}
		
		File file = new File(sa.getRealPath(stripContext(uriPath)));
		log.debug("file path is {}", file);
		
		String canonicalPath;
		try {
			canonicalPath = file.getCanonicalPath();
		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new InvalidRequestException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
				e.getMessage());
		}
		log.debug("canonical path is {}", canonicalPath);
		
		if (!canonicalPath.startsWith(sa.getFSRoot())) {
			log.debug("file {} is not owned by {}", canonicalPath, sa.getName());
			throw new InvalidRequestException(HttpServletResponse.SC_CONFLICT, 
				"Invalid TURL!");
		}
		
		return sa;
	}
	
	protected AuthorizationStatus checkUserReadPermissionsOnStorageArea(
		UserCredentials user, StorageArea sa) {
		
		if (user.isAnonymous()) {
			if (sa.isHTTPReadable()) {
				return AuthorizationStatus.AUTHORIZED();
			}
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN, String.format(
					"Unauthorized: Anonymous users are not authorized to read into {}",
					sa.getName()));
		}
		/* user is not anonymous */
		if (sa.isHTTPReadable()) {
			return AuthorizationStatus.AUTHORIZED();
		}
		return null; 
	}
	
	protected AuthorizationStatus checkUserWritePermissionsOnStorageArea(
		UserCredentials user, StorageArea sa) {
		
		if (user.isAnonymous()) {
			if (sa.isHTTPWritable()) {
				return AuthorizationStatus.AUTHORIZED();
			}
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN, String.format(
					"Unauthorized: Anonymous users are not authorized to read into {}",
					sa.getName()));
		}
		/* user is not anonymous */
		if (sa.isHTTPWritable()) {
			return AuthorizationStatus.AUTHORIZED();
		}
		return null;
	}
	
}
