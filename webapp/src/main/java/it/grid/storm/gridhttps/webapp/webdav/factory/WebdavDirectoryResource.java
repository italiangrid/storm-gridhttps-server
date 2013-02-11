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

import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.data.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.gridhttps.webapp.data.StormResource;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormResourceException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlFolderPage;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebdavDirectoryResource extends StormDirectoryResource implements MakeCollectionableResource, PutableResource,
		CopyableResource, DeletableResource, MoveableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(WebdavDirectoryResource.class);

	public WebdavDirectoryResource(StormFactory factory, File dir, StorageArea storageArea) {
		super(factory, dir, storageArea);
	}

	public WebdavDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea());
	}

	public WebdavDirectoryResource(StormFactory factory, File dir, StorageArea storageArea, SurlInfo surlInfo) {
		super(factory, dir, storageArea, surlInfo);
	}

	public WebdavDirectoryResource(StormDirectoryResource parentDir, String childDirName, SurlInfo surlInfo) {
		this(parentDir.getFactory(), new File(parentDir.getFile(), childDirName), parentDir.getStorageArea(), surlInfo);
	}

	public CollectionResource createCollection(String name) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for MKCOL DIRECTORY");
		String methodName = MiltonServlet.request().getMethod();
		if (methodName.equals("PUT")) {
			/*
			 * it is a PUT with a path that contains a directory that does not
			 * exist, so send a 409 error to the client method
			 */
			log.warn(MiltonServlet.request().getRequestURI() + " path contains one or more intermediate collections that not exist!");
			throw new ConflictException(this,
					"A resource cannot be created at the destination URI until one or more intermediate collections are created.");
		}
		return super.createCollection(name);
	}

	/**
	 * Will redirect if a default page has been specified on the factory
	 * 
	 * @param request
	 * @return
	 */
	public String checkRedirect(Request request) {
		return null;
	}

	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		log.debug("Called function for PUT FILE");
		return super.createNew(name, in, length, contentType);
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
			NotAuthorizedException, BadRequestException {
		log.debug("Called function for GET DIRECTORY");
		Collection<SurlInfo> entries = null;
		int numberOfMaxEntries = 0;
		try {
			entries = this.getSurlInfo(StormResource.RECURSIVE_DETAILED).getSubpathInfo();
		} catch (TooManyResultsException e) {
			TReturnStatus status = e.getStatus();
			String[] array = status.getExplanation().split(" ");
			String numberOfMaxEntriesString = array[array.length - 1]; // last
																		// element
			try {
				numberOfMaxEntries = Integer.valueOf(numberOfMaxEntriesString);
			} catch (NumberFormatException e2) {
				log.error("Error parsing explanation string to retrieve the max number of entries of a srmLs -l, "
						+ numberOfMaxEntriesString + " is not a valid integer!");
				throw new RuntimeException("Error parsing explanation string to retrieve the max number of entries of a srmLs -l, "
						+ numberOfMaxEntriesString + " is not a valid integer!");
			}
			log.warn("Too many results with Ls, max entries is " + numberOfMaxEntries + ". Re-trying with counted Ls.");
			entries = StormResourceHelper.doLsDetailed(this, Recursion.LIMITED, numberOfMaxEntries);
		}
		if (entries != null)
			buildDirectoryPage(out, entries, numberOfMaxEntries);
	}

	private void buildDirectoryPage(OutputStream out, Collection<SurlInfo> entries, int nmax) {
		String dirPath = MiltonServlet.request().getRequestURI();
		StormHtmlFolderPage page = new StormHtmlFolderPage(out);
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		page.addNavigator(getStorageArea().getStfn(getFile().getPath()));
		if (nmax > 0)
			page.addTooManyResultsWarning(nmax);
		page.addFolderList(dirPath, entries);
		page.end();
	}

	@Override
	public String getContentType(String accepts) {
		return "text/html";
	}

	public Long getContentLength() {
		return null;
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for DELETE DIRECTORY");
		super.delete();
	}

	public void moveTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for MOVE DIRECTORY");
		super.moveTo(newParent, newName);
	}

	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called function for COPY DIRECTORY");
		super.copyTo(newParent, newName);
	}

}