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
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.backendApi.StormBackendApi;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormResourceException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TStatusCode;
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

	/* utilities: */

	private static void abortRequest(BackendApi backend, TRequestToken token, UserCredentials user) throws RuntimeApiException {
		log.debug("Aborting srm request...");
		StormBackendApi.abortRequest(backend, token, user);
	}

	/* MOVE */

	public static void doMoveTo(StormResource source, StormResource newParent, String newName) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		doMoveTo(source, newParent, newName, httpHelper.getUser());
	}

	public static void doMoveTo(StormResource source, StormResource newParent, String newName, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("Called doMoveTo()");
		String fromSurl = source.getSurl().asString();
		String toSurl = (new Surl(newParent.getSurl(), newName)).asString();
		StormBackendApi.mv(source.getFactory().getBackendApi(), fromSurl, toSurl, user);
	}

	/* DELETE */

	public static void doDelete(StormResource source) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		doDelete(source, httpHelper.getUser());
	}

	public static void doDelete(StormResource source, UserCredentials user) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
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

	/* GET */

	public static InputStream doGetFile(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(httpHelper.getRequestProtocol());
		return doGetFile(source, httpHelper.getUser(), transferProtocols);
	}

	public static InputStream doGetFile(StormFileResource source, UserCredentials user, ArrayList<String> transferProtocols)
			throws NotFoundException, RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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

	/* MKCOL */

	public static StormDirectoryResource doMkCol(StormDirectoryResource sourceDir, String name) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doMkCol(sourceDir, name, httpHelper.getUser());
	}

	public static StormDirectoryResource doMkCol(StormDirectoryResource sourceDir, String name, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("Called doMkCol()");
		Surl newDir = new Surl(sourceDir.getSurl(), name);
		StormBackendApi.mkdir(sourceDir.getFactory().getBackendApi(), newDir.asString(), user);
		return new StormDirectoryResource(sourceDir, name);
	}

	/* PUT */

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in) throws RuntimeApiException,
			StormRequestFailureException, StormResourceException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(httpHelper.getRequestProtocol());
		return doPut(sourceDir, name, in, httpHelper.getUser(), transferProtocols);
	}

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in, UserCredentials user,
			ArrayList<String> transferProtocols) throws RuntimeApiException, StormRequestFailureException, StormResourceException,
			TooManyResultsException {
		log.debug("Called doPut()");
		File fsDest = new File(sourceDir.getFile(), name);
		Surl newFile = new Surl(sourceDir.getSurl(), name);
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPut(sourceDir.getFactory().getBackendApi(), newFile.asString(), user,
				transferProtocols);
		// put
		try {
			sourceDir.getFactory().getContentService().setFileContent(fsDest, in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new StormResourceException("FileNotFoundException!", e);
		} catch (IOException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new StormResourceException("IOException!", e);
		}
		StormBackendApi.putDone(sourceDir.getFactory().getBackendApi(), newFile.asString(), outputPtp.getToken(), user);
		return new StormFileResource(sourceDir, name);
	}

	public static StormFileResource doPutOverwrite(StormFileResource source, InputStream in) throws RuntimeApiException,
			StormRequestFailureException, StormResourceException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doPutOverwrite(source, in, httpHelper.getUser());
	}

	public static StormFileResource doPutOverwrite(StormFileResource source, InputStream in, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, StormResourceException, TooManyResultsException {
		log.debug("Called doPutOverwrite()");
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(httpHelper.getRequestProtocol());
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPutOverwrite(source.getFactory().getBackendApi(), source.getSurl()
				.asString(), user, transferProtocols);
		// overwrite
		try {
			source.getFactory().getContentService().setFileContent(source.getFile(), in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			abortRequest(source.getFactory().getBackendApi(), outputPtp.getToken(), user);
			throw new StormResourceException("Couldnt write to: " + source.getFile().getAbsolutePath(), ex);
		}
		StormBackendApi.putDone(source.getFactory().getBackendApi(), source.getSurl().asString(), outputPtp.getToken(), user);
		return source;
	}

	/* LS */

	private static ArrayList<SurlInfo> filterLs(ArrayList<SurlInfo> collection) {
		ArrayList<SurlInfo> filteredOutput = new ArrayList<SurlInfo>();
		for (SurlInfo info : collection) {
			if (!(info.getStatus().getStatusCode().equals(TStatusCode.SRM_INVALID_PATH) || (info.getStatus().getStatusCode()
					.equals(TStatusCode.SRM_FAILURE)))) {
				filteredOutput.add(info);
			} else {
				log.warn(info.getStfn() + " status code is: " + info.getStatus().getStatusCode().getValue());
			}
		}
		return filteredOutput;
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doLsDetailed(source, recursion, httpHelper.getUser());
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("Called doLsDetailed()");
		LsOutputData output = StormBackendApi.lsDetailed(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				new RecursionLevel(recursion));
		return filterLs((ArrayList<SurlInfo>) output.getInfos());
	}

	public static ArrayList<SurlInfo> doLs(StormResource source) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doLs(source, httpHelper.getUser());
	}

	public static ArrayList<SurlInfo> doLs(StormResource source, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		log.debug("Called doLs()");
		LsOutputData output = StormBackendApi.ls(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
		return filterLs((ArrayList<SurlInfo>) output.getInfos());
	}

	public static ArrayList<SurlInfo> doLimitedLsDetailed(StormResource resource) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		return doLimitedLsDetailed(resource.getFactory().getBackendApi(), resource.getFile());
	}

	public static ArrayList<SurlInfo> doLimitedLsDetailed(BackendApi backend, File file) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doLimitedLsDetailed(httpHelper.getUser(), backend, file);
	}

	public static ArrayList<SurlInfo> doLimitedLsDetailed(UserCredentials user, BackendApi backend, File file) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		log.debug("Called doLimitedLsDetailed()");
		Surl surl = new Surl(file);
		LsOutputData output = StormBackendApi.lsDetailed(backend, surl.asString(), user, new RecursionLevel(Recursion.LIMITED, 0));
		return filterLs((ArrayList<SurlInfo>) output.getInfos());
	}

	/* PING */

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort) throws RuntimeApiException,
			StormRequestFailureException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doPing(stormBackendHostname, stormBackendPort, httpHelper.getUser());
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException {
		log.debug("Called doPing()");
		BackendApi backend = StormBackendApi.getBackend(stormBackendHostname, stormBackendPort);
		return StormBackendApi.ping(backend, user);
	}

	/* COPY */

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException, StormRequestFailureException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		doCopyDirectory(sourceDir, newParent, newName, httpHelper.isDepthInfinity(), httpHelper.getUser());
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName,
			boolean isDepthInfinity, UserCredentials user) throws RuntimeApiException, StormRequestFailureException,
			StormResourceException, TooManyResultsException {
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
			StormRequestFailureException, StormResourceException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		doCopyFile(source, newParent, newName, httpHelper.getUser());
	}

	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, StormResourceException, TooManyResultsException {
		log.debug("Called doCopyFile()");
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(httpHelper.getRequestProtocol());
		/* prepareToGet on source file to lock the resource */
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				transferProtocols);
		try {
			/* create destination */
			transferProtocols.clear();
			transferProtocols.add(httpHelper.getDestinationProtocol());
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

	/* STATUS OF PTG */

	public static RequestOutputData doPrepareToGetStatus(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doPrepareToGetStatus(source, httpHelper.getUser());
	}

	public static RequestOutputData doPrepareToGetStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("Called doPrepareToGetStatus()");
		return StormBackendApi.prepareToGetStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}

	/* STATUS OF PTP */

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		return doPrepareToPutStatus(source, httpHelper.getUser());
	}

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("Called doPrepareToPutStatus()");
		return StormBackendApi.prepareToPutStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}

	/* */

}