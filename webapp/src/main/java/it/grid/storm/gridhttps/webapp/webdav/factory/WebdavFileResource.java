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
package it.grid.storm.gridhttps.webapp.webdav.factory;

import io.milton.common.RangeUtils;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.data.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.StormFileResource;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.storagearea.StorageArea;

import java.io.*;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebdavFileResource extends StormFileResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource,
		PropFindableResource, ReplaceableResource {

	private static final Logger log = LoggerFactory.getLogger(WebdavFileResource.class);
	
	public WebdavFileResource(StormFactory factory, File file, StorageArea storageArea) {
		super(factory, file, storageArea);
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
		log.debug("Called function for GET FILE");
		InputStream in = StormResourceHelper.doGetFile(this);
		if (in == null) {
			log.error("Unable to get resource content '" + getFile().toString() + "'");
			return;
		}
		if (range != null) {
			log.debug("sendContent: ranged content: " + getFile().getAbsolutePath());
			RangeUtils.writeRange(in, range, out);
		} else {
			log.debug("sendContent: send whole file " + getFile().getAbsolutePath());
			IOUtils.copy(in, out);
		}
		out.flush();
		IOUtils.closeQuietly(in);
	}

	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.debug("Called function for PUT-OVERWRITE");
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		if (!httpHelper.isOverwriteRequest()) {
			throw new NotAuthorizedException("Resource exists but this is not an overwrite request!", this);
		}
		super.replaceContent(in, length);
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for DELETE FILE");
		StormResourceHelper.doDelete(this);
	}

	public void moveTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for MOVE FILE");
		if (newParent instanceof StormDirectoryResource) {
			super.moveTo((StormDirectoryResource) newParent, newName);
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for COPY FILE");		
		if (newParent instanceof StormDirectoryResource) {
			super.copyTo((StormDirectoryResource) newParent, newName);
		} else {
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
		}
	}
}
