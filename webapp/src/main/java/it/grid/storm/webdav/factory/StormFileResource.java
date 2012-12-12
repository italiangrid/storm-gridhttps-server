package it.grid.storm.webdav.factory;

import io.milton.common.ContentTypeUtils;
import io.milton.common.RangeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.resource.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormFileResource extends StormResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource,
		PropFindableResource, ReplaceableResource {

	private static final Logger log = LoggerFactory.getLogger(StormFileResource.class);

	/**
	 * 
	 * @param host
	 *            - the requested host. E.g. www.mycompany.com
	 * @param stormResourceFactory
	 * @param file
	 */
	public StormFileResource(String host, StormResourceFactory fileSystemResourceFactory, File file) {
		super(host, fileSystemResourceFactory, file);
	}

	public SurlInfo getExtraProperties() {
		ArrayList<SurlInfo> info = null;
		try {
			info = StormResourceHelper.doLsDetailed(this, Recursion.NONE);
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		} catch (StormResourceException e) {
			log.error(e.getMessage());
		}
		return info != null ? info.get(0) : null;
	}

	public Long getContentLength() {
		return getFile().length();
	}

	public String getContentType(String preferredList) {
		String mime = ContentTypeUtils.findContentTypes(getFile());
		String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
		if (log.isTraceEnabled()) {
			log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[] { preferredList, mime, s });
		}
		return s;
	}

	public String checkRedirect(Request arg0) {
		return null;
	}

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
		log.info("Called function for GET FILE");
		InputStream in = StormResourceHelper.doGetFile(this);
		if (in == null) {
			log.error("Unable to get resource content '" + getFile().toString() + "'");
			return;
		}
		if (range != null) {
			log.debug("sendContent: ranged content: " + getFile().getAbsolutePath());
			RangeUtils.writeRange(in, range, out);
		} else {
			log.debug("sendContent: send whole file " + getFile().getAbsolutePath());
			IOUtils.copy(in, out);
		}
		out.flush();
		IOUtils.closeQuietly(in);
	}

	/**
	 * @{@inheritDoc
	 */
	public Long getMaxAgeSeconds(Auth auth) {
		return getFactory().maxAgeSeconds(this);
	}

	public String getName() {
		String name = super.getName();
		return name;
	}

	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.info("Called function for PUT-OVERWRITE");
		if (!StormHTTPHelper.isOverwriteRequest()) {
			throw new NotAuthorizedException("Resource exists but this is not an overwrite request!", this);
		}
		StormResourceHelper.doPutOverwrite(this, in);
	}

	public void delete() throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for DELETE FILE");
		StormResourceHelper.doDelete(this);
	}

	public void moveTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for MOVE FILE");
		if (newParent instanceof StormDirectoryResource) {
			StormResourceHelper.doMoveTo(this, (StormDirectoryResource) newParent, newName);
			setFile(new File(((StormDirectoryResource) newParent).getFile(), newName));
		} else
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
	}

	public void copyTo(CollectionResource newParent, String newName) throws NotAuthorizedException, ConflictException, BadRequestException {
		log.info("Called function for COPY FILE");		
		if (newParent instanceof StormDirectoryResource) {
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			StormResourceHelper.doCopyFile(this, newFsParent, newName);
		} else {
			log.error("Directory Resource class " + newParent.getClass().getName() + " not supported!");
		}
			
	}

}
