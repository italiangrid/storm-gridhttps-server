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
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;

public class PropfindMethodAuthorization extends WebDAVMethodAuthorization {
	
	private static final Logger log = LoggerFactory.getLogger(PropfindMethodAuthorization.class);
	
	public PropfindMethodAuthorization() {
		super();
	}

	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user)
		throws AuthorizationException {

		HttpHelper httpHelper = new HttpHelper(request, response);
		String srcPath = this.stripContext(httpHelper.getRequestURI().getRawPath());
		log.debug(getClass().getSimpleName() + ": path = " + srcPath);
		StorageArea srcSA = StorageAreaManager.getMatchingSA(srcPath);
		if (srcSA == null)
			return AuthorizationStatus.NOTAUTHORIZED(400, "Unable to resolve storage area!");
		log.debug(getClass().getSimpleName() + ": storage area = " + srcSA.getName());
		if (!srcSA.isProtocol(httpHelper.getRequestProtocol().toUpperCase()))
			return AuthorizationStatus.NOTAUTHORIZED(401, "Storage area " + srcSA.getName() + " doesn't support " + httpHelper.getRequestProtocol() + " protocol");
		
		return super.isAuthorized(request.getScheme(), srcSA, Operation.READ, user);
	}
}