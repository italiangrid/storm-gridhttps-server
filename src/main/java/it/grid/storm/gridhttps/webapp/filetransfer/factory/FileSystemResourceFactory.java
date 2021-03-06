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
package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.gridhttps.webapp.webdav.factory.WebdavResourceFactory;
import it.grid.storm.xmlrpc.ApiException;

import java.io.File;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileSystemResourceFactory extends StormFactory {

	private static final Logger log = LoggerFactory.getLogger(WebdavResourceFactory.class);

	public FileSystemResourceFactory() throws UnknownHostException, ApiException {

		super(Configuration.getBackendInfo().getHostname(), Configuration
			.getBackendInfo().getPort(), Configuration.getGridhttpsInfo()
			.getFiletransferContextPath());
		setAllowDirectoryBrowsing(false);
		log.debug("FileSystemResourceFactory created!");
	}

	@Override
	public StormResource getDirectoryResource(StorageArea storageArea, File directory) {
		return new FileTransferDirectoryResource(this, storageArea, directory);
	}

	@Override
	public StormResource getFileResource(StorageArea storageArea, File file) {
		return new FileTransferFileResource(this, storageArea, file);
	}

}
