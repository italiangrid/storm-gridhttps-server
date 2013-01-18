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
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;

public class CopyMethodAuthorization extends AbstractMethodAuthorization {

	private StorageArea srcSA;
	private StorageArea destSA;

	public CopyMethodAuthorization(HttpHelper httpHelper, StorageArea srcSA, StorageArea destSA) {
		super(httpHelper);
		this.srcSA = srcSA;
		this.destSA = destSA;
	}
	
	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		if (getHTTPHelper().hasDestinationHeader()) { 
			URI srcURI = getHTTPHelper().getRequestURI();
			URI destURI = getHTTPHelper().getDestinationURI();
			String path = srcSA.getRealPath(srcURI.getPath());
			String operation = Constants.CP_FROM_OPERATION;
			AuthorizationStatus srcResponse = askAuth(user, operation, path);
			if (srcResponse.isAuthorized()) {
				path = destSA.getRealPath(destURI.getPath());
				operation = getHTTPHelper().isOverwriteRequest() ? Constants.CP_TO_OVERWRITE_OPERATION : Constants.CP_TO_OPERATION;
				return askAuth(user, operation, path);
			} 
			return srcResponse;
		} 
		return AuthorizationStatus.NOTAUTHORIZED(400, "no destination header found");
	}

}