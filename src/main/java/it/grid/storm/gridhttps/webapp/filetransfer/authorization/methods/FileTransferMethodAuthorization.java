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
package it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods;

import java.io.File;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.authorization.methods.AbstractMethodAuthorization;

public abstract class FileTransferMethodAuthorization extends AbstractMethodAuthorization {

	private String contextPath;
	
	public FileTransferMethodAuthorization() {		
		this.setContextPath(Configuration.getGridhttpsInfo().getFiletransferContextPath());
	}
	
	protected String stripContext(String path) {
		if (this.getContextPath().isEmpty())
			return path;
		String contextPath = File.separator + this.getContextPath();
		String stripped = path.replaceFirst(contextPath, "");
		if (stripped.isEmpty())
			return File.separator;
		return stripped;
	}

	public String getContextPath() {
		return contextPath;
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

}