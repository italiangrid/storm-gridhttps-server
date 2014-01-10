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
package it.grid.storm.gridhttps.webapp.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.Resource;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.common.StormResource;
import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.factory.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.common.factory.StormFileResource;
import it.grid.storm.gridhttps.webapp.common.srmOperations.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TReturnStatus;
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

	private final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);

	private String hostnameBE;
	private int portBE;
	private String tokenBE;
	private ArrayList<TStatusCode> lsIgnored;
	private BackendApi backend;
	private HttpHelper helper;

	public static StormResourceHelper getInstance() throws SRMOperationException {
		return new StormResourceHelper(Configuration.getBackendInfo().getHostname(), Configuration.getBackendInfo().getPort(), 
			Configuration.getBackendInfo().getToken());
	}
	
	private StormResourceHelper(String hostname, int port, String token) throws SRMOperationException {
		init(hostname, port, token);
	}
	
	private void init(String hostname, int port, String token) throws SRMOperationException {
		this.hostnameBE = hostname;
		this.portBE = port;
		this.tokenBE = token;
		this.lsIgnored = new ArrayList<TStatusCode>();
		this.lsIgnored.add(TStatusCode.SRM_FAILURE);
		this.lsIgnored.add(TStatusCode.SRM_INVALID_PATH);
		try {
			this.backend = new BackendApi(this.hostnameBE, new Long(this.portBE), this.tokenBE);
		} catch (ApiException e) {
			log.error(e.toString());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		this.helper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
	}

	private BackendApi getBackend() {
		return this.backend;
	}

	private HttpHelper getHttpHelper() {
		return this.helper;
	}
	
	/* ABORT REQUEST */

	private void doAbortRequest(Surl surl, TRequestToken token) throws SRMOperationException {
		
		AbortRequest rollbackOp = new AbortRequest(surl, token);
		RequestOutputData output = rollbackOp.executeAs(getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess()) {
			log.warn(String.format("abortRequest failed on surl %s and token %s: %s",
				surl, token, output.getStatus().getExplanation()));
		}
	}

	/* MKCOL */

	public StormDirectoryResource doMkCol(StormDirectoryResource parentDir, String destName) throws SRMOperationException {
		
		MkDir operation = new MkDir(new Surl(parentDir.getSurl(), destName));
		RequestOutputData output = operation.executeAs(getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess()) {
			throw new SRMOperationException(output.getStatus());
		}
		return new StormDirectoryResource(parentDir, destName);
	}

	/* PING */

	public PingOutputData doPing(UserCredentials user, String hostname, int port) throws SRMOperationException {
		
		Ping operation = new Ping(hostname, port);
		PingOutputData output = operation.executeAs(user, this.getBackend());
		if (!output.isSuccess()) {
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_FAILURE, "ping error"));
		}
		return output;
	}

	/* DELETE */

	public RequestOutputData doDelete(StormResource toDelete) throws SRMOperationException {
		
		SRMOperation operation;
		if (toDelete instanceof StormDirectoryResource)
			operation = new RmDir(toDelete.getSurl());
		else
			operation = new Rm(toDelete.getSurl());
		RequestOutputData output = (RequestOutputData) operation.executeAs(getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess()) {
			throw new SRMOperationException(output.getStatus());
		}
		return output;
	}

	/* MOVE */

	public RequestOutputData doMove(StormResource source, StormDirectoryResource destParent, String destName)
			throws SRMOperationException {
		
		Move operation = new Move(source.getSurl(), new Surl(destParent.getSurl(), destName));
		RequestOutputData output = operation.executeAs(getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess()) {
			throw new SRMOperationException(output.getStatus());
		}
		return output;
	}

	/* GET */

	public InputStream doGetFile(StormFileResource source) throws SRMOperationException {
		
		// PTG
		PrepareToGet ptg = new PrepareToGet(source.getSurl());
		PtGOutputData outputPtG = ptg.executeAs(this.getHttpHelper().getUser(), this.getBackend());
		if (!outputPtG.getStatus().getStatusCode().equals(TStatusCode.SRM_FILE_PINNED)) {
			this.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw new SRMOperationException(outputPtG.getStatus());
		}
		// READ FROM DISK
		InputStream in = null;
		try {
			in = source.getFactory().getContentService().getFileContent(source.getFile());
		} catch (Exception e) {
			log.error(e.getMessage());
			this.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		// RF
		SurlArrayRequestOutputData oRf;
		ReleaseFile rf = new ReleaseFile(source.getSurl(), outputPtG.getToken());
		try {
			oRf = rf.executeAs(this.getHttpHelper().getUser(), this.getBackend());
		} catch (SRMOperationException e) {
			this.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw e;
		}
		if (!oRf.isSuccess()) {
			this.doAbortRequest(source.getSurl(), outputPtG.getToken());
			throw new SRMOperationException(oRf.getStatus());
		}
		return in;
	}

	/* PUT */

	public StormFileResource doPut(StormDirectoryResource parentDir, String name, InputStream in) throws SRMOperationException {
		
		// PTP
		Surl toCreate = new Surl(parentDir.getSurl(), name);
		PrepareToPut ptp = new PrepareToPut(toCreate);
		FileTransferOutputData oPtP = ptp.executeAs(getHttpHelper().getUser(), this.getBackend());
		if (!oPtP.getStatus().getStatusCode().equals(TStatusCode.SRM_SPACE_AVAILABLE)) {
			this.doAbortRequest(toCreate, oPtP.getToken());
			throw new SRMOperationException(oPtP.getStatus());
		}
		// WRITE FILE
		File fsDest = new File(parentDir.getFile(), name);
		try {
			parentDir.getFactory().getContentService().setFileContent(fsDest, in);
		} catch (Exception e) {
			log.error(e.getMessage());
			this.doAbortRequest(toCreate, oPtP.getToken());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		// PD
		PutDone pd = new PutDone(toCreate, oPtP.getToken());
		SurlArrayRequestOutputData oPd;
		try {
			oPd = pd.executeAs(getHttpHelper().getUser(), this.getBackend());
		} catch (SRMOperationException e) {
			this.doAbortRequest(toCreate, oPtP.getToken());
			throw e;
		}
		if (!oPd.isSuccess()) {
			this.doAbortRequest(toCreate, oPtP.getToken());
			throw new SRMOperationException(oPd.getStatus());
		}
		return new StormFileResource(parentDir, name);
	}
	
	public void doPutOverwrite(StormFileResource toReplace, InputStream in) throws SRMOperationException {

		// PTP
		PrepareToPut ptp = new PrepareToPut(toReplace.getSurl(), true);
		FileTransferOutputData oPtP = ptp.executeAs(getHttpHelper().getUser(), this.getBackend()); 
		if (!oPtP.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			this.doAbortRequest(toReplace.getSurl(), oPtP.getToken());
			throw new SRMOperationException(oPtP.getStatus());
		}
		// WRITE FILE
		try {
			toReplace.getFactory().getContentService()
				.setFileContent(toReplace.getFile(), in);
		} catch (Exception e) {
			log.error(e.getMessage());
			this.doAbortRequest(toReplace.getSurl(), oPtP.getToken());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		// PD
		PutDone pd = new PutDone(toReplace.getSurl(), oPtP.getToken());
		SurlArrayRequestOutputData oPd;
		try {
			oPd = pd.executeAs(getHttpHelper().getUser(), this.getBackend());
		} catch (SRMOperationException e) {
			this.doAbortRequest(toReplace.getSurl(), oPtP.getToken());
			throw e;
		}
		if (!oPd.isSuccess()) {
			this.doAbortRequest(toReplace.getSurl(), oPtP.getToken());
			throw new SRMOperationException(oPd.getStatus());
		}
	}

	/* COPY */

	public void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, BadRequestException {
		this.doCopyDirectory(sourceDir, newParent, newName, getHttpHelper().isDepthInfinity());
	}
	
	private void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName,
			boolean isDepthInfinity) throws NotAuthorizedException, BadRequestException {

		log.debug(String.format("copy '%s' to '%s' ...", sourceDir.getSurl()
			.asString(), newParent.getSurl().asString() + File.separator + newName));
		// create destination folder:
		StormDirectoryResource destinationResource = this.doMkCol(newParent, newName);
		// COPY every resource from the source to the destination folder:
		for (Resource r : sourceDir.getChildren()) {
			if (r instanceof StormFileResource) { // is a file
				this.doCopyFile((StormFileResource) r, destinationResource, ((StormFileResource) r).getName());
			} else if (r instanceof StormDirectoryResource) { // is a directory
				if (isDepthInfinity) { // recursion on
					this.doCopyDirectory((StormDirectoryResource) r, destinationResource,
							((StormDirectoryResource) r).getName(), isDepthInfinity);
				} else { // recursion off
					this.doMkCol(destinationResource, ((StormDirectoryResource) r).getName());
				}
			}
		}
	}
	
	public void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName) throws SRMOperationException {

		log.debug(String.format("copy '%s' to '%s' ...", source.getSurl()
			.asString(), newParent.getSurl().asString() + File.separator + newName));
		InputStream input = null;
		try {
			input = source.getInputStream();
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		this.doPut(newParent, newName, input);
	}

	/* LS */

	public ArrayList<SurlInfo> filterLs(Collection<SurlInfo> collection) {
		ArrayList<SurlInfo> filteredOutput = new ArrayList<SurlInfo>();
		for (SurlInfo info : collection) {
			if (info == null) {
				log.warn("ignored NULL surl-info!!");
				continue;
			}
			if (this.lsIgnored.contains(info.getStatus().getStatusCode())) {
				log.warn(String.format("ignored '%s' from ls list, status is %s",
					info.getStfn(), info.getStatus().getStatusCode().getValue()));
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
	
	public LsOutputData doLs(File source) throws SRMOperationException {
		
		Ls operation = new Ls(new Surl(source));
		LsOutputData output = operation.executeAs(this.getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess())
			throw new SRMOperationException(output.getStatus());
		return output;
	}

	public LsOutputData doLsDetailed(File source, RecursionLevel recursion, int count) throws SRMOperationException {
		
		/*
		 * TIP: the lightest ls is obtained with:
		 * 	RecursionLevel recursion = new RecursionLevel(Recursion.LIMITED, 0)
		 * 
		 */
		
		LsDetailed operation = new LsDetailed(new Surl(source), recursion, count);
		LsOutputData output = operation.executeAs(this.getHttpHelper().getUser(), this.getBackend());
		if (!output.isSuccess())
			throw new SRMOperationException(output.getStatus());
		return output;
	}
	
	public LsOutputData doLsDetailed(File source) throws SRMOperationException {
		
		return doLsDetailed(source, LsDetailed.DEFAULT_RECURSION_LEVEL, LsDetailed.DEFAULT_COUNT);
	}

	public LsOutputData doLsDetailed(File source, RecursionLevel recursion) throws SRMOperationException {
		
		return doLsDetailed(source, recursion, LsDetailed.DEFAULT_COUNT);
	}

	public LsOutputData doLs(StormResource source) throws SRMOperationException {
		
		return this.doLs(source.getFile());
	}

	public LsOutputData doLsDetailed(StormResource source) throws SRMOperationException {
		
		return this.doLsDetailed(source.getFile());
	}

	public LsOutputData doLsDetailed(StormResource source, RecursionLevel recursion) throws SRMOperationException {
		
		return this.doLsDetailed(source.getFile(), recursion);
	}

	public LsOutputData doLsDetailed(StormResource source, RecursionLevel recursion, int count) throws SRMOperationException {
		
		return this.doLsDetailed(source.getFile(), recursion, count);
	}

	/* LIMITED LS DETAILED */

	public LsOutputData doLimitedLsDetailed(File source) throws SRMOperationException {
		
		return this.doLsDetailed(source, new RecursionLevel(Recursion.LIMITED, 0));
	}

	public LsOutputData doLimitedLsDetailed(File source, int count) throws SRMOperationException {
		
		return this.doLsDetailed(source, new RecursionLevel(Recursion.LIMITED, 0), count);
	}

	public LsOutputData doLimitedLsDetailed(StormResource source) throws SRMOperationException {
		
		return this.doLimitedLsDetailed(source.getFile());
	}

	public LsOutputData doLimitedLsDetailed(StormResource source, int count) throws SRMOperationException {
		
		return this.doLimitedLsDetailed(source.getFile(), count);
	}

}