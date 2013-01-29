package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import it.grid.storm.gridhttps.webapp.data.StormDirectoryResource;
import it.grid.storm.gridhttps.webapp.data.StormFactory;
import it.grid.storm.storagearea.StorageArea;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferDirectoryResource extends StormDirectoryResource {

	private static final Logger log = LoggerFactory.getLogger(FileTransferDirectoryResource.class);
	
	public FileTransferDirectoryResource(StormFactory factory, File dir, StorageArea storageArea) {
		super(factory, dir, storageArea);
		if (!dir.exists()) {
			throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + dir.getAbsolutePath());
		}
	}

	@Override
	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		log.debug("DirectoryResource PUT FILE disabled for fileTransfer");
		return null;
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException {
		log.debug("DirectoryResource GET DIRECTORY disabled for fileTransfer");
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