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
package it.grid.storm.gridhttps.webapp.common.authorization.methods;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;

public abstract class AbstractMethodAuthorization {

	public abstract AuthorizationStatus isUserAuthorized(HttpServletRequest request, HttpServletResponse response, UserCredentials user) throws AuthorizationException;

	protected AuthorizationStatus askAuth(UserCredentials user, String operation, String path) {	
		try {
			boolean response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
			if (!response && !user.isAnonymous()) {
				/* Re-try as anonymous user: */
				user.forceAnonymous();
				response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
			}
			if (response) {
				return AuthorizationStatus.AUTHORIZED();
			} else {
				return AuthorizationStatus.NOTAUTHORIZED(401, "You are not authorized to access the requested resource");
			}			
		} catch (IllegalArgumentException e) {
			return AuthorizationStatus.NOTAUTHORIZED(500, "Error: " + e.getMessage());
		} catch (Exception e) {
			return AuthorizationStatus.NOTAUTHORIZED(500, "Error: " + e.getMessage());
		}
	}
	
}
