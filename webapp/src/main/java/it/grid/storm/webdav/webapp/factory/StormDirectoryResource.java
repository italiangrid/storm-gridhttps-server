package it.grid.storm.webdav.webapp.factory;

import io.milton.http.Auth;
import io.milton.http.Range;
import io.milton.http.Request;
import io.milton.http.XmlWriter;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileContentService;
import io.milton.resource.*;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StormDirectoryResource extends StormResource implements MakeCollectionableResource, PutableResource, CopyableResource, DeletableResource, MoveableResource, PropFindableResource, GetableResource {

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
        File fnew = new File(file, name);
        boolean ok = fnew.mkdir();
        if (!ok) {
            throw new RuntimeException("Failed to create: " + fnew.getAbsolutePath());
        }
        return new StormDirectoryResource(host, factory, fnew, contentService);
    }

    public Resource child(String name) {
        File fchild = new File(file, name);
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
		
    	//prepare to put
    	String BEHostname = (String)MiltonServlet.request().getAttribute("STORM_BACKEND_HOST");
    	long BEPort = Integer.valueOf((String)MiltonServlet.request().getAttribute("STORM_BACKEND_PORT"));
    	String contextPath = (String)MiltonServlet.request().getAttribute("STORAGE_AREA_NAME");
    	BackendApi be;
    	try {
			be = new BackendApi(BEHostname, BEPort);
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}
    	List<String> surls = new ArrayList<String>();
    	String surl = "srm://"+BEHostname+":"+BEPort+"/"+contextPath+"/"+this.getFile().getPath();
    	surls.add(surl);
    	log.debug("prepare to put:");
    	log.debug(" # surl = " + surl);
    	
    	String userDN = (String)MiltonServlet.request().getAttribute("SUBJECT_DN");
    	log.debug(" # userDN = " + userDN);
    	
    	List<String> userFQANS = new ArrayList<String>();
    	
    	for (String s : (String[])MiltonServlet.request().getAttribute("FQANS"))
    		userFQANS.add(s);
    	log.debug(" # fqANs = ( " + userFQANS.toArray().toString() + ")");
    	
    	log.debug(" > prepareToPut("+userDN+","+userFQANS.toString()+","+surls.toString()+")");
//    	try {
//			be.prepareToPut(userDN, userFQANS, surls);
//		} catch (ApiException e) {
//			throw new IOException(e.getMessage());
//		}
    	
    	//put
    	File dest = new File(this.getFile(), name);
		contentService.setFileContent(dest, in);        
    	
		log.debug(" > putDone("+userDN+","+userFQANS.toString()+","+surls.toString()+", null)");
		//put done
//    	try {
//			be.putDone(userDN, userFQANS, surls, null);			
//		} catch (ApiException e) {
//			throw new IOException(e.getMessage());
//		}

    	return factory.resolveFile(this.host, dest);

    }

    @Override
    protected void doCopy(File dest) {
        try {
            FileUtils.copyDirectory(this.getFile(), dest);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to copy to:" + dest.getAbsolutePath(), ex);
        }
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
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType) throws IOException, NotAuthorizedException {
    	log.info("Called function for GET DIRECTORY");
    	String subpath = getFile().getCanonicalPath().substring(factory.getRoot().getCanonicalPath().length()).replace('\\', '/');
//        String uri = subpath;
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
            // This is to match up with the prefix set on SimpleSSOSessionProvider in MyCompanyDavServlet
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
}
