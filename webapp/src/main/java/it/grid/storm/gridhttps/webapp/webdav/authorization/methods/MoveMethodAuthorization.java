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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

public class MoveMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(MoveMethodAuthorization.class);

	public MoveMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}
	
	public AuthorizationStatus isUserAuthorized() {
		String destinationURL = getHttpHelper().getDestinationHeader();
		boolean hasDestination = destinationURL != null;
		if (hasDestination) { 
			StorageArea reqStorageArea, destStorageArea;
			try {
				reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
				destStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getDestinationURI());
			} catch (IllegalArgumentException e) {
				log.error(e.getMessage());
				return new AuthorizationStatus(false, e.getMessage());
			} catch (IllegalStateException e) {
				log.error(e.getMessage());
				return new AuthorizationStatus(false, e.getMessage());
			}
			String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
			if (askAuth(Constants.MOVE_FROM_OPERATION, reqPath)) {
				String destPath = destStorageArea.getRealPath(getHttpHelper().getDestinationURI().getPath());
				String operation = getHttpHelper().isOverwriteRequest() ? Constants.MOVE_TO_OVERWRITE_OPERATION : Constants.MOVE_TO_OPERATION;
				if (askAuth(operation, destPath)) {
					return new AuthorizationStatus(true, "");
				} else {
					return new AuthorizationStatus(false, "You are not authorized to access the required resource (destination)");
				}
			} else {
				return new AuthorizationStatus(false, "You are not authorized to access the required resource (source)");
			}
		} else {
			return new AuthorizationStatus(false, "no destination header found");
		}
	}
	
}