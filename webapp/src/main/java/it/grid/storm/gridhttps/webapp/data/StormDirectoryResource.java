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

import io.milton.http.Auth;
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
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
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

	public StormDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea());
	}
	
	@Override
	public CollectionResource createCollection(String name) throws RuntimeApiException, StormRequestFailureException, NotAuthorizedException, ConflictException, BadRequestException {
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
		try {
			Collection<SurlInfo> children = StormResourceHelper.doLsDetailed(this, Recursion.NONE).get(0).getSubpathInfo();
			for (SurlInfo entry : children) {
				if (!(entry.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) && entry.getStatus().getStatusCode().equals(TStatusCode.SRM_FAILURE))) {
					StormResource resource = getFactory().resolveFile(entry);
					if (resource != null) {
						list.add(resource);
					} else {
						log.error("Couldnt resolve file {}", entry.getStfn());
					}
				} else {
					log.warn(entry.getStfn() + " status is " + entry.getStatus().getStatusCode().getValue());
				}
			}
		} catch (RuntimeApiException e) {
			log.error("retrieving children for '" + this.getFile() + "': " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.error("retrieving children for '" + this.getFile() + "': " + e.getReason());
		}
		return list;
	}

	@Override
	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		return StormResourceHelper.doPut(this, name, in);
	}

	public boolean hasChildren() {
		boolean hasC = false;
		try {
			hasC = !StormResourceHelper.doLs(this).get(0).getSubpathInfo().isEmpty();
		} catch (RuntimeApiException e) {
			log.error("checking if '" + this.getFile() + "' has children: " + e.getReason());
		} catch (StormRequestFailureException e) {
			log.error("checking if '" + this.getFile() + "' has children: " + e.getReason());
		}
		return hasC;
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
	public String getContentType(String accepts) {
		return null;
	}

	@Override
	public Long getContentLength() {
		return null;
	}

	@Override
	public Long getMaxAgeSeconds(Auth auth) {
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

}