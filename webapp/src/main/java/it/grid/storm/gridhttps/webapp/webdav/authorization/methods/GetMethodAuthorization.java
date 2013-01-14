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

import java.net.URI;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;

public class GetMethodAuthorization extends AbstractMethodAuthorization {

	private StorageArea SA;
	
	public GetMethodAuthorization(StorageArea SA) {
		this.SA = SA;
	}
	
	public AuthorizationStatus isUserAuthorized() {
		URI requestedURI = HttpHelper.getHelper().getRequestURI();
		String requiredPath = SA.getRealPath(requestedURI.getPath());
		String operation = Constants.PREPARE_TO_GET_OPERATION;
		if (askAuth(operation, requiredPath)) {
			return new AuthorizationStatus(true, "");
		} else {
			return new AuthorizationStatus(false, "You are not authorized to access the requested resource");
		}
	}

}
