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

import java.io.File;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;

public class PutMethodAuthorization extends FileTransferMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(PutMethodAuthorization.class);
		
	public PutMethodAuthorization() {
		super();
	}

	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user) {

		HttpHelper httpHelper = new HttpHelper(request, response);
		AuthorizationStatus status = null;
		
		String uriPath = httpHelper.getRequestURI().getRawPath();
		log.debug("uriPath: {}", uriPath);
		
		StorageArea matched = getMatchingStorageArea(uriPath);
		String realPath = matched.getRealPath(stripContext(uriPath));
		log.debug("real path is {}", realPath);

		status = checkUserWritePermissionsOnStorageArea(user, matched);
		if (status == null) {
			/* not anonymous user and https protocol */
			status = super.askBEAuth(user, Constants.WRITE_OPERATION, realPath);
		}
		if (!status.isAuthorized()) {
			return status;
		}
		/* is authorized */
		File file = new File(realPath);
		String canPath = null;
		try {
			canPath = file.getCanonicalPath();
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new AuthorizationException(e.getMessage());
		}
		log.debug("canonical path is: {}", canPath);
		StorageArea target = StorageAreaManager.getMatchingSAFromFsPath(canPath);
		log.debug("target storage area is: {}", target.getName());
		
		if (matched.getFSRoot().equals(target.getFSRoot())) {
			log.debug("target storage area matches {}!", matched.getName());
			return status;
		}
		
		log.debug("target storage area doesn't match {}!", matched.getName());
		status = checkUserWritePermissionsOnStorageArea(user, target);
		if (status == null) {
			status = super.askBEAuth(user, Constants.WRITE_OPERATION, canPath);
		}
		return status;
	}
}
