package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileContentService;
import io.milton.resource.*;
import it.grid.storm.storagearea.StorageArea;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectoryResource extends FileSystemResource implements PutableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(DirectoryResource.class);

	final FileContentService contentService;
	
	public DirectoryResource(String host, FileSystemResourceFactory factory, File dir, FileContentService contentService, StorageArea storageArea) {
		super(host, factory, dir, storageArea);
		this.contentService = contentService;
		if (!dir.exists()) {
			throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + dir.getAbsolutePath());
		}
	}

	public Resource child(String name) {
		File fchild = new File(this.file, name);
		return factory.resolveFile(this.host, fchild, storageArea);
	}

	public List<? extends Resource> getChildren() {
		ArrayList<FileSystemResource> list = new ArrayList<FileSystemResource>();
		File[] files = this.file.listFiles();
		if (files != null) {
			for (File fchild : files) {
				FileSystemResource res = factory.resolveFile(this.host, fchild, storageArea);
				if (res != null) {
					list.add(res);
				} else {
					log.error("Couldnt resolve file {}", fchild.getAbsolutePath());
				}
			}
		}
		return list;
	}

	/**
	 * Will redirect if a default page has been specified on the factory
	 * 
	 * @param request
	 * @return
	 */
	public String checkRedirect(Request request) {
		if (factory.getDefaultPage() != null) {
			return request.getAbsoluteUrl() + "/" + factory.getDefaultPage();
		} else {
			return null;
		}
	}

	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException, NotAuthorizedException,
			ConflictException, BadRequestException {
		log.debug("DirectoryResource PUT FILE disabled for fileTransfer");
		return null;
	}

	/**
	 * Will generate a listing of the contents of this directory, unless the
	 * factory's allowDirectoryBrowsing has been set to false.
	 * 
	 * If so it will just output a message saying that access has been disabled.
	 * 
	 * @param out
	 * @param range
	 * @param params
	 * @param contentType
	 * @throws IOException
	 * @throws NotAuthorizedException
	 */
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

	public boolean hasChildren() {
		return (file.list() != null && file.list().length > 0);
	}
}