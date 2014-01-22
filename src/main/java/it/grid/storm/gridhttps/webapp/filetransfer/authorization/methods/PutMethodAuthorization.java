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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.HttpHelper;
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
		
		String srcPath = this.stripContext(httpHelper.getRequestURI().getRawPath());
		log.debug("context stripped: " + srcPath);
		srcPath = this.resolvePath(srcPath);
		log.debug("resolved path: " + srcPath);
		StorageArea srcSA = getMatchingSA(srcPath);
		log.debug("path {} matches storage area {}", srcPath, srcSA.getName());
		AuthorizationStatus status = checkSA(srcSA, httpHelper.getRequestProtocol());
		if (!status.isAuthorized()) {
			return status;
		}
		if (user.isAnonymous()) {
			if (srcSA.isHTTPWritable()) {
				return AuthorizationStatus.AUTHORIZED();
			}
			return AuthorizationStatus.NOTAUTHORIZED(
				HttpServletResponse.SC_FORBIDDEN, String.format(
					"Unauthorized: Anonymous users are not authorized to read %s",
					httpHelper.getRequestStringURI()));
		}
		/* user is not anonymous */
		if (srcSA.isHTTPWritable()) {
			return AuthorizationStatus.AUTHORIZED();
		}
		return super.askBEAuth(user, Constants.WRITE_OPERATION,
			srcSA.getRealPath(srcPath));
	}
}
