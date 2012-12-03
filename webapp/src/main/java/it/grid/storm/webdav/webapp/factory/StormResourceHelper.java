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
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.webdav.webapp.authorization.UserCredentials;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;
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

	public static void doMoveTo(StormResource source, StormResource newParent, String newName) throws NotAuthorizedException,
			ConflictException, BadRequestException {
		log.info("Called doMoveTo()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		String fromSurl = source.getSurl();
		String toSurl = newParent.getSurl() + "/" + newName;
		StormBackendApi.mv(source.factory.getBackendApi(), fromSurl, toSurl, user);
	}

	public static void doDelete(StormResource source) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doDelete()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		if (source instanceof StormDirectoryResource) { // DIRECTORY
			StormDirectoryResource sourceDir = (StormDirectoryResource) source;
			if (sourceDir.hasChildren()) {
				StormBackendApi.rmdirRecoursively(sourceDir.factory.getBackendApi(), sourceDir.getSurl(), user);
			} else {
				StormBackendApi.rmdir(sourceDir.factory.getBackendApi(), sourceDir.getSurl(), user);
			}
		} else { // FILE
			StormFileResource sourceFile = (StormFileResource) source;
			StormBackendApi.rm(sourceFile.factory.getBackendApi(), sourceFile.getSurl(), user);
		}
	}

	public static InputStream doGetFile(StormFileResource source) throws NotFoundException {
		log.info("Called doGetFile()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.factory.getBackendApi(), source.getSurl(), user);
		InputStream in = null;
		try {
			in = source.factory.getContentService().getFileContent(source.file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		StormBackendApi.releaseFile(source.factory.getBackendApi(), source.getSurl(), outputPtG.getToken(), user);
		return in;
	}

	public static void doMkCol(StormDirectoryResource sourceDir, String name) {
		log.info("Called doMkCol()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		String newDirSurl = sourceDir.getSurl() + "/" + name;
		StormBackendApi.mkdir(sourceDir.factory.getBackendApi(), newDirSurl, user);
	}

	public static void doPut(StormDirectoryResource sourceDir, String name, InputStream in) {
		log.info("Called doPut()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		File destinationFile = new File(sourceDir.file, name);
		String newFileSurl = sourceDir.getSurl() + "/" + name;
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPut(sourceDir.factory.getBackendApi(), newFileSurl, user);
		// put
		try {
			sourceDir.factory.getContentService().setFileContent(destinationFile, in);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.factory.getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("FileNotFoundException!", e);
		} catch (IOException e) {
			log.error(e.getMessage());
			abortRequest(sourceDir.factory.getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("IOException!", e);
		}
		StormBackendApi.putDone(sourceDir.factory.getBackendApi(), newFileSurl, outputPtp.getToken(), user);
	}

	public static void doPutOverwrite(StormFileResource source, InputStream in) throws BadRequestException, ConflictException,
			NotAuthorizedException {
		log.info("Called doPutOverewrite()");
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPutOverwrite(source.factory.getBackendApi(), source.getSurl(), user);
		// overwrite
		try {
			source.factory.getContentService().setFileContent(source.file, in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			abortRequest(source.factory.getBackendApi(), outputPtp.getToken(), user);
			throw new RuntimeException("Couldnt write to: " + source.file.getAbsolutePath(), ex);
		}
		StormBackendApi.putDone(source.factory.getBackendApi(), source.getSurl(), outputPtp.getToken(), user);
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion) {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		LsOutputData output = StormBackendApi.lsDetailed(source.factory.getBackendApi(), source.getSurl(), user, new RecursionLevel(
				recursion));
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static ArrayList<SurlInfo> doLs(StormResource source) {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		LsOutputData output = StormBackendApi.ls(source.factory.getBackendApi(), source.getSurl(), user);
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort) {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		BackendApi backend = StormBackendApi.getBackend(stormBackendHostname, stormBackendPort);
		return StormBackendApi.ping(backend, user);
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
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
				doCopyFile((StormFileResource) sr, destinationResource, rName);
			} else { // r is a Directory
				if (StormHTTPHelper.isDepthInfinity()) { // recursively...
					doCopyDirectory((StormDirectoryResource) sr, destinationResource, rName);
				} else { // not recursively...
					log.debug("copy '" + srcPath + "' to '" + destPath + "/" + rName + "'");
					destinationResource.createCollection(rName);
				}
			}
		}
	}

	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName) {
		UserCredentials user = new UserCredentials(StormHTTPHelper.getRequest());
		/* prepareToGet on source file to lock the resource */
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.factory.getBackendApi(), source.getSurl(), user);
		/* create destination */
		StormResourceHelper.doPut(newParent, newName, source.getInputStream());
		/* release source resource */
		try {
			StormBackendApi.releaseFile(source.factory.getBackendApi(), source.getSurl(), outputPtG.getToken(), user);
		} catch (RuntimeException e) {
			StormBackendApi.abortRequest(source.factory.getBackendApi(), outputPtG.getToken(), user);
			throw e;
		}
	}

}