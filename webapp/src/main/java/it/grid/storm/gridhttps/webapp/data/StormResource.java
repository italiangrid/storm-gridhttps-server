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

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StormResource implements Resource, DigestResource {

	private static final Logger log = LoggerFactory.getLogger(StormResource.class);
	private File file;
	private StormFactory factory;
	private String host;
	private Surl surl;
	private StorageArea storageArea;
	private SurlInfo surlInfo;

	public StormResource(String host, StormFactory factory, File file, StorageArea storageArea) {
		setHost(host);
		setFile(file);
		setFactory(factory);
		setStorageArea(storageArea);
		setSurl(new Surl(getFile(), getStorageArea()));
		setSurlInfo(null);
	}

	public StormResource(String host, StormFactory factory, File file, StorageArea storageArea, SurlInfo surlInfo) {
		this(host, factory, file, storageArea);
		setSurlInfo(surlInfo);
	}

	protected void setHost(String host) {
		this.host = host;
	}

	protected void setFactory(StormFactory factory) {
		this.factory = factory;
	}

	protected void setSurl(Surl surl) {
		this.surl = surl;
	}

	protected void setStorageArea(StorageArea storageArea) {
		this.storageArea = storageArea;
	}

	protected void setFile(File newFile) {
		this.file = newFile;
	}

	public String getHost() {
		return host;
	}

	public File getFile() {
		return file;
	}

	public StormFactory getFactory() {
		return factory;
	}

	public String getUniqueId() {
		String s = file.lastModified() + "_" + file.length() + "_" + file.getAbsolutePath();
		return s.hashCode() + "";
	}

	public String getName() {
		return file.getName();
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

	public Date getModifiedDate() {
		return new Date(file.lastModified());
	}

	public Date getCreateDate() {
		return null;
	}

	public int compareTo(Resource o) {
		return this.getName().compareTo(o.getName());
	}

	public Surl getSurl() {
		return surl;
	}

	public ArrayList<String> getSurlAsList() {
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(getSurl().asString());
		return surls;
	}

	public InputStream getInputStream() {
		InputStream in;
		try {
			in = new FileInputStream(getFile());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			return null;
		}
		return in;
	}

	public StorageArea getStorageArea() {
		return storageArea;
	}

	public SurlInfo getSurlInfo() {
		if (surlInfo == null) {
			setSurlInfo(loadSurlInfo());
		}
		return surlInfo;
	}

	public void setSurlInfo(SurlInfo surlInfo) {
		this.surlInfo = surlInfo;
	}

	protected SurlInfo loadSurlInfo() {
		ArrayList<SurlInfo> info = null;
		try {
			info = StormResourceHelper.doLsDetailed(this, Recursion.NONE);
		} catch (RuntimeApiException e) {
			log.error("Retrieving surl-info for " + getFile() + ": " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.error("Retrieving surl-info for " + getFile() + ": " + e.getReason());
		}
		return info != null ? info.get(0) : null;
	}

	public String getCheckSumType() {
		SurlInfo info = getSurlInfo();
		String checksumType = "";
		if (info != null) {
			checksumType = info.getCheckSumType() != null ? info.getCheckSumType().getValue() : "";
		}
		return checksumType;
	}

	public String getCheckSumValue() {
		SurlInfo info = getSurlInfo();
		String checksumValue = "";
		if (info != null) {
			checksumValue = info.getCheckSumValue() != null ? info.getCheckSumValue().getValue() : "";
		}
		return checksumValue;
	}

	public String getStatus() {
		SurlInfo info = getSurlInfo();
		String status = "";
		if (info != null) {
			status = info.getStatus().toString();
		}
		return status;
	}

}