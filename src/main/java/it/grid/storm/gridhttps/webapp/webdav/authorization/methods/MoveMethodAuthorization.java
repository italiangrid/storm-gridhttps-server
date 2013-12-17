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

public class MoveMethodAuthorization extends WebDAVMethodAuthorization {
	
	private static final Logger log = LoggerFactory.getLogger(MoveMethodAuthorization.class);
	
	public MoveMethodAuthorization() {
		super();
	}

	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user)
		throws AuthorizationException {

		HttpHelper httpHelper = new HttpHelper(request, response);
		
		if (!httpHelper.hasDestinationHeader())
			return AuthorizationStatus.NOTAUTHORIZED(400, "No destination header found");
		
		String srcPath = this.stripContext(httpHelper.getRequestURI().getRawPath());
		log.debug(getClass().getSimpleName() + ": from path = " + srcPath);
		String destPath = this.stripContext(httpHelper.getDestinationURI().getRawPath());
		log.debug(getClass().getSimpleName() + ": to path = " + destPath);
		
		if (srcPath.equals(destPath))
			return AuthorizationStatus.NOTAUTHORIZED(403, "The source and destination URIs are the same!");
		
		StorageArea srcSA = StorageAreaManager.getMatchingSA(srcPath);
		if (srcSA == null)
			return AuthorizationStatus.NOTAUTHORIZED(400, "Unable to resolve storage area!");
		log.debug(getClass().getSimpleName() + ": from storage area = " + srcSA.getName());
		if (!srcSA.isProtocol(httpHelper.getRequestProtocol().toUpperCase()))
			return AuthorizationStatus.NOTAUTHORIZED(401, "Storage area " + srcSA.getName() + " doesn't support " + httpHelper.getRequestProtocol() + " protocol");
		
		StorageArea destSA = StorageAreaManager.getMatchingSA(destPath);
		if (destSA == null)
			return AuthorizationStatus.NOTAUTHORIZED(400, "Unable to resolve storage area!");
		log.debug(getClass().getSimpleName() + ": to storage area = " + destSA.getName());
		if (!destSA.isProtocol(httpHelper.getDestinationProtocol().toUpperCase()))
			return AuthorizationStatus.NOTAUTHORIZED(401, "Storage area " + destSA.getName() + " doesn't support " + httpHelper.getDestinationProtocol() + " protocol");
		
		AuthorizationStatus srcResponse;
		
		srcResponse = super.isAuthorized(request.getScheme(), srcSA, Operation.READWRITE, user);
		if (!srcResponse.isAuthorized())
			return srcResponse;
		if (destSA.getName().equals(srcSA.getName()))
			return srcResponse;
		
		return super.isAuthorized(request.getScheme(), destSA, Operation.READWRITE, user);
	}
	
}