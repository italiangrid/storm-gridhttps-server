package it.grid.storm.webdav.webapp.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.XmlWriter;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormDirectoryResource extends StormResource implements MakeCollectionableResource, PutableResource, CopyableResource,
		DeletableResource, MoveableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(StormDirectoryResource.class);

	public StormDirectoryResource(String host, StormResourceFactory factory, File dir) {
		super(host, factory, dir);
		if (!dir.exists()) {
			throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + dir.getAbsolutePath());
		}
	}

	public CollectionResource createCollection(String name) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for MKCOL DIRECTORY");

		String methodName = StormHTTPHelper.getRequestMethod();
		if (methodName.equals("PUT")) {
			/*
			 * it is a PUT with a path that contains a directory that does not
			 * exist, so send a 409 error to the client method can't be COPY
			 * because error 409 is handled by CopyHandler MOVE? Check if it can
			 * be a problem!!!
			 */
			log.warn("Auto-creation of directory for " + methodName + " requests is disabled!");
			StormHTTPHelper.sendError(409, "Conflict");
			return null;
		}
		StormResourceHelper.doMkCol(this, name);
		File fnew = new File(file, name);
		return new StormDirectoryResource(host, factory, fnew);
	}

	public Resource child(String name) {
		File fchild = new File(this.file, name);
		return factory.resolveFile(this.host, fchild);
	}

	public List<? extends Resource> getChildren() {
		ArrayList<StormResource> list = new ArrayList<StormResource>();
		File[] files = this.file.listFiles();
		if (files != null) {
			for (File fchild : files) {
				StormResource res = factory.resolveFile(this.host, fchild);
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
		log.info("Called function for PUT FILE");
		StormResourceHelper.doPut(this, name, in);
		File destinationFile = new File(this.file, name);
		return factory.resolveFile(this.host, destinationFile);
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
		log.info("Called function for GET DIRECTORY");
		String subpath = getFile().getCanonicalPath().substring(factory.getRoot().getCanonicalPath().length()).replace('\\', '/');
		String uri = "/" + factory.getContextPath() + subpath;
				
		XmlWriter w = new XmlWriter(out);
		w.open("html");
		w.open("head");
		w.close("head");
		w.open("body");
		w.begin("h1").open().writeText(this.getName()).close();
		w.open("table");
		for (SurlInfo entry : StormResourceHelper.doLsDetailed(this, Recursion.FULL).get(0).getSubpathInfo()) {
			w.open("tr");
			w.open("td");
			String name = entry.getStfn().split("/")[entry.getStfn().split("/").length-1];
			String checksumType = entry.getCheckSumType() == null ? "" : entry.getCheckSumType().toString() ;
			String checksumValue = entry.getCheckSumValue() == null ? "" : entry.getCheckSumValue().toString() ;
			String checksum = checksumType.isEmpty() && checksumValue.isEmpty() ? "" : "[checksum-type: " + checksumType + " checksum-value: " + checksumType + "]";
			String path = buildHref(uri, name);
			w.begin("a").writeAtt("href", path).open().writeText(name).close();
			w.close("td");
			w.begin("td").open().writeText(entry.getModificationTime() + checksum).close();
			w.close("tr");
		}			
		w.close("table");
		w.close("body");
		w.close("html");
		w.flush();
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

	private String buildHref(String uri, String name) {
		String abUrl = uri;
		if (!abUrl.endsWith("/")) {
			abUrl += "/";
		}
		if (ssoPrefix == null) {
			return abUrl + name;
		} else {
			// This is to match up with the prefix set on
			// SimpleSSOSessionProvider in MyCompanyDavServlet
			String s = insertSsoPrefix(abUrl, ssoPrefix);
			return s += name;
		}
	}

	public static String insertSsoPrefix(String abUrl, String prefix) {
		// need to insert the ssoPrefix immediately after the host and port
		int pos = abUrl.indexOf("/", 8);
		String s = abUrl.substring(0, pos) + "/" + prefix;
		s += abUrl.substring(pos);
		return s;
	}

	public boolean hasChildren() {
		return (file.list().length > 0);
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for DELETE DIRECTORY");
		StormResourceHelper.doDelete(this);
	}

	public void moveTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for MOVE DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			StormResourceHelper.doMoveTo(this, newFsParent, newName);
			file = newFsParent.file;
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for COPY DIRECTORY");
		if (newParent instanceof StormDirectoryResource) {
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			StormResourceHelper.doCopyDirectory(this, newFsParent, newName);
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

}
