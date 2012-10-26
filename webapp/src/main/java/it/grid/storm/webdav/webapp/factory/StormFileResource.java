package it.grid.storm.webdav.webapp.factory;

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
import io.milton.servlet.MiltonServlet;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormFileResource extends StormResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource,
		PropFindableResource, ReplaceableResource {

	private static final Logger log = LoggerFactory.getLogger(StormFileResource.class);

	final FileContentService contentService;

	/**
	 * 
	 * @param host
	 *            - the requested host. E.g. www.mycompany.com
	 * @param stormResourceFactory
	 * @param file
	 */
	public StormFileResource(String host, StormResourceFactory fileSystemResourceFactory, File file, FileContentService contentService) {
		super(host, fileSystemResourceFactory, file);
		this.contentService = contentService;
	}

	public String getChecksumType() {
		SurlInfo info = doLsDetailed();
		return info.getCheckSumType() == null ? "" : info.getCheckSumType().getValue();
	}

	public String getChecksumValue() {
		SurlInfo info = doLsDetailed();
		return info.getCheckSumValue() == null ? "" : info.getCheckSumValue().getValue();
	}

	public String getStatus() {
		SurlInfo info = doLsDetailed();
		return info.getStatus() == null ? "" : info.getStatus().getExplanation();
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

	public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException, BadRequestException, NotFoundException {
		log.info("Called function for GET FILE");
		InputStream in = StormResourceHelper.doGetFile(this);
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

	/**
	 * @{@inheritDoc
	 */
	public Long getMaxAgeSeconds(Auth auth) {
		return factory.maxAgeSeconds(this);
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
			StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
			File destinationFile = new File(newFsParent.getFile(), newName);
			StormResourceHelper.doMoveTo(this, newFsParent, newName);
			file = destinationFile;
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
//			log.warn("BETA REMOTE COPY");
//			String destinationStr = MiltonServlet.request().getHeader("Destination");
//			
//			URL url = null;
//			try {
//				url = new URL(destinationStr);
//			} catch (MalformedURLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			HttpURLConnection httpCon = null;
//			try {
//				httpCon = (HttpURLConnection) url.openConnection();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			httpCon.setDoOutput(true);
//			try {
//				httpCon.setRequestMethod("PUT");
//			} catch (ProtocolException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			InputStream in = null;
//			try {
//				in = StormResourceHelper.doGetFile(this);
//			} catch (NotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			OutputStreamWriter out = null;
//			try {
//				out = new OutputStreamWriter(httpCon.getOutputStream());
//				IOUtils.copy(in, out);
//				out.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			
//			DefaultHttpClient client = new DefaultHttpClient();
//			URI uri = URI.create(destinationStr);
//	        HttpPut p = new HttpPut(uri);
//	        byte[] bytes = null;
//			try {
//				bytes = IOUtils.toByteArray(this.getInputStream());
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//            ByteArrayEntity requestEntity = new ByteArrayEntity(bytes);
//            p.setEntity(requestEntity);
//            HttpResponse response = null;
//			try {
//				response = client.execute(p);
//			} catch (ClientProtocolException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}            
//            StatusLine line = response.getStatusLine();
//            log.info("complete: " + line);
//            // return code indicates upload failed so throw exception
//            if( line.getStatusCode() < 200 || line.getStatusCode() >= 300 ) {
//                log.error( "Failed upload" );
//            }
//            // shut down connection
//            client.getConnectionManager().shutdown();
			
			
		}
			
	}

}
