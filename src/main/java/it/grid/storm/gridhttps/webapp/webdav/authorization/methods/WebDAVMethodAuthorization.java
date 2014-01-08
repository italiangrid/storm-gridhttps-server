/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.webdav.authorization.methods;

import javax.servlet.http.HttpServletResponse;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.common.exceptions.InvalidRequestException;

public abstract class WebDAVMethodAuthorization extends
	AbstractMethodAuthorization {

	public static enum Permission {
		READ, READWRITE
	};

	public WebDAVMethodAuthorization() {

		super(Configuration.getGridhttpsInfo().getWebdavContextPath());
	}

	protected StorageArea getMatchingSA(String path)
		throws InvalidRequestException {

		StorageArea sa = StorageAreaManager.getMatchingSA(path);
		if (sa == null) {
			throw new InvalidRequestException(HttpServletResponse.SC_BAD_REQUEST,
				"Unable to resolve storage area for path " + path);
		}
		return sa;
	}

	private AuthorizationStatus checkSA(StorageArea sa, String requestedProtocol) {

		if (!sa.isProtocol(requestedProtocol.toUpperCase())) {
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN, "Storage area " + sa.getName()
					+ " doesn't support " + requestedProtocol + " protocol");
		}
		return AuthorizationStatus.AUTHORIZED();
	}

	private AuthorizationStatus isAnonymousAuthorized(StorageArea sa,
		Permission op, UserCredentials user) {

		/*
		 * Knowing that: - Permissions requested can be READ or READWRITE - A
		 * storage area can be HTTPReadable or HTTPReadable + HTTPWritable The user
		 * is not AUTHORIZED to access if: - the storage area is not HTTPReadable -
		 * the storage area is not HTTPWritable and the permission asked is
		 * READWRITE
		 */
		if ((!sa.isHTTPReadable())
			|| (!sa.isHTTPWritable() && Permission.READWRITE.equals(op))) {
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN,
				"Anonymous users are not authorized to access " + sa.getName() + " in "
					+ op + " mode ");
		}
		return AuthorizationStatus.AUTHORIZED();
	}

	private AuthorizationStatus isAuthenticatedUserAuthorized(StorageArea sa,
		Permission op, UserCredentials user) {

		if (user.isAnonymous())
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN, String.format(
					"Anonymous users are not authorized to access %s in %s mode",
					sa.getName(), op));
		return AuthorizationStatus.AUTHORIZED();
	}

	protected AuthorizationStatus isAuthorized(String protocol, StorageArea sa,
		Permission op, UserCredentials user) {

		AuthorizationStatus status = checkSA(sa, protocol);
		if (!status.isAuthorized()) {
			return status;
		}
		if (protocol.toUpperCase().equals("HTTP"))
			return isAnonymousAuthorized(sa, op, user);
		return isAuthenticatedUserAuthorized(sa, op, user);
	}

}
