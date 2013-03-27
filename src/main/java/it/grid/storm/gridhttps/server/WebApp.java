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
package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.utils.Zip;

import java.io.File;
import java.io.IOException;

public class WebApp {

	private File resourceBase;
	private File descriptorFile;

	public WebApp(File resourceBase) {
		this.setResourceBase(resourceBase);
		this.setDescriptorFile(new File(resourceBase, "/WEB-INF/web.xml"));
	}

	public File getDescriptorFile() {
		return descriptorFile;
	}

	private void setDescriptorFile(File descriptorFile) {
		this.descriptorFile = descriptorFile;
	}

	public File getResourceBase() {
		return resourceBase;
	}

	private void setResourceBase(File resourceBase) {
		this.resourceBase = resourceBase;
	}

	public void init(File warFile) throws IOException {
		if (!getResourceBase().exists()) {
			if (getResourceBase().mkdirs()) {
				Zip.unzip(warFile.toString(), getResourceBase().toString());
			} else {
				throw new IOException("Error on creation of '" + getResourceBase() + "' directory!");
			}
		} else {
			throw new IOException("'" + getResourceBase() + "' already exists!");
		}
	}

}