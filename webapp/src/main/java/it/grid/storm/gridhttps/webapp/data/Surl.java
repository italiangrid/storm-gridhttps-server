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
package it.grid.storm.gridhttps.webapp.data;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Surl {
	
	private static final Logger log = LoggerFactory.getLogger(Surl.class);
	
	private URI surl;
	private final String scheme = "srm";
	
	public Surl(String path) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), path);
	}
	
	public Surl(String feHostname, int fePort, String path) {
		buildURI(feHostname, fePort, path);
	}
	
	public Surl(String feHostname, int fePort, File resource) {
		StorageArea storageArea = StorageAreaManager.getMatchingSA(resource);
		buildURI(feHostname, fePort, resource.getPath().replaceFirst(storageArea.getFSRoot(), storageArea.getStfnRoot()));
	}
	
	public Surl(File resource) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), resource);
	}
	
	public Surl(String feHostname, int fePort, File resource, StorageArea storageArea) {
		buildURI(feHostname, fePort, resource.getPath().replaceFirst(storageArea.getFSRoot(), storageArea.getStfnRoot()));
	}
	
	public Surl(File resource, StorageArea storageArea) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), resource, storageArea);
	}
	
	public Surl(Surl baseSurl, String childName) {
		buildURI(baseSurl.asURI().getHost(), baseSurl.asURI().getPort(), baseSurl.asURI().getRawPath() + File.separator + childName);
	}
	
	private void buildURI(String feHostname, int fePort, String path) {
		try {
			surl = new URI(scheme, null, feHostname, fePort, path, null, null);
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
		}
	}
	
	public String asString() {
		return this.surl.toASCIIString();
	}
	
	public String toString() {
		return this.surl.toASCIIString();
	}
	
	public URI asURI() {
		return this.surl;
	}
	
}