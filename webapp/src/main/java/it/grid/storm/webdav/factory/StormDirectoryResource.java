package it.grid.storm.webdav.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.XmlWriter;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.resource.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.SizeUnit;
import it.grid.storm.srm.types.TFileType;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
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
	 * @throws StormResourceException 
	 * @throws RuntimeApiException 
	 */
	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException,
			NotAuthorizedException, RuntimeApiException, StormResourceException {
		log.info("Called function for GET DIRECTORY");
		
		String stfnRoot = "/" + StormHTTPHelper.getRequest().getRequestURI().replaceFirst("/", "").split("/")[0];
		String fsPath = StorageAreaManager.getInstance().getFsRootFromStfn().get(stfnRoot);
		String subpath = getFile().getCanonicalPath().substring(fsPath.length()).replace('\\', '/');
		String uri = stfnRoot + subpath;
		Collection<SurlInfo> entries = StormResourceHelper.doLsDetailed(this, Recursion.FULL).get(0).getSubpathInfo();
		buildDirectoryPage(out, uri, entries);
	}

	private void buildDirectoryPage(OutputStream out, String dirPath, Collection<SurlInfo> entries) throws RuntimeApiException, StormResourceException {
		XmlWriter w = new XmlWriter(out);
		w.open("html");
		w.open("head");
		w.begin("style").writeAtt("type", "text/css").open().writeText(getTableStyle()).close();
		w.close("head");
		w.open("body");
		w.begin("h1").open().writeText(this.getName()).close();
		w.open("table");
		w.open("tr");
		w.begin("td").open().begin("b").open().writeText("name").close().close();
		w.begin("td").open().begin("b").open().writeText("size").close().close();
		w.begin("td").open().begin("b").open().writeText("modified").close().close();
		w.begin("td").open().begin("b").open().writeText("checksum-type").close().close();
		w.begin("td").open().begin("b").open().writeText("checksum-value").close().close();
		w.close("tr");
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
		DecimalFormat decimalFormat = new DecimalFormat("#.##");
		for (SurlInfo entry : StormResourceHelper.doLsDetailed(this, Recursion.FULL).get(0).getSubpathInfo()) {
			w.open("tr");
			w.open("td");
			// entry name-link
			String name = entry.getStfn().split("/")[entry.getStfn().split("/").length - 1];
			String path = buildHref(dirPath, name);
			if (entry.getType().equals(TFileType.DIRECTORY))
				w.begin("img").writeAtt("alt", "").writeAtt("src", getFolderIco()).open().close();
			w.begin("a").writeAtt("href", path).open().writeText(name).close();
			w.close("td");
			// size
			w.begin("td").open().writeText(decimalFormat.format(entry.getSize().getSizeIn(SizeUnit.KILOBYTES)) + " KB").close();
			// modified date
			w.begin("td").open().writeText(dateFormat.format(entry.getModificationTime())).close();
			// checksum type
			String checksumType = entry.getCheckSumType() == null ? "" : entry.getCheckSumType().toString();
			w.begin("td").open().writeText(checksumType).close();
			// checksum value
			String checksumValue = entry.getCheckSumValue() == null ? "" : entry.getCheckSumValue().toString();
			w.begin("td").open().writeText(checksumValue).close();
			w.close("tr");
		}
		w.close("table");
		w.close("body");
		w.close("html");
		w.flush();
	}

	private String getTableStyle() {
		String out = "table {width: 100%; font-family: Arial,\"Bitstream Vera Sans\",Helvetica,Verdana,sans-serif; color: #333;}";
		out += "table td, table th {color: #555;}";
		out += "table th {text-shadow: rgba(255, 255, 255, 0.796875) 0px 1px 0px; font-family: Georgia,\"Times New Roman\",\"Bitstream Charter\",Times,serif; font-weight: normal; padding: 7px 7px 8px; text-align: left; line-height: 1.3em; font-size: 14px;}";
		out += "table td {font-size: 12px; padding: 4px 7px 2px; vertical-align: top; }";
		out += "img {margin-right: 5px; margin-top: 0; vertical-align: bottom; width: 12px; }";
		return out;
	}

	private String getFolderIco() {
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAAGXcA1uAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAACFklEQVR4nGL4//8/AwwDBBBDSkpKKRDvB2GAAAJxfIH4PwgDBBADsjKAAALJsMKUAQQQA0wJCAMEEIhTiCQwHYjngrQABBBIIgtZJQwDBBDYQCCjAIgbkLAmQACh2IiMAQII2cKtQDwbiDtAEgABBJNYB3MaFEcABBDMDgzLAQIIJKiPRaIOIIBgOpBd1AASAwggmEQ4SBU0tCJwuRSEAQIIpLgam91Y8CaQBoAAggVGCJGa/gMEEEhDCi4fYsGXAAII5odiIjVIAwQQTs/hwgABRLIGgAACOUcY6vEqIC4DYlF8GgACCJdnpwLxTGiimAPEFjANAAFEbOj8h2kACCCYhjwgTkdLUeh4A0gDQADBNGgRaVMpQACBNBwB4jYiNagDBBBIQwYQ3yLWHwABBNIgQIrHAQIIZ07Bgv+A1AIEEEyDUgpaZkHDebBgBQggkpMGQACRrIFUDBBAIOfLAvEbAv49B8Ss5FgAEEAgC+qIjQUi8F8gtke2ACCAQBZUICn4lwIpeX2AWAeINaC0FpQNwmpArIKEQRGoAMVyQCyJbAFAACGXFheAmAmIV1LRR+4AAQSyIBeIfwExHxDHUNFwEA4ACCCQBaBcPAWaXjdQ0fAfQMwCEEAgQxOB2AaIOYD4DxUtWAFyNEAAwSMDKBBJ5eAJAZkLEEDIFmgD8TcqGAxKqmUwcwECiOY5GSCAaG4BQIABAFbNMXYg1UnRAAAAAElFTkSuQmCC";
		return out;
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

	public Long getMaxAgeSeconds(Auth auth) {
		return null;
	}

	public String getContentType(String accepts) {
		return "text/html";
	}

	public Long getContentLength() {
		return null;
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