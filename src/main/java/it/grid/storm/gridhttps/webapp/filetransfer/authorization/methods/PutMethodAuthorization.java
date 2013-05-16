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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.srmOperations.PrepareToPutStatus;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	private static final Logger log = LoggerFactory.getLogger(PutMethodAuthorization.class);
	
	private StorageArea SA;
	
	public PutMethodAuthorization(HttpHelper httpHelper, StorageArea SA) {
		super(httpHelper);
		this.SA = SA;
	}

	private String stripContext(String url) {
		return url.replaceFirst(File.separator + Configuration.getGridhttpsInfo().getFiletransferContextPath(), "");
	}
	
	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		String path = stripContext(getHTTPHelper().getRequestURI().getRawPath());
		if (SA != null) {
			String reqPath = SA.getRealPath(path);
			File resource = new File(reqPath);
			if (resource.exists()) {
				if (resource.isFile()) {
					AuthorizationStatus status = doPrepareToPutStatus(new Surl(resource, SA));
					if (status.isAuthorized()) {
						return askAuth(user, Constants.WRITE_OPERATION, reqPath);
					} else {
						return status;
					}
				} else {
					return AuthorizationStatus.NOTAUTHORIZED(400, "Resource required is not a file"); 
				}
			} else {
				Surl surl = new Surl(resource, SA);
				return AuthorizationStatus.NOTAUTHORIZED(412, "File not exist! You must do a prepare-to-put on surl '" + surl + "' before!"); 
			}
		} else {
			return AuthorizationStatus.NOTAUTHORIZED(500, "Null storage area!");
		}
	}

	private AuthorizationStatus doPrepareToPutStatus(Surl surl) {
		log.debug("Check for a prepare-to-put");
		SurlArrayRequestOutputData outputSPtP;
		try {
			BackendApi backEnd = new BackendApi(Configuration.getBackendInfo().getHostname(), new Long(Configuration.getBackendInfo().getPort()));
			PrepareToPutStatus operation = new PrepareToPutStatus(surl);
			outputSPtP = operation.executeAs(this.getHTTPHelper().getUser(), backEnd);
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
			return AuthorizationStatus.NOTAUTHORIZED(500, e.getMessage());
		} catch (ApiException e) {
			log.error(e.getMessage());
			return AuthorizationStatus.NOTAUTHORIZED(500, e.getMessage());
		} 
		String requestStatus = outputSPtP.getStatus().getStatusCode().getValue();
		log.debug("Request-status: " + requestStatus);
		if (requestStatus.equals("SRM_INVALID_REQUEST")) {
			return AuthorizationStatus.NOTAUTHORIZED(412, "You must do a prepare-to-put on surl '" + surl.asString() + "' before!");			
		} else if (requestStatus.equals("SRM_SUCCESS")) {
			String surlStatus = outputSPtP.getStatus(surl.asString()).getStatusCode().getValue();
			log.debug("Surl-status: " + surlStatus);
			if (surlStatus.equals("SRM_SPACE_AVAILABLE")) {
				return AuthorizationStatus.AUTHORIZED();
			} 
			return AuthorizationStatus.NOTAUTHORIZED(412, outputSPtP.getStatus(surl.asString()).getExplanation());			
		}
		return AuthorizationStatus.NOTAUTHORIZED(500, outputSPtP.getStatus().getExplanation());
	}
}
