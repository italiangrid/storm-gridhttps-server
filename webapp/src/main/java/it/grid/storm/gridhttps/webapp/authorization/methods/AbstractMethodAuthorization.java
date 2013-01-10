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
package it.grid.storm.gridhttps.webapp.authorization.methods;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(AbstractMethodAuthorization.class);

	private HttpHelper httpHelper;
	private UserCredentials user;

	public AbstractMethodAuthorization(HttpHelper httpHelper) {
		this.setHttpHelper(httpHelper);
		this.setUser(new UserCredentials(getHttpHelper()));
	}

	protected boolean askAuth(String operation, String path) {
		log.debug("Asking authorization for operation " + operation + " on " + path);
		UserCredentials user = new UserCredentials(httpHelper);
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, operation, path);
		} catch (Exception e) {
			log.error(e.getMessage());
			return false;
		}
		log.debug("Response: " + response);
		return response;
	}

	public abstract AuthorizationStatus isUserAuthorized();

	public HttpHelper getHttpHelper() {
		return httpHelper;
	}

	public UserCredentials getUser() {
		return user;
	}

	private void setUser(UserCredentials user) {
		this.user = user;
	}
	
	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

}
