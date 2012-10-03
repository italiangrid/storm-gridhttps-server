
package it.grid.storm.webdav.webapp.factory;


import io.milton.common.ContentTypeUtils;
import io.milton.common.RangeUtils;
import io.milton.common.ReadingException;
import io.milton.common.WritingException;
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
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.*;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StormFileResource extends StormResource implements CopyableResource, DeletableResource, GetableResource, MoveableResource, PropFindableResource, ReplaceableResource {

    private static final Logger log = LoggerFactory.getLogger(StormFileResource.class);
    
    private final FileContentService contentService;

    /**
     *
     * @param host - the requested host. E.g. www.mycompany.com
     * @param stormResourceFactory
     * @param file
     */
    public StormFileResource(String host, StormResourceFactory fileSystemResourceFactory, File file, FileContentService contentService) {
        super(host, fileSystemResourceFactory, file);
        this.contentService = contentService;
    }

    public Long getContentLength() {
        return file.length();
    }

    public String getContentType(String preferredList) {
    	String mime = ContentTypeUtils.findContentTypes(this.file);
        String s = ContentTypeUtils.findAcceptableContentType(mime, preferredList);
        if (log.isTraceEnabled()) {
            log.trace("getContentType: preferred: {} mime: {} selected: {}", new Object[]{preferredList, mime, s});
        }
        return s;
    }

    public String checkRedirect(Request arg0) {
        return null;
    }

    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotFoundException {
    	log.info("Called function for GET FILE");
    	
    	StormResourceHelper helper = new StormResourceHelper(MiltonServlet.request(), this);
    	
    	//prepare to get
    	BackendApi be = helper.createBackend();

    	log.debug("prepare to get:");
    	
    	try {
			be.prepareToGet(helper.getUserDN(), helper.getUserFQANS(), helper.getSurls(),(String[])helper.getProtocols().toArray());
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}
    	
        InputStream in = null;
        try {
            in = contentService.getFileContent(file);
            if (range != null) {
                log.debug("sendContent: ranged content: " + file.getAbsolutePath());
                RangeUtils.writeRange(in, range, out);
            } else {
                log.debug("sendContent: send whole file " + file.getAbsolutePath());
                IOUtils.copy(in, out);
            }
            out.flush();
        } catch (FileNotFoundException e) {
            throw new NotFoundException("Couldnt locate content");
        } catch (ReadingException e) {
            throw new IOException(e);
        } catch (WritingException e) {
            throw new IOException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        
        // releaseFiles
    	try {
			be.releaseFiles(helper.getUserDN(), helper.getUserFQANS(), helper.getSurls(),null);
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}
        
    }

    /**
     * @{@inheritDoc}
     */
    public Long getMaxAgeSeconds(Auth auth) {
        return factory.maxAgeSeconds(this);
    }

    /**
     * @{@inheritDoc}
     */
    @Override
    protected void doCopy(File dest) {
        try {
            FileUtils.copyFile(file, dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed doing copy to: " + dest.getAbsolutePath(), ex);
        }
    }

    public String getName() {
    	String name = super.getName();
    	log.debug("StormFileResource.getName() = "+name);
		return name;
    }
    
	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
		try {
			contentService.setFileContent(file, in);
		} catch (IOException ex) {
			throw new BadRequestException("Couldnt write to: " + file.getAbsolutePath(), ex);
		}
	}
}
