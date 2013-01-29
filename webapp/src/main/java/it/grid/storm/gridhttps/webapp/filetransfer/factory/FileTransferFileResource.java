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
package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.common.ContentTypeUtils;
import io.milton.common.RangeUtils;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.StormFileResource;
import it.grid.storm.storagearea.StorageArea;

import java.io.*;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferFileResource extends StormFileResource implements GetableResource, PropFindableResource, ReplaceableResource {

	private static final Logger log = LoggerFactory.getLogger(FileTransferFileResource.class);

	public FileTransferFileResource(StormFactory factory, File file, StorageArea storageArea) {
		super(factory, file, storageArea);
	}

	public Long getContentLength() {
		return getFile().length();
	}

	public String getContentType(String preferredList) {
		String mime = ContentTypeUtils.findContentTypes(this.getFile());
		String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
		if (log.isTraceEnabled()) {
			log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[] { preferredList, mime, s });
		}
		return s;
	}
	
	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, BadRequestException, NotFoundException {
		log.debug("Called function for GET FILE");
		
		InputStream in = null;
		try {
			in = this.getFactory().getContentService().getFileContent(this.getFile());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		if (in == null) {
			log.error("Unable to get resource content '" + this.getFile().toString() + "'");
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
		try {
			// overwrite
			this.getFactory().getContentService().setFileContent(this.getFile(), in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			throw new RuntimeException("Couldnt write to: " + this.getFile().getAbsolutePath(), ex);
		} 
	}

}
