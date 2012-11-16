package it.grid.storm.webdav.webapp.factory;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.webdav.webapp.Configuration;
import it.grid.storm.webdav.webapp.authorization.UserCredentials;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class StormBackendApi {

	private static final Logger log = LoggerFactory.getLogger(StormBackendApi.class);

	public static BackendApi getBackend(String hostname, int port) {
		BackendApi backend = null;
		try {
			backend = new BackendApi(Configuration.stormBackendHostname, new Long(Configuration.stormBackendPort));
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return backend;
	}
	
	public static RequestOutputData abortRequest(BackendApi backend, TRequestToken token, UserCredentials user) {
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
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("IllegalArgumentException!", e);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		}
		return output;
	}

	public static PingOutputData ping(BackendApi backend, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static PtGOutputData prepareToGet(BackendApi backend, String surl, UserCredentials user) {
		PtGOutputData outputPtG = null;
		log.debug("prepare to get surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtG = backend.prepareToGet(surl);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtG = backend.prepareToGet(user.getUserDN(), surl);
			} else {
				outputPtG = backend.prepareToGet(user.getUserDN(), user.getUserFQANS(), surl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return outputPtG;
	}

	public static SurlArrayRequestOutputData releaseFile(BackendApi backend, String surl, TRequestToken token, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static FileTransferOutputData prepareToPut(BackendApi backend, String newFileSurl, UserCredentials user) {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		FileTransferOutputData outputPtp = null;
		log.debug("prepare to put surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtp = backend.prepareToPut(newFileSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtp = backend.prepareToPut(user.getUserDN(), newFileSurl);
			} else {
				outputPtp = backend.prepareToPut(user.getUserDN(), user.getUserFQANS(), newFileSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return outputPtp;
	}

	public static FileTransferOutputData prepareToPutOverwrite(BackendApi backend, String newFileSurl, UserCredentials user) {
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);
		FileTransferOutputData outputPtp = null;
		log.debug("prepare to put overwrite surl: " + newFileSurl);
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtp = backend.prepareToPutOverwrite(newFileSurl);
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtp = backend.prepareToPutOverwrite(user.getUserDN(), newFileSurl);
			} else {
				outputPtp = backend.prepareToPutOverwrite(user.getUserDN(), user.getUserFQANS(), newFileSurl);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return outputPtp;
	}

	public static SurlArrayRequestOutputData putDone(BackendApi backend, String newFileSurl, TRequestToken token, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			abortRequest(backend, token, user);
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return outputPd;
	}

	public static RequestOutputData mkdir(BackendApi backend, String newDirSurl, UserCredentials user) throws ApiException,
			IllegalArgumentException {
		RequestOutputData output = null;
		log.debug("mkdir surl: " + newDirSurl);
		if (user.isAnonymous()) { // HTTP
			output = backend.mkdir(newDirSurl);
		} else if (user.getUserFQANS().isEmpty()) {
			output = backend.mkdir(user.getUserDN(), newDirSurl);
		} else {
			output = backend.mkdir(user.getUserDN(), user.getUserFQANS(), newDirSurl);
		}
		return output;
	}

	public static RequestOutputData rm(BackendApi backend, String surl, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static RequestOutputData rmdir(BackendApi backend, String surl, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static RequestOutputData rmdirRecoursively(BackendApi backend, String surl, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static RequestOutputData mv(BackendApi backend, String fromSurl, String toSurl, UserCredentials user) {
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
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}

	public static LsOutputData lsDetailed(BackendApi backend, String surl, UserCredentials user) {
		ArrayList<String> surlList = new ArrayList<String>();
		surlList.add(surl);
		LsOutputData output = null;
		log.debug("lsDetailed surl: " + surl);
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.lsDetailed(surlList);
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.lsDetailed(user.getUserDN(), surlList);
			} else {
				output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), surlList);
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		return output;
	}
}