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
import java.util.Collection;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.Resource;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormResourceException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.gridhttps.webapp.srmOperations.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);

	private static String hostnameBE;
	private static int portBE;
	private static ArrayList<TStatusCode> lsIgnored;

	public static void init(String hostname, int port) {
		StormResourceHelper.hostnameBE = hostname;
		StormResourceHelper.portBE = port;
		StormResourceHelper.lsIgnored = new ArrayList<TStatusCode>();
		StormResourceHelper.lsIgnored.add(TStatusCode.SRM_FAILURE);
		StormResourceHelper.lsIgnored.add(TStatusCode.SRM_INVALID_PATH);
	}

	private static BackendApi getBackend() throws RuntimeApiException {
		try {
			return new BackendApi(StormResourceHelper.hostnameBE, new Long(StormResourceHelper.portBE));
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
	}

	/* ABORT REQUEST */

	private static void doAbortRequest(Surl surl, TRequestToken token) throws RuntimeApiException, StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		AbortRequest rollbackOp = new AbortRequest(surl, token);
		rollbackOp.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	/* MKCOL */

	public static StormDirectoryResource doMkCol(StormDirectoryResource parentDir, String destName) throws RuntimeApiException,
			StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		MkDir operation = new MkDir(new Surl(parentDir.getSurl(), destName));
		operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		return new StormDirectoryResource(parentDir, destName);
	}

	/* PING */

	public static PingOutputData doPing(UserCredentials user, String hostname, int port) throws RuntimeApiException, StormRequestFailureException {
		Ping operation = new Ping(hostname, port);
		return operation.executeAs(user, StormResourceHelper.getBackend());
	}

	/* DELETE */

	public static RequestOutputData doDelete(StormResource toDelete) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		SRMOperation operation;
		if (toDelete instanceof StormDirectoryResource) { // DIRECTORY
			operation = new RmDir(toDelete.getSurl());
		} else { // FILE
			operation = new Rm(toDelete.getSurl());
		}
		return (RequestOutputData) operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	/* MOVE */

	public static RequestOutputData doMove(StormResource source, StormDirectoryResource destParent, String destName)
			throws RuntimeApiException, StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		Move operation = new Move(source.getSurl(), new Surl(destParent.getSurl(), destName));
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	/* GET */

	public static InputStream doGetFile(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		// PTG
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(helper.getRequestProtocol());
		PrepareToGet operation = new PrepareToGet(source.getSurl(), transferProtocols);
		PtGOutputData outputPtG = operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		// READ FROM DISK
		log.debug("reading file '" + source.getFile().toString() + "' ...");
		InputStream in = null;
		try {
			in = source.getFactory().getContentService().getFileContent(source.getFile());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			StormResourceHelper.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw new NotFoundException("Couldn't locate content");
		}
		// RF
		ReleaseFile operation2 = new ReleaseFile(source.getSurl(), outputPtG.getToken());
		try {
			operation2.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		} catch (RuntimeApiException e) {
			StormResourceHelper.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw e;
		} catch (StormRequestFailureException e) {
			StormResourceHelper.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw e;
		} catch (RuntimeException e) {
			StormResourceHelper.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw e;
		}
		return in;
	}

	/* PUT */

	public static StormFileResource doPut(StormDirectoryResource parentDir, String name, InputStream in)
			throws RuntimeApiException, StormRequestFailureException, StormResourceException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		// PTP
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(helper.getRequestProtocol());
		Surl toCreate = new Surl(parentDir.getSurl(), name);
		PrepareToPut operation = new PrepareToPut(toCreate, transferProtocols, false);
		FileTransferOutputData outputPtP = operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		// WRITE FILE
		File fsDest = new File(parentDir.getFile(), name);
		try {
			parentDir.getFactory().getContentService().setFileContent(fsDest, in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			StormResourceHelper.doAbortRequest(toCreate, outputPtP.getToken());
			throw new StormResourceException("FileNotFoundException!", e);
		} catch (IOException e) {
			log.error(e.getMessage());
			StormResourceHelper.doAbortRequest(toCreate, outputPtP.getToken());
			throw new StormResourceException("IOException!", e);
		}
		// PD
		PutDone operation2 = new PutDone(toCreate, outputPtP.getToken());
		try {
			operation2.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		} catch (RuntimeApiException e) {
			StormResourceHelper.doAbortRequest(toCreate, outputPtP.getToken());
			throw e;
		} catch (StormRequestFailureException e) {
			StormResourceHelper.doAbortRequest(toCreate, outputPtP.getToken());
			throw e;
		} catch (RuntimeException e) {
			StormResourceHelper.doAbortRequest(toCreate, outputPtP.getToken());
			throw e;
		}
		return new StormFileResource(parentDir, name);
	}
	
	public static void doPutOverwrite(StormFileResource toReplace, InputStream in)
			throws RuntimeApiException, StormRequestFailureException, StormResourceException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		// PTP
		ArrayList<String> transferProtocols = new ArrayList<String>();
		transferProtocols.add(helper.getRequestProtocol());
		PrepareToPut operation = new PrepareToPut(toReplace.getSurl(), transferProtocols, true);
		FileTransferOutputData outputPtP = operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		// WRITE FILE
		try {
			toReplace.getFactory().getContentService().setFileContent(toReplace.getFile(), in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			StormResourceHelper.doAbortRequest(toReplace.getSurl(), outputPtP.getToken());
			throw new StormResourceException("FileNotFoundException!", e);
		} catch (IOException e) {
			log.error(e.getMessage());
			StormResourceHelper.doAbortRequest(toReplace.getSurl(), outputPtP.getToken());
			throw new StormResourceException("IOException!", e);
		}
		// PD
		PutDone operation2 = new PutDone(toReplace.getSurl(), outputPtP.getToken());
		try {
			operation2.executeAs(helper.getUser(), StormResourceHelper.getBackend());
		} catch (RuntimeApiException e) {
			StormResourceHelper.doAbortRequest(toReplace.getSurl(), outputPtP.getToken());
			throw e;
		} catch (StormRequestFailureException e) {
			StormResourceHelper.doAbortRequest(toReplace.getSurl(), outputPtP.getToken());
			throw e;
		} catch (RuntimeException e) {
			StormResourceHelper.doAbortRequest(toReplace.getSurl(), outputPtP.getToken());
			throw e;
		}
	}

	/* STATUS OF PTG */

	public static SurlArrayRequestOutputData doPrepareToGetStatus(Surl source) throws RuntimeApiException, StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		PrepareToGetStatus operation = new PrepareToGetStatus(source);
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}
	
	public static SurlArrayRequestOutputData doPrepareToGetStatus(StormFileResource source) throws RuntimeApiException, StormRequestFailureException {
		return StormResourceHelper.doPrepareToGetStatus(source.getSurl());
	}

	/* STATUS OF PTP */

	public static SurlArrayRequestOutputData doPrepareToPutStatus(Surl source) throws RuntimeApiException, StormRequestFailureException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		PrepareToPutStatus operation = new PrepareToPutStatus(source);
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}
	
	public static SurlArrayRequestOutputData doPrepareToPutStatus(StormFileResource source) throws RuntimeApiException, StormRequestFailureException {
		return StormResourceHelper.doPrepareToPutStatus(source.getSurl());
	}

	/* COPY */

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException, StormRequestFailureException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		doCopyDirectory(sourceDir, newParent, newName, httpHelper.isDepthInfinity());
	}
	
	private static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName,
			boolean isDepthInfinity) throws RuntimeApiException, StormRequestFailureException, StormResourceException, BadRequestException,
			NotAuthorizedException {
		log.debug("copy '" + sourceDir.getSurl().asString() + "' to '" + newParent.getSurl().asString() + File.separator + newName
				+ "' ...");
		// create destination folder:
		StormDirectoryResource destinationResource = StormResourceHelper.doMkCol(newParent, newName);
		// COPY every resource from the source to the destination folder:
		for (Resource r : sourceDir.getChildren()) {
			if (r instanceof StormFileResource) { // is a file
				StormResourceHelper.doCopyFile((StormFileResource) r, destinationResource, ((StormFileResource) r).getName());
			} else if (r instanceof StormDirectoryResource) { // is a directory
				if (isDepthInfinity) { // recursion on
					StormResourceHelper.doCopyDirectory((StormDirectoryResource) r, destinationResource,
							((StormDirectoryResource) r).getName(), isDepthInfinity);
				} else { // recursion off
					StormResourceHelper.doMkCol(destinationResource, ((StormDirectoryResource) r).getName());
				}
			}
		}
	}
	
	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName) throws RuntimeApiException,
			StormRequestFailureException, StormResourceException {
		log.debug("copy '" + source.getSurl().asString() + "' to '" + newParent.getSurl().asString() + File.separator + newName + "' ...");
		try {
			StormResourceHelper.doPut(newParent, newName, source.getInputStream());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new StormResourceException("FileNotFoundException!", e);
		}
	}

	/* LS */

	public static ArrayList<SurlInfo> filterLs(Collection<SurlInfo> collection) {
		ArrayList<SurlInfo> filteredOutput = new ArrayList<SurlInfo>();
		for (SurlInfo info : collection) {
			if (info == null) {
				log.warn("ignored NULL surl-info!!");
				continue;
			}
			if (StormResourceHelper.lsIgnored.contains(info.getStatus().getStatusCode())) {
				log.warn("ignored '" + info.getStfn() + "' from ls list, status is " + info.getStatus().getStatusCode().getValue());
				continue;
			}
			if (info.getType() == null) {
				log.warn("Surl-Info Type is NULL! ignored surl-info details: ");
				log.warn(info.toString());
				continue;
			}
			filteredOutput.add(info);
		}
		return filteredOutput;
	}

	// new RecursionLevel(Recursion.LIMITED, 0) >> LIMITED DETAILED LS

	public static LsOutputData doLs(File source) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		Ls operation = new Ls(new Surl(source));
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	public static LsOutputData doLsDetailed(File source) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		LsDetailed operation = new LsDetailed(new Surl(source));
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	public static LsOutputData doLsDetailed(File source, RecursionLevel recursion) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		LsDetailed operation = new LsDetailed(new Surl(source), recursion);
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	public static LsOutputData doLsDetailed(File source, RecursionLevel recursion, int count) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		HttpHelper helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		LsDetailed operation = new LsDetailed(new Surl(source), recursion, count);
		return operation.executeAs(helper.getUser(), StormResourceHelper.getBackend());
	}

	public static LsOutputData doLs(StormResource source) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		return StormResourceHelper.doLs(source.getFile());
	}

	public static LsOutputData doLsDetailed(StormResource source) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		return StormResourceHelper.doLsDetailed(source.getFile());
	}

	public static LsOutputData doLsDetailed(StormResource source, RecursionLevel recursion) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		return StormResourceHelper.doLsDetailed(source.getFile(), recursion);
	}

	public static LsOutputData doLsDetailed(StormResource source, RecursionLevel recursion, int count) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		return StormResourceHelper.doLsDetailed(source.getFile(), recursion, count);
	}

	/* LIMITED LS DETAILED */

	public static LsOutputData doLimitedLsDetailed(File source) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		return StormResourceHelper.doLsDetailed(source, new RecursionLevel(Recursion.LIMITED, 0));
	}

	public static LsOutputData doLimitedLsDetailed(File source, int count) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		return StormResourceHelper.doLsDetailed(source, new RecursionLevel(Recursion.LIMITED, 0), count);
	}

	public static LsOutputData doLimitedLsDetailed(StormResource source) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		return StormResourceHelper.doLimitedLsDetailed(source.getFile());
	}

	public static LsOutputData doLimitedLsDetailed(StormResource source, int count) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
		return StormResourceHelper.doLimitedLsDetailed(source.getFile(), count);
	}

}