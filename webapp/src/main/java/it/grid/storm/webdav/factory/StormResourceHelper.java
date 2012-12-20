package it.grid.storm.webdav.factory;

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
import it.grid.storm.HttpHelper;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.authorization.UserCredentials;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.backendApi.StormBackendApi;
import it.grid.storm.data.Surl;
import it.grid.storm.webdav.factory.StormDirectoryResource;
import it.grid.storm.webdav.factory.StormFileResource;
import it.grid.storm.webdav.factory.StormResource;
import it.grid.storm.webdav.factory.StormResourceHelper;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
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
		log.info("Aborting srm request...");
		StormBackendApi.abortRequest(backend, token, user);
	}

	public static void doMoveTo(StormResource source, StormResource newParent, String newName) throws NotAuthorizedException,
			ConflictException, BadRequestException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		doMoveTo(source, newParent, newName, user);
	}

	public static void doMoveTo(StormResource source, StormResource newParent, String newName, UserCredentials user)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doMoveTo()");
		String fromSurl = source.getSurl().asString();
		String toSurl = (new Surl(newParent.getSurl(), newName)).asString();		
		StormBackendApi.mv(source.getFactory().getBackendApi(), fromSurl, toSurl, user);
	}

	public static void doDelete(StormResource source) throws NotAuthorizedException, ConflictException, BadRequestException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		doDelete(source, user);
	}

	public static void doDelete(StormResource source, UserCredentials user) throws NotAuthorizedException, ConflictException,
			BadRequestException {
		log.info("Called doDelete()");
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doGetFile(source, user);
	}

	public static InputStream doGetFile(StormFileResource source, UserCredentials user) throws NotFoundException, RuntimeApiException,
			StormResourceException {
		log.info("Called doGetFile()");
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				source.getStorageArea().getProtocols());
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doMkCol(sourceDir, name, user);
	}

	public static StormDirectoryResource doMkCol(StormDirectoryResource sourceDir, String name, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.info("Called doMkCol()");
		File fsDest = new File(sourceDir.getFile(), name);
		StormDirectoryResource newDir = new StormDirectoryResource(sourceDir.getHost(), sourceDir.getFactory(), fsDest,
				sourceDir.getStorageArea());
		StormBackendApi.mkdir(sourceDir.getFactory().getBackendApi(), newDir.getSurl().asString(), user);
		return newDir;
	}

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in) throws RuntimeApiException,
			StormResourceException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doPut(sourceDir, name, in, user);
	}

	public static StormFileResource doPut(StormDirectoryResource sourceDir, String name, InputStream in, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.info("Called doPut()");
		File fsDest = new File(sourceDir.getFile(), name);
		StormFileResource srmDest = new StormFileResource(sourceDir.getHost(), sourceDir.getFactory(), fsDest, sourceDir.getStorageArea());
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPut(sourceDir.getFactory().getBackendApi(), srmDest.getSurl()
				.asString(), user, sourceDir.getStorageArea().getProtocols());
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doPutOverwrite(source, in, user);
	}

	public static StormFileResource doPutOverwrite(StormFileResource source, InputStream in, UserCredentials user) throws BadRequestException,
			ConflictException, NotAuthorizedException {
		log.info("Called doPutOverewrite()");
		FileTransferOutputData outputPtp = StormBackendApi.prepareToPutOverwrite(source.getFactory().getBackendApi(), source.getSurl()
				.asString(), user, source.getStorageArea().getProtocols());
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doLsDetailed(source, recursion, user);
	}

	public static ArrayList<SurlInfo> doLsDetailed(StormResource source, Recursion recursion, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.info("Called doLsDetailed()");
		LsOutputData output = StormBackendApi.lsDetailed(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				new RecursionLevel(recursion));
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static ArrayList<SurlInfo> doLs(StormResource source) throws RuntimeApiException, StormResourceException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doLs(source, user);
	}

	public static ArrayList<SurlInfo> doLs(StormResource source, UserCredentials user) throws RuntimeApiException, StormResourceException {
		log.info("Called doLs()");
		LsOutputData output = StormBackendApi.ls(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
		return (ArrayList<SurlInfo>) output.getInfos();
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort) throws RuntimeApiException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doPing(stormBackendHostname, stormBackendPort, user);
	}

	public static PingOutputData doPing(String stormBackendHostname, int stormBackendPort, UserCredentials user) throws RuntimeApiException {
		log.info("Called doPing()");
		BackendApi backend = StormBackendApi.getBackend(stormBackendHostname, stormBackendPort);
		return StormBackendApi.ping(backend, user);
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName)
			throws NotAuthorizedException, ConflictException, BadRequestException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		doCopyDirectory(sourceDir, newParent, newName, httpHelper.isDepthInfinity(), user);
	}

	public static void doCopyDirectory(StormDirectoryResource sourceDir, StormDirectoryResource newParent, String newName,
			boolean isDepthInfinity, UserCredentials user) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called doCopyDirectory()");
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		doCopyFile(source, newParent, newName, user);
	}

	public static void doCopyFile(StormFileResource source, StormDirectoryResource newParent, String newName, UserCredentials user)
			throws RuntimeApiException, StormResourceException {
		log.info("Called doCopyFile()");
		/* prepareToGet on source file to lock the resource */
		PtGOutputData outputPtG = StormBackendApi.prepareToGet(source.getFactory().getBackendApi(), source.getSurl().asString(), user,
				source.getStorageArea().getProtocols());
		try {
			/* create destination */
			StormResourceHelper.doPut(newParent, newName, source.getInputStream(), user);
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
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doPrepareToGetStatus(source, user);
	}

	public static RequestOutputData doPrepareToGetStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormResourceException {
		log.info("Called doPrepareToGetStatus()");
		return StormBackendApi.prepareToGetStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source) throws NotFoundException, RuntimeApiException,
			StormResourceException {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		UserCredentials user = new UserCredentials(httpHelper);
		return doPrepareToPutStatus(source, user);
	}

	public static RequestOutputData doPrepareToPutStatus(StormFileResource source, UserCredentials user) throws NotFoundException,
			RuntimeApiException, StormResourceException {
		log.info("Called doPrepareToPutStatus()");
		return StormBackendApi.prepareToPutStatus(source.getFactory().getBackendApi(), source.getSurl().asString(), user);
	}


}