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
package it.grid.storm.gridhttps.webapp.backendApi;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TStatusCode;
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

	public static PtGOutputData prepareToGet(BackendApi backend, String surl, UserCredentials user, List<String> transferProtocols)
			throws RuntimeApiException, TooManyResultsException, StormRequestFailureException {
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
		log.debug(outputPtG.getStatus().getExplanation());
		if (outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			return outputPtG;
		} else if (outputPtG.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("prepare-to-get output status is " + outputPtG.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("prepare-to-get output status is " + outputPtG.getStatus().getStatusCode().getValue());
		}
	}

	public static SurlArrayRequestOutputData releaseFile(BackendApi backend, String surl, TRequestToken token, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			abortRequest(backend, token, user);
			throw new TooManyResultsException("release-file output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			abortRequest(backend, token, user);
			throw new StormRequestFailureException("release-file output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static FileTransferOutputData prepareToPut(BackendApi backend, String newFileSurl, UserCredentials user,
			List<String> transferProtocols) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(outputPtp.getStatus().getExplanation());
		if (outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			return outputPtp;
		} else if (outputPtp.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("prepare-to-put output status is " + outputPtp.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("prepare-to-put output status is " + outputPtp.getStatus().getStatusCode().getValue());
		}
	}

	public static FileTransferOutputData prepareToPutOverwrite(BackendApi backend, String newFileSurl, UserCredentials user,
			List<String> transferProtocols) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(outputPtp.getStatus().getExplanation());
		if (outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			return outputPtp;
		} else if (outputPtp.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("prepare-to-put-overwrite output status is "
					+ outputPtp.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("prepare-to-put-overwrite output status is "
					+ outputPtp.getStatus().getStatusCode().getValue());
		}
	}

	public static SurlArrayRequestOutputData putDone(BackendApi backend, String newFileSurl, TRequestToken token, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			abortRequest(backend, token, user);
			throw new StormRequestFailureException("put-done status is " + outputPd.getStatus().getStatusCode().getValue());
		}
		if (outputPd.isSuccess()) {
			return outputPd;
		} else if (outputPd.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			abortRequest(backend, token, user);
			throw new TooManyResultsException("put-done output status is " + outputPd.getStatus().getStatusCode().getValue());
		} else {
			abortRequest(backend, token, user);
			throw new StormRequestFailureException("put-done output status is " + outputPd.getStatus().getStatusCode().getValue());
		}
	}

	public static RequestOutputData mkdir(BackendApi backend, String newDirSurl, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("mkdir output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("mkdir output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static RequestOutputData rm(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("rm output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("rm output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static RequestOutputData rmdir(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("rmdir output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("rmdir output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static RequestOutputData rmdirRecoursively(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("rmdir-recoursively output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("mkdir-recoursively output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static RequestOutputData mv(BackendApi backend, String fromSurl, String toSurl, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("mv output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("mv output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static LsOutputData lsDetailed(BackendApi backend, String surl, UserCredentials user, RecursionLevel recursion)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("ls-detailed output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("ls-detailed output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static LsOutputData ls(BackendApi backend, String surl, UserCredentials user) throws RuntimeApiException,
			StormRequestFailureException, TooManyResultsException {
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
		log.debug(output.getStatus().getExplanation());
		if (output.isSuccess()) {
			return output;
		} else if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("ls output status is " + output.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("ls output status is " + output.getStatus().getStatusCode().getValue());
		}
	}

	public static SurlArrayRequestOutputData prepareToPutStatus(BackendApi backend, String newFileSurl, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(outputSPtp.getStatus().getExplanation());
		
		if (outputSPtp.getStatus().getStatusCode().getValue().equals("SRM_SUCCESS")) {
			return outputSPtp;
		} else if (outputSPtp.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("prepare-to-put-status output status is " + outputSPtp.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("prepare-to-put-status output status is " + outputSPtp.getStatus().getStatusCode().getValue());
		}
	}

	public static SurlArrayRequestOutputData prepareToGetStatus(BackendApi backend, String newFileSurl, UserCredentials user)
			throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
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
		log.debug(outputSPtg.getStatus().getExplanation());
		
		if (outputSPtg.getStatus().getStatusCode().getValue().equals("SRM_SUCCESS")) {
			return outputSPtg;
		} else if (outputSPtg.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS)) {
			throw new TooManyResultsException("prepare-to-put-status output status is " + outputSPtg.getStatus().getStatusCode().getValue());
		} else {
			throw new StormRequestFailureException("prepare-to-put-status output status is " + outputSPtg.getStatus().getStatusCode().getValue());
		}
	}

}