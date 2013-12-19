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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;

public class MoveMethodAuthorization extends WebDAVMethodAuthorization {
	
	private static final Logger log = LoggerFactory.getLogger(MoveMethodAuthorization.class);
	
	public MoveMethodAuthorization() {
		super();
	}

	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user) {

		HttpHelper httpHelper = new HttpHelper(request, response);
		
		if (!httpHelper.hasDestinationHeader()) {
			return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_BAD_REQUEST, "No destination header found");
		}
		
		String srcPath = this.stripContext(httpHelper.getRequestURI().getRawPath());
		String destPath = this.stripContext(httpHelper.getDestinationURI().getRawPath());
		
		if (srcPath.equals(destPath)) {
			return AuthorizationStatus.NOTAUTHORIZED(HttpServletResponse.SC_FORBIDDEN, "The source and destination URIs are the same!");
		}
		
		StorageArea srcSA = getMatchingSA(srcPath);
		log.debug("srcPath {} matches storage area {}", srcPath, srcSA.getName());
		StorageArea destSA = getMatchingSA(destPath);
		log.debug("destPath {} matches storage area {}", destPath, destSA.getName());
		
		AuthorizationStatus status = super.isAuthorized(request.getScheme(), srcSA, Permission.READWRITE, user);
		if (!status.isAuthorized()) {
			return status;
		}
		if (destSA.getName().equals(srcSA.getName())) {
			log.debug("source and destination storage area are the same");
			return status;
		} 
		log.debug("source and destination storage area are different");
		return super.isAuthorized(request.getScheme(), destSA, Permission.READWRITE, user);
	}
}