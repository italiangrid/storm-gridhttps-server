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
package it.grid.storm.gridhttps.webapp.common;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StormResource implements Resource, DigestResource, PropFindableResource {

	private static final Logger log = LoggerFactory.getLogger(StormResource.class);

	private File file;
	private StormFactory factory;
	private String host;
	private StorageArea storageArea;

	public StormResource(String host, StormFactory factory, StorageArea storageArea, File file) {
		this.setHost(host);
		this.setFactory(factory);
		this.setFile(file);
		this.setStorageArea(storageArea);
	}
	
	private void setHost(String host) {
		this.host = host;
	}

	private void setFactory(StormFactory factory) {
		this.factory = factory;
	}

	protected void setFile(File file) {
		this.file = file;
	}
	
	private void setStorageArea(StorageArea storageArea) {
		this.storageArea = storageArea;
	}

	public String getHost() {
		return host;
	}

	public File getFile() {
		return this.file;
	}

	public StorageArea getStorageArea() {
		return this.storageArea;
	}
	
	public StormFactory getFactory() {
		return factory;
	}

	public String getUniqueId() {
		String id = this.getFile().toString() + "_" + this.getSurl().asString();
		return id.hashCode() + "";
	}

	public String getName() {
		return this.getFile().getName();
	}

	/*
	 * If you want the resource to be cached return the number of seconds to
	 * cache it for, otherwise return null to disable caching(non-Javadoc)
	 * 
	 * @see
	 * io.milton.resource.GetableResource#getMaxAgeSeconds(io.milton.http.Auth)
	 */
	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}

	public Object authenticate(String user, String password) {
		return factory.getSecurityManager().authenticate(user, password);
	}

	public Object authenticate(DigestResponse digestRequest) {
		return factory.getSecurityManager().authenticate(digestRequest);
	}

	public boolean isDigestAllowed() {
		return factory.isDigestAllowed();
	}

	public boolean authorise(Request request, Method method, Auth auth) {
		boolean b = factory.getSecurityManager().authorise(request, method, auth, this);
		if (log.isTraceEnabled()) {
			log.trace("authorise: result=" + b);
		}
		return b;
	}

	public String getRealm() {
		return factory.getRealm(this.host);
	}

	public int compareTo(Resource o) {
		return this.getName().compareTo(o.getName());
	}

	public Surl getSurl() {
		return new Surl(this.getFile());
	}

	public InputStream getInputStream() throws FileNotFoundException {
		return new FileInputStream(this.getFile());
	}

	public abstract SurlInfo getSurlInfo() throws RuntimeApiException, SRMOperationException;
	
	public Date getModifiedDate() {
		return new Date(this.getFile().lastModified());
	}

	public Date getCreateDate() {
		return new Date(this.getFile().lastModified());
	}

}