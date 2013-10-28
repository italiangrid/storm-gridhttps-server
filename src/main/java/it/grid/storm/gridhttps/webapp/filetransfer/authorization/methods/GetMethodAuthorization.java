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
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.srmOperations.PrepareToGetStatus;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class GetMethodAuthorization extends FileTransferMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(GetMethodAuthorization.class);
		
	public GetMethodAuthorization() {
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
		File resource = new File(srcSA.getRealPath(srcPath));
		if (!resource.exists()) 
			return AuthorizationStatus.NOTAUTHORIZED(400, "File does not exist");
		if (!resource.isFile())
			return AuthorizationStatus.NOTAUTHORIZED(400,"Resource required is not a file");
		AuthorizationStatus status = doPrepareToGetStatus(user, new Surl(resource, srcSA));
		if (!status.isAuthorized())
			return status;
		return super.askAuth(user, Constants.READ_OPERATION, srcSA.getRealPath(srcPath));
	}

	private AuthorizationStatus doPrepareToGetStatus(UserCredentials user, Surl surl) {
		log.debug("Check for a prepare-to-get");	
		SurlArrayRequestOutputData outputSPtG;
		try {
			BackendApi backEnd = new BackendApi(Configuration.getBackendInfo().getHostname(), new Long(Configuration.getBackendInfo().getPort()));
			PrepareToGetStatus operation = new PrepareToGetStatus(surl);
			outputSPtG = operation.executeAs(user, backEnd);
		} catch (SRMOperationException e) {
			log.error(e.toString());
			return AuthorizationStatus.NOTAUTHORIZED(500, e.getMessage());
		} catch (ApiException e) {
			log.error(e.getMessage());
			return AuthorizationStatus.NOTAUTHORIZED(500, e.getMessage());
		}
		String requestStatus = outputSPtG.getStatus().getStatusCode().getValue();
		log.debug("Request-status: " + requestStatus);
		if (requestStatus.equals("SRM_INVALID_REQUEST")) {
			return AuthorizationStatus.NOTAUTHORIZED(412, "You must do a prepare-to-get on surl '" + surl.asString() + "' before!");			
		} else if (requestStatus.equals("SRM_SUCCESS")) {
			String surlStatus = outputSPtG.getStatus(surl.asString()).getStatusCode().getValue();
			log.debug("Surl-status: " + surlStatus);
			if (surlStatus.equals("SRM_FILE_PINNED")) {
				return AuthorizationStatus.AUTHORIZED();
			} 
			return AuthorizationStatus.NOTAUTHORIZED(412, outputSPtG.getStatus(surl.asString()).getExplanation());			
		}
		return AuthorizationStatus.NOTAUTHORIZED(500, outputSPtG.getStatus().getExplanation());
	}
	
}
