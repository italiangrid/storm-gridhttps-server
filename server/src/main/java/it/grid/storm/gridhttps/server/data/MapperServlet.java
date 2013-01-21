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
package it.grid.storm.gridhttps.server.data;

import it.grid.storm.gridhttps.server.DefaultConfiguration;
import it.grid.storm.gridhttps.server.exceptions.InitException;


public class MapperServlet {
	private String contextPath;
	private String contextSpec;
	private int port;

	public MapperServlet(String contextPath, String contextSpec, int port) {
		this();
		this.setContextPath(contextPath);
		this.setContextSpec(contextSpec);
		this.setPort(port);
	}

	public MapperServlet() {
		this.setContextPath(DefaultConfiguration.MAPPER_SERVLET_CONTEXT_PATH);
		this.setContextSpec(DefaultConfiguration.MAPPER_SERVLET_CONTEXT_SPEC);
		this.setPort(DefaultConfiguration.STORM_GHTTPS_MAPPER_SERVLET_PORT);
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextSpec() {
		return contextSpec;
	}

	public void setContextSpec(String contextSpec) {
		this.contextSpec = contextSpec;
	}

	public String toString() {
		return "{'" + contextPath + "', " + contextSpec + "}";
	}

	public void checkConfiguration() throws InitException {
		if (contextPath.isEmpty())
			throw new InitException("contextPath is empty!");
		if (contextSpec.isEmpty())
			throw new InitException("contextSpec is empty!");
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}