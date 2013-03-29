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

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	public OptionsMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	private static final Logger log = LoggerFactory.getLogger(OptionsMethodAuthorization.class);

	@Override
	public AuthorizationStatus isUserAuthorized(UserCredentials user) {
		String hostnameBE = Configuration.getBackendInfo().getHostname(); 
		int portBE = Configuration.getBackendInfo().getPort();
		/* ping storm-backend if method = OPTIONS */
		log.debug("ping " + hostnameBE + ":" + portBE);
		try {
			PingOutputData output = StormResourceHelper.doPing(hostnameBE, portBE, user);
			log.debug(output.getBeOs());
			log.debug(output.getBeVersion());
			log.debug(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage() + ": " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.error(e.getMessage() + ": " + e.getReason());
		}
		return AuthorizationStatus.AUTHORIZED();
	}
}
