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
	
	protected StorageArea getMatchingSA(String path) throws InvalidRequestException {
		StorageArea sa = StorageAreaManager.getMatchingSA(path);
		if (sa == null) {
			throw new InvalidRequestException(HttpServletResponse.SC_BAD_REQUEST, "Unable to resolve storage area for " + path);
		}
		return sa;
	}
	
	protected AuthorizationStatus checkSA(StorageArea sa, String requestedProtocol) {

		if (!sa.isProtocol(requestedProtocol.toUpperCase())) {
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN,
				String.format("Storage area %s doesn't support %s protocol",
					sa.getName(), requestedProtocol));
		}
		return AuthorizationStatus.AUTHORIZED();
	}

}
