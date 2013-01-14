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

public class MoveMethodAuthorization extends AbstractMethodAuthorization {
	
	private StorageArea srcSA;
	private StorageArea destSA;

	public MoveMethodAuthorization(StorageArea srcSA, StorageArea destSA) {
		this.srcSA = srcSA;
		this.destSA = destSA;
	}
	
	public AuthorizationStatus isUserAuthorized() {
		HttpHelper httpHelper = HttpHelper.getHelper();
		if (httpHelper.hasDestinationHeader()) { 
			URI srcURI = httpHelper.getRequestURI();
			URI destURI = httpHelper.getDestinationURI();
			String srcPath = srcSA.getRealPath(srcURI.getPath());
			if (askAuth(Constants.MOVE_FROM_OPERATION, srcPath)) {
				String destPath = destSA.getRealPath(destURI.getPath());
				String operation = httpHelper.isOverwriteRequest() ? Constants.MOVE_TO_OVERWRITE_OPERATION : Constants.MOVE_TO_OPERATION;
				if (askAuth(operation, destPath)) {
					return new AuthorizationStatus(true, "");
				} else {
					return new AuthorizationStatus(false, "You are not authorized to access the destination resource");
				}
			} else {
				return new AuthorizationStatus(false, "You are not authorized to access the requested resource");
			}
		} else {
			return new AuthorizationStatus(false, "no destination header found");
		}
	}
	
}