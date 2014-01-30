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
package it.grid.storm.gridhttps.webapp.common.factory;

import io.milton.common.ContentTypeUtils;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.common.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormFileResource extends StormResource {

	private static final Logger log = LoggerFactory.getLogger(StormFileResource.class);
	
	public StormFileResource(StormFactory factory, StorageArea storageArea, File file) {
		super(factory, storageArea, file);
	}
	
	public StormFileResource(StormDirectoryResource parentDir, String childFileName) {
		this(parentDir.getFactory(), parentDir.getStorageArea(), new File(parentDir.getFile(), childFileName));
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

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.getInstance().doDelete(this);
	}

	public void moveTo(StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.getInstance().doMove(this, newParent, newName);
		setFile(new File(newParent.getFile(), newName));
	}

	public void copyTo(StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.getInstance().doCopyFile(this, newParent, newName);
	}

	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {		
		StormResourceHelper.getInstance().doPutOverwrite(this, in);
	}

	@Override
	public SurlInfo getSurlInfo() throws RuntimeApiException, SRMOperationException {
		return StormResourceHelper.getInstance().doLimitedLsDetailed(this.getSurl()).getInfos().iterator().next();
	}

}
