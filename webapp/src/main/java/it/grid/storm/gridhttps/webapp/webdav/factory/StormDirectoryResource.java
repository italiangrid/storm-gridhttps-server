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

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlFolderPage;
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

	public StormDirectoryResource(StormResourceFactory factory, File dir, StorageArea storageArea) {
		super(factory.getLocalhostname(), factory, dir, storageArea);
	}

	public StormDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea());
	}

	public CollectionResource createCollection(String name) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for MKCOL DIRECTORY");
		String methodName = MiltonServlet.request().getMethod();
		if (methodName.equals("PUT")) {
			/*
			 * it is a PUT with a path that contains a directory that does not
			 * exist, so send a 409 error to the client method can't be COPY
			 * because error 409 is handled by CopyHandler MOVE? Check if it can
			 * be a problem!!!
			 */
			log.warn("Auto-creation of directory for " + methodName + " requests is disabled!");
			throw new ConflictException(this, "A resource cannot be created at the destination URI until one or more intermediate collections are created.");
		}
		return StormResourceHelper.doMkCol(this, name);
	}

	/* works like a find child : return null if not exists */
	public Resource child(String name) {
		File fsDest = new File(this.getFile(), name);
		StormResource childResource = this.getFactory().resolveFile(this.getHost(), fsDest, this.getStorageArea());
		if (childResource == null) {
			SurlInfo detail;
			try {
				detail = StormResourceHelper.doLs(getFactory(), fsDest).get(0);
				if (detail.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) && detail.getStatus().getStatusCode().equals(TStatusCode.SRM_FAILURE)) {
					return getFactory().resolveFile(detail);
				} else {
					log.warn(detail.getStfn() + " status is " + detail.getStatus().getStatusCode().getValue());
				}
			} catch (RuntimeApiException e) {
				log.error(e.getMessage() + ": " + e.getReason());
			} catch (StormResourceException e) {
				log.error(e.getMessage() + ": " + e.getReason());
			}
		}
		return childResource;
		
//		List<? extends Resource> children = getChildren();
//		for (Resource r : children) {
//			if (((StormResource) r).getFile().getName().equals(name)) {
//				return r;
//			}
//		}
//		return null;
		// File fchild = new File(getFile(), name);
		// return getFactory().resolveFile(getHost(), fchild, getStorageArea());
	}

	public List<? extends Resource> getChildren() {
		ArrayList<StormResource> list = new ArrayList<StormResource>();
		try {
			Collection<SurlInfo> children = StormResourceHelper.doLsDetailed(this, Recursion.NONE).get(0).getSubpathInfo();
			for (SurlInfo entry : children) {
				if (entry.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) && entry.getStatus().getStatusCode().equals(TStatusCode.SRM_FAILURE)) {
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
			log.error(e.getMessage());
		} catch (StormResourceException e) {
			log.error(e.getMessage());
		}
		return list;
	}

	/**
	 * Will redirect if a default page has been specified on the factory
	 * 
	 * @param request
	 * @return
	 */
	public String checkRedirect(Request request) {
		if (getFactory().getDefaultPage() != null) {
			return request.getAbsoluteUrl() + "/" + getFactory().getDefaultPage();
		} else {
			return null;
		}
	}

	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		log.debug("Called function for PUT FILE");
		return StormResourceHelper.doPut(this, name, in);
	}

	/**
	 * Will generate a listing of the contents of this directory, unless the
	 * factory's allowDirectoryBrowsing has been set to false.
	 * 
	 * If so it will just output a message saying that access has been disabled.
	 * 
	 * @param out
	 * @param range
	 * @param params
	 * @param contentType
	 * @throws IOException
	 * @throws NotAuthorizedException
	 * @throws StormResourceException
	 * @throws RuntimeApiException
	 */
	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, RuntimeApiException, StormResourceException {
		log.debug("Called function for GET DIRECTORY");
		Collection<SurlInfo> entries = StormResourceHelper.doLsDetailed(this, Recursion.NONE).get(0).getSubpathInfo();
		buildDirectoryPage(out, entries);
	}

	private void buildDirectoryPage(OutputStream out, Collection<SurlInfo> entries) throws RuntimeApiException, StormResourceException {
		String dirPath = MiltonServlet.request().getRequestURI();
		StormHtmlFolderPage page = new StormHtmlFolderPage(out);
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		page.addNavigator(getStorageArea().getStfn(getFile().getPath()));
		page.addFolderList(dirPath, entries);
		page.end();
	}

	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}

	public String getContentType(String accepts) {
		return "text/html";
	}

	public Long getContentLength() {
		return null;
	}

	// public static String insertSsoPrefix(String abUrl, String prefix) {
	// // need to insert the ssoPrefix immediately after the host and port
	// int pos = abUrl.indexOf("/", 8);
	// String s = abUrl.substring(0, pos) + "/" + prefix;
	// s += abUrl.substring(pos);
	// return s;
	// }

	public boolean hasChildren() {
		try {
			return !StormResourceHelper.doLs(this).get(0).getSubpathInfo().isEmpty();
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
			return false;
		} catch (StormResourceException e) {
			log.error(e.getMessage());
			return false;
		}
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for DELETE DIRECTORY");
		StormResourceHelper.doDelete(this);
	}

	public void moveTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for MOVE DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormResourceHelper.doMoveTo(this, (StormDirectoryResource) newParent, newName);
			setFile(((StormDirectoryResource) newParent).getFile());
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for COPY DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormResourceHelper.doCopyDirectory(this, (StormDirectoryResource) newParent, newName);
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

}