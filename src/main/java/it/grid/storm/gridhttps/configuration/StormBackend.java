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

public class StormBackend {
	
	private String hostname;
	private int port;
	private String token;
	private int servicePort;

	
	public StormBackend(String hostname, int port, String token, int servicePort) {
		this();
		this.setHostname(hostname);
		this.setPort(port);
		this.setToken(token);
		this.setServicePort(servicePort);
	}
	
	public StormBackend() {
		this.setPort(DefaultConfiguration.STORM_BE_PORT);
		this.setServicePort(DefaultConfiguration.STORM_BE_SERVICE_PORT);
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
	
	/**
	 * @return the token
	 */
	public String getToken() {
	
		return token;
	}
	
	/**
	 * @param token the token to set
	 */
	public void setToken(String token) {
	
		this.token = token;
	}

	public int getServicePort() {
		return servicePort;
	}

	public void setServicePort(int servicePort) {
		this.servicePort = servicePort;
	}
	
	public String toString() {
		return "{'"+hostname+"', "+port+", "+servicePort+"}";	
	}
	
	public void checkConfiguration() throws InitException {
		if (hostname.isEmpty())
			throw new InitException("backend hostname is empty!");
		if (port <= 0)
			throw new InitException("backend port is "+port+"!");
		if(token == null)
			throw new InitException("backend token must be provide");
		if (servicePort <= 0)
			throw new InitException("backend service port is "+servicePort+"!");
		if (servicePort == port)
			throw new InitException("backend port is equal to service port!");
	}

}