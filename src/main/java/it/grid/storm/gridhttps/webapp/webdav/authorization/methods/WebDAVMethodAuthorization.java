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
package it.grid.storm.gridhttps.webapp.webdav.authorization.methods;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.authorization.methods.AbstractMethodAuthorization;

public abstract class WebDAVMethodAuthorization extends AbstractMethodAuthorization {
	
	public static enum Operation { READ, READWRITE };
	
	public WebDAVMethodAuthorization() {		
		super(Configuration.getGridhttpsInfo().getWebdavContextPath());
	}
	
	private AuthorizationStatus isAnonymousAuthorized(StorageArea sa, Operation op, UserCredentials user) {
		if (sa.isHTTPReadable())
			if (Operation.READWRITE.equals(op))
				if (sa.isHTTPWritable())
					return AuthorizationStatus.AUTHORIZED();
				else
					return AuthorizationStatus.NOTAUTHORIZED(405, "Anonymous users are not authorized to access " + sa.getName() + " in " + op + " mode ");
			else
				return AuthorizationStatus.AUTHORIZED();
		else
			return AuthorizationStatus.NOTAUTHORIZED(405, "Anonymous users are not authorized to access " + sa.getName() + " in " + op + " mode ");
	}
	
	private AuthorizationStatus isSecureAuthorized(StorageArea sa, Operation op, UserCredentials user) {
		if (user.isAnonymous())
			return AuthorizationStatus.NOTAUTHORIZED(405, "Anonymous users are not authorized to access " + sa.getName() + " in " + op + " mode ");
		return AuthorizationStatus.AUTHORIZED();
	}
	
	protected AuthorizationStatus isAuthorized(String protocol, StorageArea sa, Operation op, UserCredentials user) {
		if (protocol.toUpperCase().equals("HTTP"))
			return isAnonymousAuthorized(sa, op, user);
		return isSecureAuthorized(sa, op, user);
	}
	
}
