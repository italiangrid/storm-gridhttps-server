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
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.common.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.factory.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlFolderPage;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebdavDirectoryResource extends StormDirectoryResource implements MakeCollectionableResource, PutableResource,
		CopyableResource, DeletableResource, MoveableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(WebdavDirectoryResource.class);

	public WebdavDirectoryResource(StormFactory factory, StorageArea storageArea, File dir) {
		super(factory, storageArea, dir);
	}

	public WebdavDirectoryResource(StormDirectoryResource parentDir, String childDirName) {
		this(parentDir.getFactory(), parentDir.getStorageArea(), new File(parentDir.getFile(), childDirName));
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
		ArrayList<SurlInfo> entries = null;
		int numberOfMaxEntries = 0;
		try {
			entries = this.getChildrenSurlInfo();
		} catch (SRMOperationException e) {
			if (e.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
				TReturnStatus status = e.getStatus();
				String[] array = status.getExplanation().split(" ");
				String numberOfMaxEntriesString = array[array.length - 1]; // last element
				try {
					numberOfMaxEntries = Integer.valueOf(numberOfMaxEntriesString);
				} catch (NumberFormatException e2) {
					log.error("Error parsing explanation string to retrieve the max number of entries of a srmLs -l, "
						+ numberOfMaxEntriesString + " is not a valid integer!");
					throw new RuntimeException("Error parsing explanation string to retrieve the max number of entries of a srmLs -l, "
						+ numberOfMaxEntriesString + " is not a valid integer!");
				}
				log.warn("Too many results with Ls, max entries is " + numberOfMaxEntries + ". Re-trying with counted Ls.");
				entries = this.getNChildrenSurlInfo(numberOfMaxEntries);
			} else
				throw e;
		}
		if (entries != null)
			buildDirectoryPage(out, entries, numberOfMaxEntries);
	}

	private void buildDirectoryPage(OutputStream out, ArrayList<SurlInfo> entries, int nmax) {
		String dirPath = MiltonServlet.request().getRequestURI();
		StormHtmlFolderPage page = new StormHtmlFolderPage(this, out);
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		String webdavContext = File.separator
			+ Configuration.getGridhttpsInfo().getWebdavContextPath();
		page.addNavigator(dirPath.replaceFirst(webdavContext, ""));
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