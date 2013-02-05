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

import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.CollectionResource;
import io.milton.resource.CopyableResource;
import io.milton.resource.DeletableResource;
import io.milton.resource.GetableResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.MoveableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormDirectoryResource extends StormResource implements MakeCollectionableResource, PutableResource, CopyableResource,
		DeletableResource, MoveableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(StormDirectoryResource.class);

	public StormDirectoryResource(StormFactory factory, File dir, StorageArea storageArea) {
		super(factory.getLocalhostname(), factory, dir, storageArea);
	}

	public StormDirectoryResource(StormFactory factory, File dir, StorageArea storageArea, SurlInfo surlInfo) {
		super(factory.getLocalhostname(), factory, dir, storageArea, surlInfo);
	}

	public StormDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea());
	}

	public StormDirectoryResource(StormDirectoryResource parentDir, String childDirName, SurlInfo surlInfo) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea(), surlInfo);
	}

	@Override
	public CollectionResource createCollection(String name) throws RuntimeApiException, StormRequestFailureException,
			NotAuthorizedException, ConflictException, BadRequestException {
		return StormResourceHelper.doMkCol(this, name);
	}

	@Override
	public Resource child(String name) throws NotAuthorizedException, BadRequestException {
		File fsDest = new File(getFile(), name);
		return getFactory().resolveFile(this.getHost(), fsDest, getStorageArea());
	}

	@Override
	public List<? extends Resource> getChildren() {
		ArrayList<StormResource> list = new ArrayList<StormResource>();
		SurlInfo info = getSurlInfo();
		if (info != null) {
			for (SurlInfo entry : info.getSubpathInfo()) {
				StormResource resource = getFactory().resolveFile(entry, getStorageArea());
				if (resource != null) {
					list.add(resource);
				} else {
					log.warn("Couldn't add child '" + entry.getStfn() + "'!");
				}
			}
		}
		return list;
	}

	@Override
	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		return StormResourceHelper.doPut(this, name, in);
	}

	public boolean hasChildren() {
		ArrayList<SurlInfo> info = null;
		try {
			info = StormResourceHelper.doLs(this);
		} catch (RuntimeApiException e) {
			log.error("Retrieving surl-info for " + getFile() + ": " + e.getReason());
			throw new RuntimeException(e);
		} catch (StormRequestFailureException e) {
			log.error("Retrieving surl-info for " + getFile() + ": " + e.getReason());
			return false;
		} catch (TooManyResultsException e) {
			log.error("Retrieving surl-info for " + getFile() + ": " + e.getReason());
			throw new RuntimeException(e);	
		}
		return info != null ? !info.get(0).getSubpathInfo().isEmpty() : false;
	}

	@Override
	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.doDelete(this);
	}

	@Override
	public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
		return null;
	}

	@Override
	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, BadRequestException, NotFoundException {

	}

	@Override
	/*
	 * Called with a list of content types which are acceptable by the client,
	 * you should select the best one you support and return this. This will be
	 * given to sendContent
	 * 
	 * @see io.milton.resource.GetableResource#getContentType(java.lang.String)
	 */
	public String getContentType(String accepts) {
		return null;
	}

	@Override
	/*
	 * If you know the resource length return it, otherwise return null. If you
	 * return null the framework will either buffer the content to find the
	 * length, or send the content with a content length and drop the connection
	 * to indicate EOF, both of which have performance impacts - so its best to
	 * give a content length if you can
	 * 
	 * @see io.milton.resource.GetableResource#getContentLength()
	 */
	public Long getContentLength() {
		return null;
	}

	@Override
	public void moveTo(CollectionResource newParent, String newName) throws ConflictException, NotAuthorizedException, BadRequestException {
		if (newParent instanceof StormDirectoryResource) {
			StormResourceHelper.doMoveTo(this, (StormDirectoryResource) newParent, newName);
			setFile(((StormDirectoryResource) newParent).getFile());
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	@Override
	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, BadRequestException, ConflictException {
		if (newParent instanceof StormDirectoryResource) {
			StormResourceHelper.doCopyDirectory(this, (StormDirectoryResource) newParent, newName);
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	@Override
	public SurlInfo getSurlInfo() {
		return loadSurlInfo();
	}

}