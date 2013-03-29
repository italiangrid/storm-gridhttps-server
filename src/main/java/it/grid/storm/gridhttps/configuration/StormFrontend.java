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
package it.grid.storm.gridhttps.configuration;

import it.grid.storm.gridhttps.configuration.exceptions.InitException;


public class StormFrontend {
	private String hostname;
	private int port;

	public StormFrontend(String hostname, int port, int servicePort) {
		this();
		this.setHostname(hostname);
		this.setPort(port);
	}

	public StormFrontend() {
		this.setPort(DefaultConfiguration.STORM_FE_PORT);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String toString() {
		return "{'" + hostname + "', " + port + "}";
	}

	public void checkConfiguration() throws InitException {
		if (hostname.isEmpty())
			throw new InitException("backend hostname is empty!");
		if (port <= 0)
			throw new InitException("backend port is " + port + "!");
	}
}