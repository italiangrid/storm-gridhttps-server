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

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;

public class GetMethodAuthorization extends AbstractMethodAuthorization {
	
	private StorageArea SA;
	
	public GetMethodAuthorization(HttpHelper httpHelper, StorageArea SA) {
		super(httpHelper);
		this.SA = SA;
	}
	
	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		String path = SA.getRealPath(getHTTPHelper().getRequestURI().getRawPath());
		String operation = Constants.PREPARE_TO_GET_OPERATION;
		return askAuth(user, operation, path);
	}

}
