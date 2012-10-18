package it.grid.storm.webdav.webapp.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.XmlWriter;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileContentService;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormDirectoryResource extends StormResource implements MakeCollectionableResource, PutableResource, CopyableResource,
		DeletableResource, MoveableResource, PropFindableResource, GetableResource {

	private static final Logger log = LoggerFactory.getLogger(StormDirectoryResource.class);

	private final FileContentService contentService;

	public StormDirectoryResource(String host, StormResourceFactory factory, File dir, FileContentService contentService) {
		super(host, factory, dir);
		this.contentService = contentService;
		if (!dir.exists()) {
			throw new IllegalArgumentException("Directory does not exist: " + dir.getAbsolutePath());
		}
		if (!dir.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + dir.getAbsolutePath());
		}
	}

	public CollectionResource createCollection(String name) {
		log.info("Called function for MKCOL DIRECTORY");

		File fnew = new File(this.file, name);

		/*
		 * if is called mkdir to create a new directory during a PUT request
		 * then abort it and send to the client an http response 409
		 */
		if (MiltonServlet.request().getMethod().toUpperCase().equals("PUT")) {
			log.warn("Auto-creation of directory for PUT requests is disabled!");
			try {
				MiltonServlet.response().sendError(409, "Absent father");
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;			
		}

		String userDN = StormResourceHelper.getUserDN();
		ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
		String surl = this.getSurl();
		String newDirSurl = surl + "/" + name;
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);

		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);
		log.debug("new directory surl = " + newDirSurl);

		try {
			// mkdir
			log.info("mkdir: " + newDirSurl);
			RequestOutputData output = this.factory.getBackendApi().mkdir(userDN, userFQANs, newDirSurl);
			log.info("success: " + output.isSuccess());
		} catch (ApiException e) {
			throw new RuntimeException(e.getMessage());
		}

		return new StormDirectoryResource(host, factory, fnew, contentService);
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

	public Resource createNew(String name, InputStream in, Long length, String contentType) throws IOException {
		log.info("Called function for PUT FILE");

		String userDN = StormResourceHelper.getUserDN();
		ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
		String surl = this.getSurl() + "/" + name;
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);

		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);

		FileTransferOutputData outputPtp;
		SurlArrayRequestOutputData outputPd;
		try {
			// prepare to put:
			log.info("prepare to put " + surl);
			outputPtp = this.factory.getBackendApi().prepareToPut(userDN, userFQANs, surl);
			log.info("success: " + outputPtp.isSuccess());
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}

		// put
		File dest = new File(this.getFile(), name);
		contentService.setFileContent(dest, in);

		try {
			// put done
			log.info("put done " + surl);
			outputPd = this.factory.getBackendApi().putDone(userDN, userFQANs, surls, outputPtp.getToken());
			log.info("success: " + outputPd.isSuccess());
			log.info("status: " + outputPd.getStatus().getExplanation());
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}

		return factory.resolveFile(this.host, dest);
	}

	@Override
	protected void doCopy(File dest) {
		log.info("Called function for COPY DIRECTORY");
		return;
		// try {
		// FileUtils.copyDirectory(this.getFile(), dest);
		// } catch (IOException ex) {
		// throw new RuntimeException("Failed to copy to:" +
		// dest.getAbsolutePath(), ex);
		// }
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
		for (Resource r : getChildren()) {
			w.open("tr");

			w.open("td");
			String path = buildHref(uri, r.getName());
			w.begin("a").writeAtt("href", path).open().writeText(r.getName()).close();

			w.close("td");

			w.begin("td").open().writeText(r.getModifiedDate() + "").close();
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

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for DELETE DIRECTORY");

		String userDN = (String) MiltonServlet.request().getAttribute("SUBJECT_DN");
		ArrayList<String> userFQANs = new ArrayList<String>();
		String[] fqansArr = StringUtils.split((String) MiltonServlet.request().getAttribute("FQANS"), ",");
		for (String s : fqansArr)
			userFQANs.add(s);
		String surl = this.getSurl();
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);

		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);

		try {
			log.info("delete directory: " + file.toString());
			RequestOutputData output;
			if (file.list().length == 0)
				output = this.factory.getBackendApi().rmdir(userDN, userFQANs, surl);
			else
				output = this.factory.getBackendApi().rmdirRecursively(userDN, userFQANs, surl);
			log.info("success: " + output.isSuccess());
		} catch (ApiException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
}
