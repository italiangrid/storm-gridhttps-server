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
package it.grid.storm.gridhttps.webapp.filetransfer.authorization;

import it.grid.storm.gridhttps.webapp.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.PutMethodAuthorization;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferAuthorization extends AuthorizationFilter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferAuthorization.class);

	private ArrayList<String> allowedMethods = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			add("GET");
			add("PUT");
		}
	};
	
	@Override
	public AuthorizationStatus isUserAuthorized(HttpServletRequest request,
		HttpServletResponse response, UserCredentials user) throws AuthorizationException {

		/* check method */
		String method = request.getMethod().toUpperCase();
		if (!isMethodAllowed(method)) {
			log.error("Received a request for a not allowed method : " + method);
			return AuthorizationStatus.NOTAUTHORIZED(405, "Method " + method + " not allowed!");
		}
		
		/* get method handler */
		AbstractMethodAuthorization authHandler = getAuthorizationMethodHandler(method);
		
		return authHandler.isUserAuthorized(request, response, user);
	}

	private AbstractMethodAuthorization getAuthorizationMethodHandler(String method) throws AuthorizationException {
		if (method.equals("GET"))
			return new GetMethodAuthorization();
		if (method.equals("PUT"))
			return new PutMethodAuthorization();
		throw new AuthorizationException("Invalid method!");
	}

	private boolean isMethodAllowed(String method) {
		return allowedMethods.contains(method);
	}
	
}