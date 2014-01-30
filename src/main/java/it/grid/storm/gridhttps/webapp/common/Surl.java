/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.common;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Surl {

	private static final Logger log = LoggerFactory.getLogger(Surl.class);

	private URI surl;
	private final String scheme = "srm";

	public Surl(StormResource resource) {
		this(resource.getStorageArea(), resource.getFile());
	}
	
	public Surl(StorageArea storageArea, File file) {

		/* verify parameters */
		if (storageArea == null) {
			throw new IllegalArgumentException("storageArea must not be null!");
		}
		if (file == null) {
			throw new IllegalArgumentException("file must not be null!");
		}
		
		/* check if file is owned by storage area */
		if (!storageArea.isOwner(file)) {
			String msg = String.format(
				"Surl: file %s doesn't belong to storage area %s", file,
				storageArea.getName());
			log.error(msg);
			throw new IllegalArgumentException(msg);
		}

		log.debug("Surl: file {} belongs to storage area {}", file,
			storageArea.getName());

		/* get canonical path */
		String path;
		try {
			path = file.getCanonicalPath();
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new RuntimeException(e.getMessage());
		}
		
		/* replace the storage area fs root with the storage area stfn root */
		if (storageArea.getFSRoot().endsWith(File.separator)) {
			path = path.replaceFirst(storageArea.getFSRoot(),
				storageArea.getStfnRoot() + File.separator);
		} else {
			path = path.replaceFirst(storageArea.getFSRoot(),
				storageArea.getStfnRoot());
		}
		
		/* build surl as URI */
		buildURI(Configuration.getFrontendInfo().getHostname(), Configuration
			.getFrontendInfo().getPort(), path);
	}

	public Surl(String path) {

		this(Configuration.getFrontendInfo().getHostname(), Configuration
			.getFrontendInfo().getPort(), path);
	}

	public Surl(String feHostname, int fePort, String path) {

		buildURI(feHostname, fePort, path);
	}

	public Surl(Surl baseSurl, String childName) {

		buildURI(baseSurl.getHostname(), baseSurl.getPort(), baseSurl.getPath()
			+ File.separator + childName);
	}

	private void buildURI(String feHostname, int fePort, String path) {

		try {
			surl = new URI(scheme, null, feHostname, fePort, path, null, null);
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
		}
	}

	public String toString() {

		return surl.toASCIIString();
	}

	public String getHostname() {
		
		return surl.getHost();
	}
	
	public int getPort() {
		
		return surl.getPort();
	}
	
	public String getPath() {
		
		return surl.getRawPath();
	}

}