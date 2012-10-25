package it.grid.storm.webdav.webapp.factory;

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
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.webdav.webapp.Configuration;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);
	
	/* STORM METHOD */
	
	private static void abortRequest(StormResourceFactory factory, TRequestToken token) {
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		log.info("Aborting srm request...");
		try { //ABORT REQUEST
			factory.getBackendApi().abortRequest(userDN, userFQANs, token);
		} catch (ApiException ex) {
			log.error(ex.getMessage());
			throw new RuntimeException(ex.getMessage());
		}
	}
	
	public static boolean doMoveTo(StormResource source, StormResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doMoveTo()");
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		String fromSurl = source.getSurl();
		String toSurl = newParent.getSurl() + "/" + newName;
		RequestOutputData output = null;
		try {
			output = (RequestOutputData) source.factory.getBackendApi().mv(userDN, userFQANs, fromSurl, toSurl);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.info(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return output.isSuccess();
	}
	
	public static boolean doDelete(StormResource source) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doDelete()");
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		RequestOutputData output = null;
		try {
			if (source instanceof StormDirectoryResource) { //DIRECTORY
				StormDirectoryResource sourceDir = (StormDirectoryResource) source;
				if (sourceDir.hasChildren()) {
					log.info("rmdir-recursively: " + sourceDir.file.toString());
					output = sourceDir.factory.getBackendApi().rmdirRecursively(userDN, userFQANs, sourceDir.getSurl());
				} else {
					log.info("rmdir: " + sourceDir.file.toString());
					output = sourceDir.factory.getBackendApi().rmdir(userDN, userFQANs, sourceDir.getSurl());
				}
			} else { //FILE
				StormFileResource sourceFile = (StormFileResource) source;
				log.info("rm: " + sourceFile.file.toString());
				output = sourceFile.factory.getBackendApi().rm(userDN, userFQANs, sourceFile.getSurlAsList());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return output.isSuccess();
	}
	
	public static InputStream doGetFile(StormFileResource source) throws NotFoundException {
		log.info("Called doGetFile()");
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		PtGOutputData outputPtG = null;
		InputStream in = null;
		SurlArrayRequestOutputData output = null;
		log.info("prepare to get: " + source.file.toString());
		try {
			outputPtG = source.factory.getBackendApi().prepareToGet(userDN, userFQANs, source.getSurl());
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}	
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.info(outputPtG.getStatus().getExplanation());
		if (!outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			log.debug("PrepareToGet has failed!");
			log.error("Failed to get content: " + source.getSurl());
			return null;
		}
		try {
			in = source.contentService.getFileContent(source.file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		log.info("release files");
		try {
			output = source.factory.getBackendApi().releaseFiles(userDN, userFQANs, source.getSurlAsList(), outputPtG.getToken());
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} 
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			log.debug("ReleaseFiles has failed!");
			log.error("Failed to get content: " + source.getSurl());
			abortRequest(source.factory, outputPtG.getToken());
			return null;
		}
		return in;
	}
	
	public static boolean doMkCol(StormDirectoryResource sourceDir, String name) {
		log.info("Called doMkCol()");
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		String newDirSurl = sourceDir.getSurl() + "/" + name;
		RequestOutputData output = null;
		log.info("mkdir: " + newDirSurl);
		try {
			output = sourceDir.factory.getBackendApi().mkdir(userDN, userFQANs, newDirSurl);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return output.isSuccess();
	}
	
	public static boolean doPut(StormDirectoryResource sourceDir, String name, InputStream in) {
		log.info("Called doPut()");

		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		File destinationFile = new File(sourceDir.file, name);
		String newFileSurl = sourceDir.getSurl() + "/" + name;
		ArrayList<String> newSurlList = new ArrayList<String>();
		newSurlList.add(newFileSurl);

		FileTransferOutputData outputPtp = null;
		SurlArrayRequestOutputData outputPd = null;
		log.info("prepare to put: " + newFileSurl);
		try {
			outputPtp = sourceDir.factory.getBackendApi().prepareToPut(userDN, userFQANs, newFileSurl);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(outputPtp.getStatus().getStatusCode().getValue());
		log.info(outputPtp.getStatus().getExplanation());
		if (!outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			log.debug("PrepareToPut has failed!");
			log.error("Failed to create new resource '" + destinationFile.toString() + "'");
			return false;
		}
		// put
		try {
			sourceDir.contentService.setFileContent(destinationFile, in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			StormResourceHelper.abortRequest(sourceDir.factory, outputPtp.getToken());
			return false;
		} catch (IOException e) {
			log.error(e.getMessage());
			StormResourceHelper.abortRequest(sourceDir.factory, outputPtp.getToken());
			return false;
		}
		log.info("put done " + newFileSurl);
		try {
			outputPd = sourceDir.factory.getBackendApi().putDone(userDN, userFQANs, newSurlList, outputPtp.getToken());
		} catch (ApiException e) {
			log.error(e.getMessage());
			StormResourceHelper.abortRequest(sourceDir.factory, outputPtp.getToken());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(outputPd.getStatus().getStatusCode().getValue());
		log.info(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			StormResourceHelper.abortRequest(sourceDir.factory, outputPtp.getToken());
			log.debug("PutDone has failed!");
			log.error("Failed to create new resource '" + destinationFile.toString() + "'");
			return false;
		}
		return true;
	}
	
	public static boolean doPutOverwrite(StormFileResource source, InputStream in) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.info("Called doPutOverewrite()");
		
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		String surl = source.getSurl();
		// prepare to put overwrite
		log.info("prepare to put overwrite: " + surl);
		FileTransferOutputData outputPtp = null;
		try {	
			outputPtp = source.factory.getBackendApi().prepareToPutOverwrite(userDN, userFQANs, surl);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(outputPtp.getStatus().getStatusCode().getValue());
		log.info(outputPtp.getStatus().getExplanation());
		if (!outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			log.debug("PrepareToPutOverwrite has failed!");
			log.error("Failed to replace content: " + surl);
			return false;
		}
		try {
			// overwrite
			source.contentService.setFileContent(source.file, in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			StormResourceHelper.abortRequest(source.factory, outputPtp.getToken());
			throw new RuntimeException("Couldnt write to: " + source.file.getAbsolutePath(), ex);
		} 
		// put done
		log.info("put done... ");
		SurlArrayRequestOutputData outputPd = null;
		try {
			outputPd = source.factory.getBackendApi().putDone(userDN, userFQANs, source.getSurlAsList(), outputPtp.getToken());
		} catch (ApiException e) {
			log.error(e.getMessage());
			StormResourceHelper.abortRequest(source.factory, outputPtp.getToken());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			StormResourceHelper.abortRequest(source.factory, outputPtp.getToken());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(outputPd.getStatus().getStatusCode().getValue());
		log.info(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			StormResourceHelper.abortRequest(source.factory, outputPtp.getToken());
			log.debug("PutDone has failed!");
			log.error("Failed to replace content: " + surl);
			return false;
		} 
		return true;
	}
	
	public static ArrayList<SurlInfo> doLsDetailed(StormResource source) { 
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		LsOutputData output = null;
		log.info("lsDetailed " + source.getSurl());
		try {
			output = source.factory.getBackendApi().lsDetailed(userDN, userFQANs, source.getSurlAsList());
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static boolean doPing() {
		// ping
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		String stormBEHostname = Configuration.stormBackendHostname;
		int stormBEPort = Configuration.stormBackendPort;
		log.info("ping " + stormBEHostname + ":" + stormBEPort);
		PingOutputData pod = null;
		BackendApi be;
		try {
			be = new BackendApi(stormBEHostname, new Long(stormBEPort));
			pod = be.ping(userDN, userFQANs);
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Backend API Exception!", e);
		} catch (IllegalArgumentException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Illegal Argument Exception!", e);
		}
		log.info(pod.getBeOs());
		log.info(pod.getBeVersion());
		log.info(pod.getVersionInfo());
		return pod.isSuccess();
	}
	
	public static boolean doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
		boolean output = true;
		// Create destination folder:
		log.debug("copy '" + sourceDir.file.toString() + "' to '" + newFsParent.file.toString() + "/" + newName + "'");
		StormDirectoryResource destinationResource = (StormDirectoryResource) newFsParent.createCollection(newName);
		// COPY every resource from the source to the destination folder:
		for (Resource r : sourceDir.getChildren()) {
			StormResource sr = (StormResource) r;
			String srcPath = sr.file.toString();
			String destPath = destinationResource.file.toString();
			String rName = sr.getName();
			if (sr instanceof StormFileResource) { // r is a File
				log.debug("copy '" + srcPath + "' to '" + destPath + "/" + rName + "'");
				output &= doCopyFile((StormFileResource) sr, destinationResource, rName);
			} else { // r is a Directory
				if (StormHTTPHelper.isDepthInfinity()) { // recursively...
					output &= doCopyDirectory((StormDirectoryResource) sr, destinationResource, rName);
				} else { // not recursively...
					log.debug("copy '" + srcPath + "' to '" + destPath + "/" + rName + "'");
					destinationResource.createCollection(rName);
				}
			}
		}
		return output;
	}
	
	public static boolean doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		File destinationFile = new File(newParent.getFile(), newName);
		try {
			newParent.createNew(newName, source.getInputStream(), source.getContentLength(), source.getContentType(null));
		} catch (IOException e) {
			log.error(e.getMessage());
			log.error("Error copying file '" + source.file.toString() + "' to '" + destinationFile.toString() + "'!");
			return false;
		}
		return true;
	}
	
	
	
}
