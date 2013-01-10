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
package it.grid.storm.gridhttps.webapp;

import it.grid.storm.gridhttps.webapp.checksum.Checksum.ChecksumAlgorithm;

import java.util.HashMap;
import java.util.Set;

public class DefaultConfiguration {
	
	private static final HashMap<String, String> map = new HashMap<String, String>() { 
		private static final long serialVersionUID = -7460222372953081397L;

	{
		put("BACKEND_PORT", "8080");
		put("BACKEND_SERVICE_PORT", "9998");
		put("FRONTEND_PORT", "8444");
		put("WEBDAV_CONTEXTPATH", "");
		put("FILETRANSFER_CONTEXTPATH", "fileTransfer");
		put("GPFS_ROOT_DIRECTORY", "/");
		put("COMPUTE_CHECKSUM", "true");
		put("CHECKSUM_TYPE", ChecksumAlgorithm.ADLER32.name());
		put("REMOVE_SPACES", "true");
		put("REMOVE_SPACES_WITH", "_");
	}};

	public static String getValue(String name) {
		return map.get(name);
	}
	
	public static Set<String> getKeys() {
		return map.keySet();
	}
	
}