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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.Resource;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.backendApi.StormBackendApi;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);

	/* STORM METHOD */

	private static void abortRequest(BackendApi backend, TRequestToken token, UserCredentials user) throws RuntimeApiException {
		log.debug("Aborting srm request...");
		StormBackendApi.abortRequest(backend, token, user);
	}

	public static void doMoveTo(StormResource source, StormResource newParent, String newName) throws NotAuthorizedException,
			ConflictException, BadRequestException {
		UserCredentials user = UserCredentials.getUser();
		doMoveTo(source, newParent, newName, user);
	}

	public static void doMoveTo(StormResource source, StormResource newParent, String newName, UserCredentials user)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called doMoveTo()");
		String fromSurl = source.getSurl().asString();
		String toSurl = (new Surl(newParent.getSurl(), newName)).asString();
		StormBackendApi.mv(source.getFactory().getBackendApi(), fromSurl, toSurl, user);
	}

	public static void doDelete(StormResource source) throws NotAuthorizedException, ConflictException, BadRequestException {
		UserCredentials user = UserCredentials.getUser();
		doDelete(source, user);
	}

	public static void doDelete(StormResource source, UserCredentials user) throws NotAuthorizedException, ConflictException,
			BadRequestException {
		log.debug("Called doDelete()");
		if (source instanceof StormDirectoryResource) { // DIRECTORY
			StormDirectoryResource sourceDir = (StormDirectoryResource) source;
			if (sourceDir.hasChildren()) {
				StormBackendApi.rmdirRecoursively(sourceDir.getFactory().getBackendApi(), sourceDir.getSurl().asString(), user);
			} else {
				StormBackendApi.rmdir(sourceDir.getFactory().getBackendApi(), sourceDir.getSurl().asString(), user);
			}
		} else { // FILE
			StormFileResource sourceFile = (StormFileResource) source;
			StormBackendApi.rm(sourceFile.getFactory().getBackendApi(), sourceFile.getSurl().asString(), user);
		}
	}

	public static InputStream doGetFile(StormFileResource source) throws NotFoundException, RuntimeApiException, StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(HttpHelper.getHelper().getRequestProtocol());
		return doGetFile(source, user, transferProtocols);
	}

	public static InputStream doGetFile(StormFileResource source, UserCredentials user, ArrayList<String> transferProtocols)
			throws NotFoundException, RuntimeApiException, StormResourceException {
		log.debug("Called doGetFile()");
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				transferProtocols);
		InputStream in = null;
		try {
			in = source.getFactory().getContentService().getFileContent(source.getFile());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		StormBackendApi.releaseFile(source.getFactory().getBackendApi(), source.getSurl().asString(), outputPtG.getToken(), user);
		return in;
	}

	public static StormDirectoryResource doMkCol(StormDirectoryResource sourceDir, String name) throws RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doMkCol(sourceDir, name, user);
	}

	public static StormDirectoryResource doMkCol(StormDirectoryResource sourceDir, String name, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.debug("Called doMkCol()");
		StormDirectoryResource newDir = new StormDirectoryResource(sourceDir, name);
		StormBackendApi.mkdir(sourceDir.getFactory().getBackendApi(), newDir.getSurl().asString(), user);
		return newDir;
	}

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in) throws RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(HttpHelper.getHelper().getRequestProtocol());
		return doPut(sourceDir, name, in, user, transferProtocols);
	}

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in, UserCredentials user, ArrayList<String> transferProtocols)
			throws RuntimeApiException, StormResourceException {
		log.debug("Called doPut()");
		File fsDest = new File(sourceDir.getFile(), name);
		StormFileResource srmDest = new StormFileResource(sourceDir.getFactory(), fsDest, sourceDir.getStorageArea());
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPut(sourceDir.getFactory().getBackendApi(), srmDest.getSurl()
				.asString(), user, transferProtocols);
		// put
		try {
			sourceDir.getFactory().getContentService().setFileContent(fsDest, in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("FileNotFoundException!", e);
		} catch (IOException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("IOException!", e);
		}
		StormBackendApi.putDone(sourceDir.getFactory().getBackendApi(), srmDest.getSurl().asString(), outputPtp.getToken(), user);
		return srmDest;
	}

	public static StormFileResource doPutOverwrite(StormFileResource source, InputStream in) throws BadRequestException, ConflictException,
			NotAuthorizedException {
		UserCredentials user = UserCredentials.getUser();
		return doPutOverwrite(source, in, user);
	}

	public static StormFileResource doPutOverwrite(StormFileResource source, InputStream in, UserCredentials user)
			throws BadRequestException, ConflictException, NotAuthorizedException {
		log.debug("Called doPutOverwrite()");
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(HttpHelper.getHelper().getRequestProtocol());
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPutOverwrite(source.getFactory().getBackendApi(), source.getSurl()
				.asString(), user, transferProtocols);
		// overwrite
		try {
			source.getFactory().getContentService().setFileContent(source.getFile(), in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			abortRequest(source.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("Couldnt write to: " + source.getFile().getAbsolutePath(), ex);
		}
		StormBackendApi.putDone(source.getFactory().getBackendApi(), source.getSurl().asString(), outputPtp.getToken(), user);
		return source;
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion) throws RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doLsDetailed(source, recursion, user);
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.debug("Called doLsDetailed()");
		LsOutputData output = StormBackendApi.lsDetailed(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				new RecursionLevel(recursion));
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static ArrayList<SurlInfo> doLs(StormResource source) throws RuntimeApiException, StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doLs(source, user);
	}

	public static ArrayList<SurlInfo> doLs(StormResource source, UserCredentials user) throws RuntimeApiException, StormResourceException {
		log.debug("Called doLs()");
		LsOutputData output = StormBackendApi.ls(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort) throws RuntimeApiException {
		UserCredentials user = UserCredentials.getUser();
		return doPing(stormBackendHostname, stormBackendPort, user);
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort, UserCredentials user) throws RuntimeApiException {
		log.debug("Called doPing()");
		BackendApi backend = StormBackendApi.getBackend(stormBackendHostname, stormBackendPort);
		return StormBackendApi.ping(backend, user);
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		UserCredentials user = UserCredentials.getUser();
		doCopyDirectory(sourceDir, newParent, newName, HttpHelper.getHelper().isDepthInfinity(), user);
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName,
			boolean isDepthInfinity, UserCredentials user) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.debug("Called doCopyDirectory()");
		// create destination folder:
		StormDirectoryResource destinationResource = doMkCol(newParent, newName, user);
		// COPY every resource from the source to the destination folder:
		for (Resource r : sourceDir.getChildren()) {
			if (r instanceof StormFileResource) {
				// is a file
				doCopyFile((StormFileResource) r, destinationResource, ((StormFileResource) r).getName(), user);
			} else if (r instanceof StormDirectoryResource) {
				// is a directory
				if (isDepthInfinity) {
					// recursion on
					doCopyDirectory((StormDirectoryResource) r, destinationResource, ((StormDirectoryResource) r).getName(),
							isDepthInfinity, user);
				} else {
					// recursion off
					doMkCol(destinationResource, ((StormDirectoryResource) r).getName(), user);
				}
			}
		}
	}

	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName) throws RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		doCopyFile(source, newParent, newName, user);
	}

	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.debug("Called doCopyFile()");
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(HttpHelper.getHelper().getRequestProtocol());
		/* prepareToGet on source file to lock the resource */
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				transferProtocols);
		try {
			/* create destination */
			transferProtocols.clear();
			transferProtocols.add(HttpHelper.getHelper().getDestinationProtocol());
			StormResourceHelper.doPut(newParent, newName, source.getInputStream(), user, transferProtocols);
			/* release source resource */
			StormBackendApi.releaseFile(source.getFactory().getBackendApi(), source.getSurl().asString(), outputPtG.getToken(), user);
		} catch (RuntimeException e) {
			StormBackendApi.abortRequest(source.getFactory().getBackendApi(), outputPtG.getToken(), user);
			throw e;
		} catch (RuntimeApiException e) {
			StormBackendApi.abortRequest(source.getFactory().getBackendApi(), outputPtG.getToken(), user);
			throw e;
		} catch (StormResourceException e) {
			StormBackendApi.abortRequest(source.getFactory().getBackendApi(), outputPtG.getToken(), user);
			throw e;
		}
	}

	public static RequestOutputData doPrepareToGetStatus(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doPrepareToGetStatus(source, user);
	}

	public static RequestOutputData doPrepareToGetStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormResourceException {
		log.debug("Called doPrepareToGetStatus()");
		return StormBackendApi.prepareToGetStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doPrepareToPutStatus(source, user);
	}

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormResourceException {
		log.debug("Called doPrepareToPutStatus()");
		return StormBackendApi.prepareToPutStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}

}