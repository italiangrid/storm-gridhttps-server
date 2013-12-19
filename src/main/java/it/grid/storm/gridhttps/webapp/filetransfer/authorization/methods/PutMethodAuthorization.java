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
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.InvalidRequestException;

public class PutMethodAuthorization extends FileTransferMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(PutMethodAuthorization.class);
		
	public PutMethodAuthorization() {
		super();
	}

	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user)
		throws AuthorizationException, InvalidRequestException {

		HttpHelper httpHelper = new HttpHelper(request, response);
		
		String srcPath = this.stripContext(httpHelper.getRequestURI().getRawPath());
		StorageArea srcSA = getMatchingSA(srcPath);
		log.debug("path {} matches storage area {}", srcPath, srcSA.getName());
		AuthorizationStatus status = checkSA(srcSA, httpHelper.getRequestProtocol());
		if (!status.isAuthorized()) {
			return status;
		}

		File resource = new File(srcSA.getRealPath(srcPath));
		if (srcSA.isHTTPWritable()) {
			/* Anonymous or not, user can always write */
			if (!resource.exists()) {
				/* It's impossible that a PtP has been done if the resource doesn't exist */
				return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_PRECONDITION_FAILED, "Precondition Failed: File doesn't exist! Please perform an SRM prepareToPut request for such file before calling this method.");
			} else if (!resource.isFile()) {
				/* Can't put on a directory */
				return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed: Resource required already exists and is not a file");
			} else {
				return AuthorizationStatus.AUTHORIZED();
			}
		} else if (user.isAnonymous()) {
			/* SA is not writable by anonymous users */
			return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_FORBIDDEN, "Unauthorized: Anonymous users are not authorized to read " + httpHelper.getRequestStringURI());
		} else {
			return super.askBEAuth(user, Constants.WRITE_OPERATION, srcSA.getRealPath(srcPath));
		}
	}	
}
