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

public class AuthorizationStatus {

	public static AuthorizationStatus AUTHORIZED() {
		return new AuthorizationStatus(true, 0, "");
	}

	public static AuthorizationStatus NOTAUTHORIZED(int errorcode, String errorMsg) {
		return new AuthorizationStatus(false, errorcode, errorMsg);
	}

	private boolean authorized;
	private String reason;
	private int errorcode;

	private AuthorizationStatus(boolean authorized, int errorcode, String reason) {
		if (authorized) {
			setAuthorized();
		} else {
			setUnauthorized(errorcode, reason);
		}
	}

	public boolean isAuthorized() {
		return authorized;
	}

	public void setAuthorized() {
		this.authorized = true;
		this.reason = "";
		this.errorcode = 0;
	}

	public void setUnauthorized(int errorcode, String msg) {
		this.authorized = false;
		this.reason = msg;
		this.errorcode = errorcode;
	}

	public String getReason() {
		return reason;
	}
	
	public int getErrorCode() {
		return errorcode;
	}

}