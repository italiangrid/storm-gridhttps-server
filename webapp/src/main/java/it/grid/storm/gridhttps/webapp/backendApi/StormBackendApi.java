package it.grid.storm.gridhttps.webapp.backendApi;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormBackendApi {

	private static final Logger log = LoggerFactory.getLogger(StormBackendApi.class);

	public static BackendApi getBackend(String hostname, int port) throws RuntimeApiException {
		BackendApi backend = null;
		try {
			backend = new BackendApi(hostname, new Long(port));
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		return backend;
	}

	public static RequestOutputData abortRequest(BackendApi backend, TRequestToken token, UserCredentials user) throws RuntimeApiException {
		log.debug("aborting srm request - token " + token.getValue());
		RequestOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.abortRequest(token);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.abortRequest(user.getUserDN(), token);
			} else {
				output = backend.abortRequest(user.getUserDN(), user.getUserFQANS(), token);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		return output;
	}

	public static PingOutputData ping(BackendApi backend, UserCredentials user) throws RuntimeApiException {
		log.debug("ping backend");
		PingOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.ping();
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.ping(user.getUserDN());
			} else {
				output = backend.ping(user.getUserDN(), user.getUserFQANS());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		} 
		return output;
	}

	public static PtGOutputData prepareToGet(BackendApi backend, String surl, UserCredentials user, List<String> transferProtocols) throws RuntimeApiException, StormResourceException {
		PtGOutputData outputPtG = null;
		log.debug("prepare to get surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtG = backend.prepareToGet(surl, transferProtocols);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtG = backend.prepareToGet(user.getUserDN(), surl, transferProtocols);
			} else {
				outputPtG = backend.prepareToGet(user.getUserDN(), user.getUserFQANS(), surl, transferProtocols);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.info(outputPtG.getStatus().getExplanation());
		if (!outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			throw new StormResourceException("prepare-to-get status is " + outputPtG.getStatus().getStatusCode().getValue());
		}
		return outputPtG;
	}

	public static SurlArrayRequestOutputData releaseFile(BackendApi backend, String surl, TRequestToken token, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> surlList = new ArrayList<String>();
		surlList.add(surl);
		SurlArrayRequestOutputData output = null;
		log.debug("release surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.releaseFiles(surlList, token);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.releaseFiles(user.getUserDN(), surlList, token);
			} else {
				output = backend.releaseFiles(user.getUserDN(), user.getUserFQANS(), surlList, token);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw new RuntimeApiException(e.getMessage(), e);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw e;
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			abortRequest(backend, token, user);
			throw new StormResourceException("release-files status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static FileTransferOutputData prepareToPut(BackendApi backend, String newFileSurl, UserCredentials user, List<String> transferProtocols) throws RuntimeApiException, StormResourceException {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		FileTransferOutputData outputPtp = null;
		log.debug("prepare to put surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtp = backend.prepareToPut(newFileSurl, transferProtocols);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtp = backend.prepareToPut(user.getUserDN(), newFileSurl, transferProtocols);
			} else {
				outputPtp = backend.prepareToPut(user.getUserDN(), user.getUserFQANS(), newFileSurl, transferProtocols);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputPtp.getStatus().getStatusCode().getValue());
		log.info(outputPtp.getStatus().getExplanation());
		if (!outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			throw new StormResourceException("prepare-to-put status is " + outputPtp.getStatus().getStatusCode().getValue());
		}
		return outputPtp;
	}

	public static FileTransferOutputData prepareToPutOverwrite(BackendApi backend, String newFileSurl, UserCredentials user, List<String> transferProtocols) throws RuntimeApiException, StormResourceException {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		FileTransferOutputData outputPtp = null;
		log.debug("prepare to put overwrite surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtp = backend.prepareToPutOverwrite(newFileSurl, transferProtocols);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtp = backend.prepareToPutOverwrite(user.getUserDN(), newFileSurl, transferProtocols);
			} else {
				outputPtp = backend.prepareToPutOverwrite(user.getUserDN(), user.getUserFQANS(), newFileSurl, transferProtocols);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputPtp.getStatus().getStatusCode().getValue());
		log.info(outputPtp.getStatus().getExplanation());
		if (!outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			throw new StormResourceException("prepare-to-put-overwrite status is " + outputPtp.getStatus().getStatusCode().getValue());
		}
		return outputPtp;
	}

	public static SurlArrayRequestOutputData putDone(BackendApi backend, String newFileSurl, TRequestToken token, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		SurlArrayRequestOutputData outputPd = null;
		log.debug("put done surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPd = backend.putDone(newSurlList, token);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPd = backend.putDone(user.getUserDN(), newSurlList, token);
			} else {
				outputPd = backend.putDone(user.getUserDN(), user.getUserFQANS(), newSurlList, token);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw new RuntimeApiException(e.getMessage(), e);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw e;
		}
		log.debug(outputPd.getStatus().getStatusCode().getValue());
		log.info(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			abortRequest(backend, token, user);
			throw new StormResourceException("put-done status is " + outputPd.getStatus().getStatusCode().getValue());
		}
		return outputPd;
	}

	public static RequestOutputData mkdir(BackendApi backend, String newDirSurl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		RequestOutputData output = null;
		log.debug("mkdir surl: " + newDirSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.mkdir(newDirSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.mkdir(user.getUserDN(), newDirSurl);
			} else {
				output = backend.mkdir(user.getUserDN(), user.getUserFQANS(), newDirSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("mkdir status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static RequestOutputData rm(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> surlList = new ArrayList<String>();
		surlList.add(surl);
		RequestOutputData output = null;
		log.debug("rm surl : " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.rm(surlList);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.rm(user.getUserDN(), surlList);
			} else {
				output = backend.rm(user.getUserDN(), user.getUserFQANS(), surlList);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("rm status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static RequestOutputData rmdir(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		RequestOutputData output = null;
		log.debug("rmdir surl : " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.rmdir(surl);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.rmdir(user.getUserDN(), surl);
			} else {
				output = backend.rmdir(user.getUserDN(), user.getUserFQANS(), surl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("rm-dir status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static RequestOutputData rmdirRecoursively(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		RequestOutputData output = null;
		log.debug("rmdir-recourively surl : " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.rmdirRecursively(surl);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.rmdirRecursively(user.getUserDN(), surl);
			} else {
				output = backend.rmdirRecursively(user.getUserDN(), user.getUserFQANS(), surl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("rm-dir-recoursively status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static RequestOutputData mv(BackendApi backend, String fromSurl, String toSurl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		RequestOutputData output = null;
		log.debug("move surl: " + fromSurl + " to surl: " + toSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.mv(fromSurl, toSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.mv(user.getUserDN(), fromSurl, toSurl);
			} else {
				output = backend.mv(user.getUserDN(), user.getUserFQANS(), fromSurl, toSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("mv status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}

	public static LsOutputData lsDetailed(BackendApi backend, String surl, UserCredentials user, RecursionLevel recursion) throws RuntimeApiException, StormResourceException {
		ArrayList<String> surlList = new ArrayList<String>();
		surlList.add(surl);
		LsOutputData output = null;
		log.debug("lsDetailed surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.lsDetailed(surlList, recursion);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.lsDetailed(user.getUserDN(), surlList, recursion);
			} else {
				output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), surlList, recursion);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("ls-detailed status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}
	
	public static LsOutputData ls(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> surlList = new ArrayList<String>();
		surlList.add(surl);
		LsOutputData output = null;
		log.debug("ls surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.ls(surlList);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.ls(user.getUserDN(), surlList);
			} else {
				output = backend.ls(user.getUserDN(), user.getUserFQANS(), surlList);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException("Backend API Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormResourceException("ls status is " + output.getStatus().getStatusCode().getValue());
		}
		return output;
	}
	
	public static SurlArrayRequestOutputData prepareToPutStatus(BackendApi backend, String newFileSurl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		SurlArrayRequestOutputData outputSPtp = null;
		log.debug("prepare to put status surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputSPtp = backend.prepareToPutStatus(newFileSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				outputSPtp = backend.prepareToPutStatus(user.getUserDN(), newFileSurl);
			} else {
				outputSPtp = backend.prepareToPutStatus(user.getUserDN(), user.getUserFQANS(), newFileSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputSPtp.getStatus().getStatusCode().getValue());
		log.info(outputSPtp.getStatus().getExplanation());
//		if (!outputSPtp.getStatus().getStatusCode().getValue().equals("SRM_SUCCESS")) {
//			throw new StormResourceException("prepare-to-put-status status is " + outputSPtp.getStatus().getStatusCode().getValue());
//		}
		return outputSPtp;
	}
	
	public static SurlArrayRequestOutputData prepareToGetStatus(BackendApi backend, String newFileSurl, UserCredentials user) throws RuntimeApiException, StormResourceException {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		SurlArrayRequestOutputData outputSPtg = null;
		log.debug("prepare to get status surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputSPtg = backend.prepareToGetStatus(newFileSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				outputSPtg = backend.prepareToGetStatus(user.getUserDN(), newFileSurl);
			} else {
				outputSPtg = backend.prepareToGetStatus(user.getUserDN(), user.getUserFQANS(), newFileSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputSPtg.getStatus().getStatusCode().getValue());
		log.info(outputSPtg.getStatus().getExplanation());
//		if (!outputSPtg.getStatus().getStatusCode().getValue().equals("SRM_SUCCESS")) {
//			throw new StormResourceException("prepare-to-get-status status is " + outputSPtg.getStatus().getStatusCode().getValue());
//		}
		return outputSPtg;
	}
	
}