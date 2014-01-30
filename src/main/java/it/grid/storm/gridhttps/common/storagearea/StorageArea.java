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
package it.grid.storm.gridhttps.common.storagearea;

import it.grid.storm.gridhttps.common.remotecall.ConfigDiscoveryServiceConstants.HttpPerms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michele Dibenedetto
 * @author Enrico Vianello
 */
public class StorageArea {
	
	private static final Logger log = LoggerFactory.getLogger(StorageArea.class);
	
	private String name;
	private String FSRoot;
	private String stfnRoot;
	private List<String> protocols;
	private HttpPerms httpPerms;
	
	/**
	 * @param name
	 *            the name of the storage area
	 * @param FSRoot
	 *            the File System root of the storage area
	 * @param stfnRoot
	 *            the storage file name root of the storage area
	 */
	public StorageArea(String name, String FSRoot, String stfnRoot, List<String> protocols, HttpPerms httpPerms) {
		this.name = name;
		this.FSRoot = normalizePath(FSRoot);
		this.stfnRoot = normalizePath(stfnRoot);
		this.protocols = new ArrayList<String>();
		for (String protocol : protocols)
			this.protocols.add(protocol.toUpperCase());
		this.httpPerms = httpPerms;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the file-transfer protocols to use to access resources
	 */
	public final List<String> getProtocols() {
		return protocols;
	}

	/**
	 * @return the root
	 */
	public final String getFSRoot() {
		return FSRoot;
	}

	/**
	 * Given a path string builds from it a path string with starting slash and
	 * without ending slash
	 * 
	 * @param path
	 *            a path string
	 * @return a path string with starting slash and without ending slash
	 */
	private final String normalizePath(String path) {
		if (path.charAt(path.length() - 1) == File.separatorChar) {
			if (path.charAt(0) != File.separatorChar) {
				return File.separatorChar + path.substring(0, path.length() - 1);
			} else {
				return path.substring(0, path.length() - 1);
			}
		} else {
			if (path.charAt(0) != File.separatorChar) {
				return File.separatorChar + path;
			} else {
				return path;
			}
		}
	}

	/**
	 * @return the stfnRoot
	 */
	public final String getStfnRoot() {
		return stfnRoot;
	}

	/**
	 * @return the String Array equivalent of the protocol
	 */
	public String[] getProtocolAsStrArray() {
		String[] out = new String[this.protocols.size()];
		int i = 0;
		for (String protocol : this.protocols)
			out[i++] = protocol;
		return out;
	}

	@Override
	public String toString() {
		return "StorageArea [name=" + name + ", root=" + FSRoot + ", stfnRoot=" + stfnRoot + ", protocol=" + getProtocols() + ", httpPerms=" + httpPerms.name() + "]";
	}

	/**
	 * @return if the protocol is accepted as transfer protocol
	 */
	public boolean isProtocol(String protocol) {
		return getProtocols().contains(protocol);
	}
	
	/**
	 * @param path "virtual" path to a storage area resource
	 * @return the phisical path to the resource
	 */
	public String getRealPath(String path) {
		return path.replaceFirst(getStfnRoot(), getFSRoot()).replace("//", "/");
	}

	public String getStfn(String fsPath) {
		return fsPath.replaceFirst(getFSRoot(), getStfnRoot()).replace("//", "/");
	}

	public boolean isOwner(String stfn) {
		return isOwner(new File(getRealPath(stfn)));
	}
	
	public boolean isOwner(File file) {

		log.debug("SA root is {}, file absolute path is {}",
			file.getAbsolutePath(), getFSRoot());
		return file.getAbsolutePath().startsWith(getFSRoot());
	}
	
	public boolean isHTTPReadable() {
		return !this.httpPerms.equals(HttpPerms.NOREAD);
	}
	
	public boolean isHTTPWritable() {
		return this.httpPerms.equals(HttpPerms.READWRITE);
	}
	
}
