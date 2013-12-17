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

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.authorization.methods.AbstractMethodAuthorization;

public abstract class FileTransferMethodAuthorization extends AbstractMethodAuthorization {
		
	public FileTransferMethodAuthorization() {		
		super(Configuration.getGridhttpsInfo().getFiletransferContextPath());
	}

	protected AuthorizationStatus askBEAuth(UserCredentials user, String operation, String path) {	
		try {
			boolean response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
			if (!response && !user.isAnonymous()) {
				user.forceAnonymous();
				response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
			}
			if (response) {
				return AuthorizationStatus.AUTHORIZED();
			} else {
				return AuthorizationStatus.NOTAUTHORIZED(401, "You are not authorized to access the requested resource");
			}			
		} catch (Throwable  t) {
			return AuthorizationStatus.NOTAUTHORIZED(500, "Error: " + t.getMessage());
		}
	}

}
