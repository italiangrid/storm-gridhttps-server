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
package it.grid.storm.gridhttps.webapp.data.exceptions;

import io.milton.http.exceptions.BadRequestException;
import it.grid.storm.srm.types.TReturnStatus;

public class TooManyResultsException extends BadRequestException {

	private static final long serialVersionUID = -3624109442893115370L;

	private TReturnStatus status;
	
	public TooManyResultsException(String reason, TReturnStatus status) {
		super(reason);
		this.setStatus(status);
	}

	public TooManyResultsException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public TReturnStatus getStatus() {
		return status;
	}

	private void setStatus(TReturnStatus status) {
		this.status = status;
	}

}