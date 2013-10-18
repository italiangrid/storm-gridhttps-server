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

public class SRMOperationException extends BadRequestException {

	private static final long serialVersionUID = 1200998154780371147L;

	private TReturnStatus status;
	private Exception e;
	
	public SRMOperationException(TReturnStatus status) {
		this(status, null);
	}
	
	public SRMOperationException(TReturnStatus status, Exception ex) {
		super(status.toString());
		this.setStatus(status);
		this.setException(ex);
	}

	public TReturnStatus getStatus() {
		return status;
	}

	private void setStatus(TReturnStatus status) {
		this.status = status;
	}

	public Exception getException() {
		return e;
	}

	private void setException(Exception e) {
		this.e = e;
	}

}