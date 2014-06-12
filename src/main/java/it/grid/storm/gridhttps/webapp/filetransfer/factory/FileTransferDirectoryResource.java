package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.webapp.common.factory.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.common.factory.StormFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferDirectoryResource extends StormDirectoryResource {

	private static final Logger log = LoggerFactory.getLogger(FileTransferDirectoryResource.class);

	public FileTransferDirectoryResource(StormFactory factory, StorageArea storageArea, File dir) {
		super(factory, storageArea, dir);
	}

	public FileTransferDirectoryResource(FileSystemResourceFactory factory, StorageArea storageArea, File dir) {
		super(factory, storageArea, dir);
	}

	@Override
	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		log.debug("DirectoryResource PUT new file disabled for fileTransfer");
		throw new NotAuthorizedException("Please perform an SRM prepareToPut request for such file before calling this method.", this);
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException {
		log.debug("DirectoryResource GET DIRECTORY disabled for fileTransfer");
		throw new NotAuthorizedException("The requested resource is not a file!", this);
	}

	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}

	public String getContentType(String accepts) {
		return "text/html";
	}

	public Long getContentLength() {
		return null;
	}

	@Override
	public void copyTo(CollectionResource toCollection, String name) throws NotAuthorizedException, BadRequestException, ConflictException {
		log.debug("DirectoryResource CopyTo DIRECTORY disabled for fileTransfer");
	}

	@Override
	public void moveTo(CollectionResource rDest, String name) throws ConflictException, NotAuthorizedException, BadRequestException {
		log.debug("DirectoryResource MoveTo DIRECTORY disabled for fileTransfer");
	}

}