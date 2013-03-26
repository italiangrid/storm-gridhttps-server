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
package it.grid.storm.gridhttps.webapp.webdav.factory;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.StormResource;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StormResourceFactory extends StormFactory {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);

	public StormResourceFactory() throws UnknownHostException, ApiException {
		super(Configuration.getBackendHostname(), Configuration.getBackendPort(), new File(Configuration.getGpfsRootDirectory()),
				Configuration.getWebdavContextPath());
		log.debug("StormResourceFactory created");
	}

	@Override
	public StormResource getDirectoryResource(File directory, StorageArea storageArea) {
		return new WebdavDirectoryResource(this, directory, storageArea);
	}

	@Override
	public StormResource getFileResource(File file, StorageArea storageArea) {
		return new WebdavFileResource(this, file, storageArea);
	}

	@Override
	public StormResource getFileResource(File file, StorageArea storageArea, SurlInfo surlinfo) {
		return new WebdavFileResource(this, file, storageArea, surlinfo);
	}

	@Override
	public StormResource getDirectoryResource(File directory, StorageArea storageArea, SurlInfo surlinfo) {
		return new WebdavDirectoryResource(this, directory, storageArea, surlinfo);
	}

}