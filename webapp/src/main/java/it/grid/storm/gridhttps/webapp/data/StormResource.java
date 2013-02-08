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
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.TCheckSumType;
import it.grid.storm.srm.types.TCheckSumValue;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TSizeInBytes;
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

public abstract class StormResource implements Resource, DigestResource, PropFindableResource {

	private static final Logger log = LoggerFactory.getLogger(StormResource.class);

	public static final int SINGLE_DETAILED = 0;
	public static final int RECURSIVE_UNDETAILED = 1;
	public static final int RECURSIVE_DETAILED = 2;

	private File file;
	private StormFactory factory;
	private String host;
	private Surl surl;
	private StorageArea storageArea;
	private TCheckSumType checkSumType;
	private TCheckSumValue checkSumValue;
	private TSizeInBytes size;
	private TReturnStatus status;
	private String stfn;
	private Date lastModified;
	private Date creationDate;
	private String name;

	public StormResource(String host, StormFactory factory, File file, StorageArea storageArea) {
		setName(file.getName());
		setHost(host);
		setFactory(factory);
		setFile(file);
		setStorageArea(storageArea);
		setSurl(new Surl(getFile(), getStorageArea()));
		importInfo(StormResource.loadSurlInfo(this, SINGLE_DETAILED));
	}

	public StormResource(String host, StormFactory factory, File file, StorageArea storageArea, SurlInfo info) {
		setName(file.getName());
		setHost(host);
		setFactory(factory);
		setFile(file);
		setStorageArea(storageArea);
		setSurl(new Surl(getFile(), getStorageArea()));
		importInfo(info);
	}

	public void importInfo(SurlInfo info) {
		if (info == null) {
			log.warn("StormResource failed to initialize surl-info attributes! info is null! Maybe " + this.getStfn() + " doesn't exist!");
			return;
		}
		setCheckSumType(info.getCheckSumType());
		setCheckSumValue(info.getCheckSumValue());
		setStatus(info.getStatus());
		setSize(info.getSize());
		setStfn(info.getStfn());
		setLastModified(info.getModificationTime());
		setCreationDate(info.getCreationTime());
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
		String id = getModifiedDate() + "_" + getSize() + "_" + getStfn();
		return id.hashCode() + "";
	}

	public String getName() {
		return name;
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
		return lastModified;
	}

	public Date getCreateDate() {
		return creationDate;
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

	public SurlInfo getSurlInfo(int depth) {
		return StormResource.loadSurlInfo(this, depth);
	}

	protected static SurlInfo loadSurlInfo(StormResource resource, int depth) {
		ArrayList<SurlInfo> info = null;
		try {
			switch (depth) {
			case SINGLE_DETAILED:
				info = StormResourceHelper.doLimitedLsDetailed(resource);
				break;
			case RECURSIVE_UNDETAILED:
				info = StormResourceHelper.doLs(resource);
				break;
			case RECURSIVE_DETAILED:
				info = StormResourceHelper.doLsDetailed(resource, Recursion.NONE);
				break;
			}
		} catch (RuntimeApiException e) {
			log.error("Retrieving surl-info for " + resource.getFile() + ": " + e.getReason());
			throw new RuntimeException(e);
		} catch (StormRequestFailureException e) {
			log.warn("Retrieving surl-info for " + resource.getFile() + ": " + e.getReason());
		} catch (TooManyResultsException e) {
			log.error("Retrieving surl-info for " + resource.getFile() + ": " + e.getReason());
			throw new RuntimeException(e);
		}
		return info != null ? info.get(0) : null;
	}

	public TCheckSumType getCheckSumType() {
		return checkSumType;
	}

	private void setCheckSumType(TCheckSumType checkSumType) {
		log.debug("set-checkSumType: " + checkSumType);
		this.checkSumType = checkSumType;
	}

	public TCheckSumValue getCheckSumValue() {
		return checkSumValue;
	}

	private void setCheckSumValue(TCheckSumValue checkSumValue) {
		log.debug("set-checkSumValue: " + checkSumValue);
		this.checkSumValue = checkSumValue;
	}

	public TSizeInBytes getSize() {
		return size;
	}

	private void setSize(TSizeInBytes size) {
		log.debug("set-size: " + size);
		this.size = size;
	}

	public TReturnStatus getStatus() {
		return status;
	}

	private void setStatus(TReturnStatus status) {
		log.debug("set-status: " + status);
		this.status = status;
	}

	public String getStfn() {
		return stfn;
	}

	private void setStfn(String stfn) {
		log.debug("set-stfn: " + stfn);
		this.stfn = stfn;
	}

	private void setLastModified(Date lastModified) {
		log.debug("set-lastModified: " + lastModified);
		this.lastModified = lastModified;
	}

	private void setCreationDate(Date creationDate) {
		log.debug("set-creationDate: " + creationDate);
		this.creationDate = creationDate;
	}

	private void setName(String name) {
		this.name = name;
	}

}