package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.common.ContentTypeUtils;
import io.milton.common.RangeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.fs.FileContentService;
import io.milton.resource.*;
import it.grid.storm.storagearea.StorageArea;

import java.io.*;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileResource extends FileSystemResource implements GetableResource, PropFindableResource, ReplaceableResource {

	private static final Logger log = LoggerFactory.getLogger(FileResource.class);

	final FileContentService contentService;

	public FileResource(String host, FileSystemResourceFactory fileSystemResourceFactory, File file, FileContentService contentService, StorageArea storageArea) {
		super(host, fileSystemResourceFactory, file, storageArea);
		this.contentService = contentService;
	}

	public Long getContentLength() {
		return file.length();
	}

	public String getContentType(String preferredList) {
		String mime = ContentTypeUtils.findContentTypes(this.file);
		String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
		if (log.isTraceEnabled()) {
			log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[] { preferredList, mime, s });
		}
		return s;
	}

	public String checkRedirect(Request arg0) {
		return null;
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, BadRequestException, NotFoundException {
		log.info("Called function for GET FILE");
		
//		log.debug("Check for a prepare-to-get");
//		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
//		UserCredentials user = new UserCredentials(httpHelper);
//		SurlArrayRequestOutputData outputSPtG = StormBackendApi.prepareToGetStatus(getFactory().getBackend(), getSurl().asString(), user);
//		if (!outputSPtG.isSuccess()) { //getStatus(getSurl().asString()).getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
//			log.warn("You must do a prepare-to-get on surl '" + getSurl().asString() + "' before!");
//			throw new NotAuthorizedException(this);
//		}
		
		InputStream in = FileSystemResourceHelper.doGetFile(this);
		if (in == null) {
			log.error("Unable to get resource content '" + this.file.toString() + "'");
			return;
		}
		if (range != null) {
			log.debug("sendContent: ranged content: " + file.getAbsolutePath());
			RangeUtils.writeRange(in, range, out);
		} else {
			log.debug("sendContent: send whole file " + file.getAbsolutePath());
			IOUtils.copy(in, out);
		}
		out.flush();
		IOUtils.closeQuietly(in);
	}

	public Long getMaxAgeSeconds(Auth auth) {
		return factory.maxAgeSeconds(this);
	}

	public String getName() {
		String name = super.getName();
		return name;
	}

	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.info("Called function for PUT-OVERWRITE");
		
//		log.debug("Check for a prepare-to-put");
//		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
//		UserCredentials user = new UserCredentials(httpHelper);
//		SurlArrayRequestOutputData outputSPtP = StormBackendApi.prepareToPutStatus(getFactory().getBackend(), getSurl().asString(), user);
//		if (!outputSPtP.isSuccess()) { //.getStatus(getSurl().asString()).getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
//			log.warn("You have to do a prepare to put on surl '" + getSurl().asString() + "' before!");
//			throw new NotAuthorizedException(this);
//		}
		
		FileSystemResourceHelper.doPutOverwrite(this, in);
	}

}
