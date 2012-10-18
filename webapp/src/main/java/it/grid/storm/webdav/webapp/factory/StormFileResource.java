package it.grid.storm.webdav.webapp.factory;


import io.milton.common.ContentTypeUtils;
import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import io.milton.http.fs.FileContentService;
import io.milton.resource.*;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

import java.io.*;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
    	
    	String userDN = StormResourceHelper.getUserDN();
		ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
    	String surl = this.getSurl();
    	ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);
		
    	log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);

//    	//prepare to get
//    	try {
//    		log.debug("prepare to get");
//			this.factory.getBackendApi().prepareToGet(userDN, userFQANs, surl);
//		} catch (ApiException e) {
//			throw new IOException(e.getMessage());
//		}
//    	
//        InputStream in = null;
//        try {
//            in = contentService.getFileContent(file);
//            if (range != null) {
//                log.debug("sendContent: ranged content: " + file.getAbsolutePath());
//                RangeUtils.writeRange(in, range, out);
//            } else {
//                log.debug("sendContent: send whole file " + file.getAbsolutePath());
//                IOUtils.copy(in, out);
//            }
//            out.flush();
//        } catch (FileNotFoundException e) {
//            throw new NotFoundException("Couldn't locate content");
//        } catch (ReadingException e) {
//            throw new IOException(e);
//        } catch (WritingException e) {
//            throw new IOException(e);
//        } finally {
//            IOUtils.closeQuietly(in);
//        }
//        
//        // releaseFiles
//    	try {
//    		log.debug("release files");
//    		helper.getBackendApi().releaseFiles(userDN, userFQANs, helper.getSurls(), null);
//		} catch (ApiException e) {
//			throw new IOException(e.getMessage());
//		}
        
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
    	log.info("Called function for COPY FILE");
    	return;
//        try {
//            FileUtils.copyFile(file, dest);
//        } catch (IOException ex) {
//            throw new RuntimeException("Failed doing copy to: " + dest.getAbsolutePath(), ex);
//        }
    }

    public String getName() {
    	String name = super.getName();
		return name;
    }
    
	public void replaceContent(InputStream in, Long length) throws BadRequestException, ConflictException, NotAuthorizedException {
		try {
			contentService.setFileContent(file, in);
		} catch (IOException ex) {
			throw new BadRequestException("Couldnt write to: " + file.getAbsolutePath(), ex);
		}
	}

	public void delete(){
		log.info("Called function for DELETE FILE");
		
		String userDN = StormResourceHelper.getUserDN();
		ArrayList<String> userFQANs = StormResourceHelper.getUserFQANs();
		String surl = this.getSurl();
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(surl);
		
		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("surl = " + surl);
			    			
		try {
			log.info("delete file: " + file.toString());
			RequestOutputData output = this.factory.getBackendApi().rm(userDN, userFQANs, surls);
			log.info("success: " + output.isSuccess());
		} catch (ApiException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
}
