
package it.grid.storm.webdav.webapp.factory;


import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import java.io.File;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StormResource implements Resource, MoveableResource, CopyableResource, DigestResource {

    private static final Logger log = LoggerFactory.getLogger(StormResource.class);
    File file;
    final StormResourceFactory factory;
    final String host;
    String ssoPrefix;

    protected abstract void doCopy(File dest);

    public StormResource(String host, StormResourceFactory factory, File file) {
        this.host = host;
        this.file = file;
        this.factory = factory;
    }

    public File getFile() {
        return file;
    }

    public String getUniqueId() {
        String s = file.lastModified() + "_" + file.length() + "_" + file.getAbsolutePath();
        return s.hashCode() + "";
    }

    public String getName() {
        return file.getName();
    }

    public Object authenticate(String user, String password) {
        return factory.getSecurityManager().authenticate(user, password);
    }

    public Object authenticate(DigestResponse digestRequest) {
        return factory.getSecurityManager().authenticate(digestRequest);
    }

    public boolean isDigestAllowed() {
        return factory.isDigestAllowed();
    }

    public boolean authorise(Request request, Method method, Auth auth) {
        boolean b = factory.getSecurityManager().authorise(request, method, auth, this);
        if( log.isTraceEnabled()) {
            log.trace("authorise: result=" + b);
        }
        return b;
    }

    public String getRealm() {
        return factory.getRealm(this.host);
    }

    public Date getModifiedDate() {
        return new Date(file.lastModified());
    }

    public Date getCreateDate() {
        return null;
    }

    public int compareTo(Resource o) {
        return this.getName().compareTo(o.getName());
    }

    public void moveTo(CollectionResource newParent, String newName) {
    	log.info("Called function for MOVE FILE or DIRECTORY");
        if (newParent instanceof StormDirectoryResource) {
        	StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
            File dest = new File(newFsParent.getFile(), newName);
            boolean ok = this.file.renameTo(dest);
            if (!ok) {
                throw new RuntimeException("Failed to move to: " + dest.getAbsolutePath());
            }
            this.file = dest;
        } else {
            throw new RuntimeException("Destination is an unknown type. Must be a StormDirectoryResource, is a: " + newParent.getClass());
        }
    }

    public void copyTo(CollectionResource newParent, String newName) {
    	log.info("Called function for COPY FILE or DIRECTORY");
        if (newParent instanceof StormDirectoryResource) {
        	StormDirectoryResource newFsParent = (StormDirectoryResource) newParent;
            File dest = new File(newFsParent.getFile(), newName);
            doCopy(dest);
        } else {
            throw new RuntimeException("Destination is an unknown type. Must be a StormDirectoryResource, is a: " + newParent.getClass());
        }
    }

    public void delete() {
    	log.info("Called function for DELETE FILE or DIRECTORY");
        boolean ok = file.delete();
        if (!ok) {
            throw new RuntimeException("Failed to delete");
        }
    }

}
