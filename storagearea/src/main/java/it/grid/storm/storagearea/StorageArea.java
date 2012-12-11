/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2010.
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
package it.grid.storm.storagearea;

import java.io.File;

/**
 * @author Michele Dibenedetto
 */
public class StorageArea {

	private String name;
	private String FSRoot;
	private String stfnRoot;
	private int protocol;

	public final static int NONE_PROTOCOL = 0;
	public final static int HTTP_PROTOCOL = 1;
	public final static int HTTPS_PROTOCOL = 2;
	public final static int HTTP_AND_HTTPS_PROTOCOLS = 3;

	/**
	 * @param name
	 *            the name of the storage area
	 * @param FSRoot
	 *            the File System root of the storage area
	 * @param stfnRoot
	 *            the storage file name root of the storage area
	 */
	public StorageArea(String name, String FSRoot, String stfnRoot, int protocol) {
		this.name = name;
		this.FSRoot = normalizePath(FSRoot);
		this.stfnRoot = normalizePath(stfnRoot);
		this.protocol = protocol;
	}

	/**
	 * @return the name
	 */
	public final String getName() {
		return name;
	}

	/**
	 * @return the http protocol to use to access resources via webdav server
	 */
	public final int getProtocol() {
		return protocol;
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
	 * @return the String equivalent of the protocol
	 */
	public String getStrProtocol() {
		switch (protocol) {
		case 0:
			return "NONE_PROTOCOL";
		case 1:
			return "HTTP_PROTOCOL";
		case 2:
			return "HTTPS_PROTOCOL";
		case 3:
			return "HTTP_AND_HTTPS_PROTOCOLS";
		default:
			return "UNDEFINED";
		}
	}

	/**
	 * @return the String Array equivalent of the protocol
	 */
	public String[] getProtocolAsStrArray() {
		switch (protocol) {
		case 1:
			String[] out1 = { "HTTP" };
			return out1;
		case 2:
			String[] out2 = { "HTTPS" };
			return out2;
		case 3:
			String[] out3 = { "HTTP", "HTTPS" };
			return out3;
		case 0:
		default:
			String[] out0 = {};
			return out0;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "StorageArea [name=" + name + ", root=" + FSRoot + ", stfnRoot=" + stfnRoot + ", protocol=" + getStrProtocol() + "]";
	}
}
