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

import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.*;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormFileResource extends StormResource {

	private static final Logger log = LoggerFactory.getLogger(StormFileResource.class);

	private SurlInfo surlInfo;
	
	public StormFileResource(StormFactory factory, File file, StorageArea storageArea) {
		super(factory.getLocalhostname(), factory, file, storageArea);
	}
	
	private void loadSurlInfo() {
		ArrayList<SurlInfo> info = null;
		try {
			info = StormResourceHelper.doLsDetailed(this, Recursion.NONE);
		} catch (RuntimeApiException e) {
			log.error("Error retrieving surl-info for " + this.getFile());
			log.error(e.getMessage() + ": " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.error("Error retrieving surl-info for " + this.getFile());
			log.error(e.getMessage() + ": " + e.getReason());
		}
		setSurlInfo(info != null ? info.get(0) : null);
	}
	
	public String getCheckSumType() {
		String checksumType = "";
		if (getSurlInfo() == null) {
			loadSurlInfo();
			if (getSurlInfo() != null) {
				checksumType = getSurlInfo().getCheckSumType() != null ? getSurlInfo().getCheckSumType().getValue() : "";
			}
		}
		return checksumType;
	}
	
	public String getCheckSumValue() {
		String checksumValue = "";
		if (getSurlInfo() == null) {
			loadSurlInfo();
			if (getSurlInfo() != null) {
				checksumValue = getSurlInfo().getCheckSumValue() != null ? getSurlInfo().getCheckSumValue().getValue() : "";
			}
		}
		return checksumValue;
	}
	
	public String getStatus() {
		String status = "";
		if (getSurlInfo() == null) {
			loadSurlInfo();
			if (getSurlInfo() != null) {
				status = getSurlInfo().getStatus().toString();
			}
		}
		return status;	
	}

	public Long getContentLength() {
		return getFile().length();
	}

	public String getContentType(String preferredList) {
		String mime = ContentTypeUtils.findContentTypes(getFile());
		String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
		if (log.isTraceEnabled()) {
			log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[] { preferredList, mime, s });
		}
		return s;
	}

	public String checkRedirect(Request arg0) {
		return null;
	}

	/**
	 * @{@inheritDoc
	 */
	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.doDelete(this);
	}

	public void moveTo(StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.doMoveTo(this, newParent, newName);
		setFile(new File(newParent.getFile(), newName));
	}

	public void copyTo(StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.doCopyFile(this, newParent, newName);
	}

	private SurlInfo getSurlInfo() {
		return surlInfo;
	}
	
	private void setSurlInfo(SurlInfo surlInfo) {
		this.surlInfo = surlInfo;
	}
	
	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {		
		StormResourceHelper.doPutOverwrite(this, in);
	}

}
