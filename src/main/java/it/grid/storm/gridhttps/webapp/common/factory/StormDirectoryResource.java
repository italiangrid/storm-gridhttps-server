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

import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.CollectionResource;
import io.milton.resource.CopyableResource;
import io.milton.resource.DeletableCollectionResource;
import io.milton.resource.MakeCollectionableResource;
import io.milton.resource.MoveableResource;
import io.milton.resource.PropFindableResource;
import io.milton.resource.PutableResource;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.common.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormDirectoryResource extends StormResource implements
	MakeCollectionableResource, PutableResource, CopyableResource,
	MoveableResource, PropFindableResource, DeletableCollectionResource {

	private static final Logger log = LoggerFactory.getLogger(StormDirectoryResource.class);

	public StormDirectoryResource(StormFactory factory, StorageArea storageArea, File dir) {
		super(factory.getLocalhostname(), factory, storageArea, dir);
	}

	public StormDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), parentDir.getStorageArea(), new File(parentDir.getFile(), childDirName));
	}

	@Override
	public CollectionResource createCollection(String name) throws RuntimeApiException, SRMOperationException,
			NotAuthorizedException, ConflictException, BadRequestException {
		return StormResourceHelper.getInstance().doMkCol(this, name);
	}

	@Override
	public Resource child(String name) throws NotAuthorizedException, BadRequestException {
		File son = new File(this.getFile(), name);
		if (!son.exists()) {
			log.debug("Child {} doesn't exist", son);
			return null;
		}
		if (son.isDirectory()) {
			return getFactory().getDirectoryResource(getStorageArea(), son);
		}
		return getFactory().getFileResource(getStorageArea(), son);
	}

	@Override
	public List<? extends Resource> getChildren() throws NotAuthorizedException, BadRequestException {
		
		List<StormResource> list = new ArrayList<StormResource>();
		File[] children = getFile().listFiles();
		
		for (File file : children) {
			StormResource res = (StormResource) child(file.getName());
			if (res != null)
				list.add(res);
		}
		
		return list;	
	}

	@Override
	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		return StormResourceHelper.getInstance().doPut(this, name, in);
	}

	@Override
	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		StormResourceHelper.getInstance().doDelete(this);
	}

	@Override
	public String checkRedirect(Request request) throws NotAuthorizedException, BadRequestException {
		return null;
	}

	@Override
	public void moveTo(CollectionResource newParent, String newName)
		throws ConflictException, NotAuthorizedException, BadRequestException {
		
		if (!(newParent instanceof StormDirectoryResource)) {
			String msg = String.format("Directory Resource class %s not supported!",
				newParent.getClass().getSimpleName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		StormResourceHelper.getInstance().doMove(this,
			(StormDirectoryResource) newParent, newName);
		setFile(((StormDirectoryResource) newParent).getFile());
	}

	@Override
	public void copyTo(CollectionResource newParent, String newName)
		throws NotAuthorizedException, BadRequestException, ConflictException {

		if (!(newParent instanceof StormDirectoryResource)) {
			String msg = String.format("Directory Resource class %s not supported!",
				newParent.getClass().getSimpleName());
			log.error(msg);
			throw new RuntimeException(msg);
		}
		
		StormResourceHelper.getInstance().doCopyDirectory(this,
			(StormDirectoryResource) newParent, newName);
	}

	@Override
	public boolean isLockedOutRecursive(Request request) {
		return false;
	}

	@Override
	public SurlInfo getSurlInfo() throws RuntimeApiException, SRMOperationException {

		return StormResourceHelper.getInstance().doLsDetailed(this.getSurl())
			.getInfos().iterator().next();
	}

	public ArrayList<SurlInfo> getChildrenSurlInfo() throws RuntimeApiException,
		SRMOperationException {

		return StormResourceHelper.getInstance().filterLs(
			StormResourceHelper.getInstance().doLsDetailed(this, 
				new RecursionLevel(Recursion.NONE)).getInfos().iterator().next()
					.getSubpathInfo());
	}

	public ArrayList<SurlInfo> getNChildrenSurlInfo(int numberOfChildren)
		throws RuntimeApiException, SRMOperationException {

		return StormResourceHelper.getInstance().filterLs(
			StormResourceHelper.getInstance().doLsDetailed(this, 
				new RecursionLevel(Recursion.NONE), numberOfChildren).getInfos().
					iterator().next().getSubpathInfo());
	}

}