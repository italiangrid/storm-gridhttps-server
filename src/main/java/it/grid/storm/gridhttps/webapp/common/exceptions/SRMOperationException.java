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
package it.grid.storm.gridhttps.webapp.common.exceptions;

import it.grid.storm.srm.types.TReturnStatus;

public class SRMOperationException extends RuntimeException {
	
	private static final long serialVersionUID = 1200998154780371147L;

	private TReturnStatus status;
	
	public SRMOperationException(TReturnStatus status) {
		super(status.toString());
		this.setStatus(status);
	}

	public TReturnStatus getStatus() {
		return status;
	}

	private void setStatus(TReturnStatus status) {
		this.status = status;
	}

}