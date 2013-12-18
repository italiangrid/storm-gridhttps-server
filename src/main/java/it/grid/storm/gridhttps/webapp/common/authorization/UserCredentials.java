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
package it.grid.storm.gridhttps.webapp.common.authorization;

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
				fqans.addAll(voms.getFQANs());
			
			if (!fqans.isEmpty())
				setUserFQANS(fqans);
			
			log.debug("User: " + this.getUserDN());
		} else {
			log.debug("User: anonymous");
		}
	}

	public String getUserDN() {
		return userDN;
	}
	
	public String getRealUserDN() {
		return userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return userFQANS;
	}
	
	public String getUserFQANSAsStr() {
		if (userFQANS.isEmpty())
			return "";
		String out = userFQANS.get(0);
		for (int i=1; i<userFQANS.size(); i++) {
			out += userFQANS.get(i);
		}
		return out;
	}

	public boolean isAnonymous() {
		return getHttpHelper().isHttp() && isUserDNEmpty() && isUserFQANSEmpty();
	}

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