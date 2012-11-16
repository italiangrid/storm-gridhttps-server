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
import it.grid.storm.webdav.webapp.authorization.UserCredentials;
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

	private static void abortRequest(BackendApi backend, TRequestToken token, UserCredentials user) {
		log.info("Aborting srm request...");
		StormBackendApi.abortRequest(backend, token, user);
	}

	public static boolean doMoveTo(StormResource source, StormResource newParent, String newName) throws NotAuthorizedException,
			ConflictException, BadRequestException {
		log.info("Called doMoveTo()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		String fromSurl = source.getSurl();
		String toSurl = newParent.getSurl() + "/" + newName;
		RequestOutputData output = StormBackendApi.mv(source.factory.getBackendApi(), fromSurl, toSurl, user);
		log.info(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return output.isSuccess();
	}

	public static boolean doDelete(StormResource source) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doDelete()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		RequestOutputData output = null;
		if (source instanceof StormDirectoryResource) { // DIRECTORY
			StormDirectoryResource sourceDir = (StormDirectoryResource) source;
			if (sourceDir.hasChildren()) {
				log.info("rmdir-recursively: " + sourceDir.file.toString());
				output = StormBackendApi.rmdirRecoursively(sourceDir.factory.getBackendApi(), sourceDir.getSurl(), user);
			} else {
				log.info("rmdir: " + sourceDir.file.toString());
				output = StormBackendApi.rmdir(sourceDir.factory.getBackendApi(), sourceDir.getSurl(), user);
			}
		} else { // FILE
			StormFileResource sourceFile = (StormFileResource) source;
			log.info("rm: " + sourceFile.file.toString());
			output = StormBackendApi.rm(sourceFile.factory.getBackendApi(), sourceFile.getSurl(), user);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return output.isSuccess();
	}

	public static InputStream doGetFile(StormFileResource source) throws NotFoundException {
		log.info("Called doGetFile()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.factory.getBackendApi(), source.getSurl(), user);
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.info(outputPtG.getStatus().getExplanation());
		if (!outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			log.debug("PrepareToGet has failed!");
			log.error("Failed to get content: " + source.getSurl());
			return null;
		}
		InputStream in = null;
		try {
			in = source.contentService.getFileContent(source.file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		SurlArrayRequestOutputData output = StormBackendApi.releaseFile(source.factory.getBackendApi(), source.getSurl(), outputPtG.getToken(), user);
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			log.warn("ReleaseFiles has failed!");
			abortRequest(source.factory.getBackendApi(), outputPtG.getToken(), user);
		}
		return in;
	}

	public static boolean doMkCol(StormDirectoryResource sourceDir, String name) {
		log.info("Called doMkCol()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		String newDirSurl = sourceDir.getSurl() + "/" + name;
		RequestOutputData output = null;
		log.info("mkdir: " + newDirSurl);
		try {
			output = StormBackendApi.mkdir(sourceDir.factory.getBackendApi(), newDirSurl, user);
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
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		File destinationFile = new File(sourceDir.file, name);
		String newFileSurl = sourceDir.getSurl() + "/" + name;
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPut(sourceDir.factory.getBackendApi(), newFileSurl, user);
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
			abortRequest(sourceDir.factory.getBackendApi(), outputPtp.getToken(), user);
			return false;
		} catch (IOException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.factory.getBackendApi(), outputPtp.getToken(), user);
			return false;
		}
		SurlArrayRequestOutputData outputPd = StormBackendApi.putDone(sourceDir.factory.getBackendApi(), newFileSurl, outputPtp.getToken(), user);
		log.debug(outputPd.getStatus().getStatusCode().getValue());
		log.info(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			log.debug("PutDone has failed!");
			log.error("Failed to create new resource '" + destinationFile.toString() + "'");
			abortRequest(sourceDir.factory.getBackendApi(), outputPtp.getToken(), user);
			return false;
		}
		return true;
	}

	public static boolean doPutOverwrite(StormFileResource source, InputStream in) throws BadRequestException, ConflictException,
			NotAuthorizedException {
		log.info("Called doPutOverewrite()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		// prepare to put overwrite
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPutOverwrite(source.factory.getBackendApi(), source.getSurl(), user);
		log.debug(outputPtp.getStatus().getStatusCode().getValue());
		log.info(outputPtp.getStatus().getExplanation());
		if (!outputPtp.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			log.debug("PrepareToPutOverwrite has failed!");
			log.error("Failed to replace content: " + source.getSurl());
			return false;
		}
		try {
			// overwrite
			source.contentService.setFileContent(source.file, in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			abortRequest(source.factory.getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("Couldnt write to: " + source.file.getAbsolutePath(), ex);
		}
		// put done
		log.info("put done... ");
		SurlArrayRequestOutputData outputPd = StormBackendApi.putDone(source.factory.getBackendApi(), source.getSurl(), outputPtp.getToken(), user);
		log.debug(outputPd.getStatus().getStatusCode().getValue());
		log.info(outputPd.getStatus().getExplanation());
		if (!outputPd.isSuccess()) {
			log.debug("PutDone has failed!");
			log.error("Failed to replace content: " + source.getSurl());
			abortRequest(source.factory.getBackendApi(), outputPtp.getToken(), user);
			return false;
		}
		return true;
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source) {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		LsOutputData output = StormBackendApi.lsDetailed(source.factory.getBackendApi(), source.getSurl(), user);
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static boolean doPing() {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		log.info("ping " + Configuration.stormBackendHostname + ":" + Configuration.stormBackendPort);
		BackendApi backend = StormBackendApi.getBackend(Configuration.stormBackendHostname, Configuration.stormBackendPort);
		PingOutputData output = StormBackendApi.ping(backend, user);
		log.info(output.getBeOs());
		log.info(output.getBeVersion());
		log.info(output.getVersionInfo());
		return output.isSuccess();
	}

	public static boolean doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException {
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

	public static boolean doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		/* prepareToGet on soruce file to lock the resource */
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.factory.getBackendApi(), source.getSurl(), user);
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.info(outputPtG.getStatus().getExplanation());
		if (!outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			log.debug("PrepareToGet has failed!");
			return false;
		}
		/* create destination */
		if (!StormResourceHelper.doPut(newParent, newName, source.getInputStream())) {
			log.error("File copy has failed!");
			abortRequest(source.factory.getBackendApi(), outputPtG.getToken(), user);
			return false;
		}
		SurlArrayRequestOutputData output = StormBackendApi.releaseFile(source.factory.getBackendApi(), source.getSurl(), outputPtG.getToken(), user);
		log.debug(output.getStatus().getStatusCode().getValue());
		log.info(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			log.warn("ReleaseFiles has failed!");
			abortRequest(source.factory.getBackendApi(), outputPtG.getToken(), user);
		}
		return true;
	}

}
