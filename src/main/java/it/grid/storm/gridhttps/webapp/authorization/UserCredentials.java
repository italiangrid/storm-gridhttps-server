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

import org.apache.commons.lang.StringUtils;
import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentials extends Object {

	private static final Logger log = LoggerFactory.getLogger(UserCredentials.class);

	private final String EMPTY_USERDN = "";

	private String userDN;
	private ArrayList<String> userFQANS;
	private HttpHelper httpHelper;

	private HttpHelper getHttpHelper() {
		return httpHelper;
	}

	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

	public UserCredentials(HttpHelper httpHelper) {
		setHttpHelper(httpHelper);
		initAsAnonymous();
		initForcedAnonymous(false);
		if (!getHttpHelper().isHttp()) {
			//in case of https requests:
			X509Certificate[] certChain = httpHelper.getX509Certificate();
			if (certChain == null) {
				log.warn("Failed to init VOMS Security Context! User initialized with empty DN and FQANs");
				return;
			}
			getHttpHelper().getVOMSSecurityContext().setClientCertChain(certChain);
			if (getHttpHelper().getVOMSSecurityContext().getClientName() != null)
				setUserDN(getHttpHelper().getVOMSSecurityContext().getClientName());
			
			ArrayList<String> fqans = new ArrayList<String>();
			for (VOMSAttribute voms : getHttpHelper().getVOMSSecurityContext().getVOMSAttributes())
				for (String s : voms.getFQANs())
					fqans.add(s);
			if (!fqans.isEmpty())
				setUserFQANS(fqans);
			
			log.debug("User: " + this.getUserDN());
		} else {
			log.debug("User: anonymous");
		}
	}

	/* public methods */

	public String getUserDN() {
		return isForcedAnonymous() ? getEmptyUserDN() : userDN;
	}
	
	public String getRealUserDN() {
		return userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return isForcedAnonymous() ? getEmptyUserFQANS() : userFQANS;
	}

	public boolean isAnonymous() {
		return isForcedAnonymous() || (getHttpHelper().isHttp() && isUserDNEmpty() && isUserFQANSEmpty());
	}

	public void forceAnonymous() {
		log.debug("forcing anonymous user");
		getHttpHelper().getRequest().setAttribute("forced", true);
	}

	public void unforceAnonymous() {
		log.debug("unforcing anonymous user");
		getHttpHelper().getRequest().setAttribute("forced", false);
	}

	/* private methods */

	private void setUserDN(String userDN) {
		log.debug("DN: " + userDN);
		this.userDN = userDN;
	}

	private void setUserFQANS(ArrayList<String> userFQANS) {
		String s = StringUtils.join(userFQANS, ",");
		log.debug("FQANS: " + s);
		this.userFQANS = userFQANS;
	}
	
	private void initAsAnonymous() {
		setUserDN(getEmptyUserDN());
		setUserFQANS(getEmptyUserFQANS());
	}

	private void initForcedAnonymous(boolean value) {
		log.debug("init forcing anonymous as " + value);
		getHttpHelper().getRequest().setAttribute("forced", value);
	}

	private boolean isForcedAnonymous() {
		return (Boolean) getHttpHelper().getRequest().getAttribute("forced");
	}

	private String getEmptyUserDN() {
		return EMPTY_USERDN;
	}

	private ArrayList<String> getEmptyUserFQANS() {
		return new ArrayList<String>();
	}

	private boolean isUserDNEmpty() {
		return getUserDN().isEmpty();
	}

	private boolean isUserFQANSEmpty() {
		return getUserFQANS().isEmpty();
	}

}