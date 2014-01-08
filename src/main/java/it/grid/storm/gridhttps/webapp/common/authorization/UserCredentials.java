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

import java.util.ArrayList;

public class UserCredentials {

	private final static String EMPTY_USERDN = "";
	private final static String EMPTY_FQANS_AS_STR = "";
	private final static String ANONYMOUS = "anonymous";

	private String userDN;
	private ArrayList<String> userFQANS;

	public UserCredentials() {
		this(EMPTY_USERDN, new ArrayList<String>());
	}

	public UserCredentials(String dn, ArrayList<String> fqans) {
		setUserDN(dn);
		setUserFQANS(fqans);
	}
	
	public String getUserDN() {
		return userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return userFQANS;
	}
	
	public String getUserFQANSAsStr() {
		if (userFQANS.isEmpty()) {
			return EMPTY_FQANS_AS_STR;
		}
		String out = userFQANS.get(0);
		for (int i=1; i<userFQANS.size(); i++) {
			out += ", " + userFQANS.get(i);
		}
		return out;
	}

	public boolean isAnonymous() {
		return userDN.isEmpty() && userFQANS.isEmpty();
	}

	private void setUserDN(String userDN) {
		this.userDN = userDN;
	}

	private void setUserFQANS(ArrayList<String> userFQANS) {
		this.userFQANS = userFQANS;
	}
	
	public String getShortName() {
		return isAnonymous() ? ANONYMOUS : userDN;
	}
	
	public String getFullName() {
		if (isAnonymous()) {
			return ANONYMOUS;
		}
		if (userFQANS.isEmpty()) {
			return userDN;
		} 
		return String.format("userDN %s with fqans {%s}", userDN, getUserFQANSAsStr());
	}
	
	public String toString() {
		if (isAnonymous()) {
			return String.format("[%s]", ANONYMOUS);
		}
		return String.format("[dn=%s, fqans={%s}]", userDN, getUserFQANSAsStr());
	}

}