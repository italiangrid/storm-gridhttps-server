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
package it.grid.storm.gridhttps.webapp.authorization;

import it.grid.storm.gridhttps.webapp.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentials {

	private static final Logger log = LoggerFactory.getLogger(UserCredentials.class);

	private final String EMPTY_USERDN = "";

	private String userDN;
	private ArrayList<String> userFQANS;
	private HttpHelper httpHelper;

	private static UserCredentials instance;

	public static void init(HttpHelper httpHelper) {
		instance = new UserCredentials(httpHelper);
	}

	public static UserCredentials getUser() {
		return instance;
	}

	private UserCredentials(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
		initAsAnonymous();
		if (this.httpHelper.isHttp())
			return;
		/* It's an HTTPS request: */
		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext currentContext = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(currentContext);
		X509Certificate[] certChain = httpHelper.getX509Certificate();
		if (certChain == null) {
			log.warn("Failed to init VOMS Security Context! User initialized with empty DN and FQANs");
			return;
		}
		currentContext.setClientCertChain(certChain);
		userDN = currentContext.getClientDN() != null ? currentContext.getClientDN().getX500() : getEmptyUserDN();
		log.debug("DN = " + userDN);
		userFQANS.clear();
		for (VOMSAttribute voms : currentContext.getVOMSAttributes())
			for (String s : voms.getFQANs()) {
				userFQANS.add(s);
				log.debug("fqan = " + s);
			}
	}

	/* public methods */

	public String getUserDN() {
		return isForcedAnonymous() ? getEmptyUserDN() : userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return isForcedAnonymous() ? getEmptyUserFQANS() : userFQANS;
	}

	public boolean isAnonymous() {
		return isForcedAnonymous() || (httpHelper.isHttp() && isUserDNEmpty() && isUserFQANSEmpty());
	}

	public void forceAnonymous() {
		httpHelper.getSession().setAttribute("forced", true);
	}

	public void unforceAnonymous() {
		httpHelper.getSession().setAttribute("forced", false);
	}

	/* private methods */

	private void initAsAnonymous() {
		userDN = getEmptyUserDN();
		userFQANS = getEmptyUserFQANS();
	}

	private boolean isForcedAnonymous() {
		return (Boolean) httpHelper.getSession().getAttribute("forced");
	}

	private String getEmptyUserDN() {
		return EMPTY_USERDN;
	}

	private ArrayList<String> getEmptyUserFQANS() {
		return new ArrayList<String>();
	}

	private boolean isUserDNEmpty() {
		return userDN.isEmpty();
	}

	private boolean isUserFQANSEmpty() {
		return userFQANS.isEmpty();
	}
	
}