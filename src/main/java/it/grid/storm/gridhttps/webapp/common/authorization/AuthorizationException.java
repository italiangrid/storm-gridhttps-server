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

import javax.servlet.http.HttpServletResponse;

public class AuthorizationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private int errorcode;
	private String msg;

	public AuthorizationException(String msg) {

		this(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
	}

	public AuthorizationException(Throwable t){

		this(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());
	}

	public AuthorizationException(int error, String msg) {

		this.errorcode = error;
		this.msg = msg;
	}

	/**
	 * @return the errorcode
	 */
	public int getErrorcode() {
	
		return errorcode;
	}

	/**
	 * @return the msg
	 */
	public String getMessage() {
	
		return msg;
	}
	
}
