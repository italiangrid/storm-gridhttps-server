package it.grid.storm.webdav.factory;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class StormResource implements Resource, MoveableResource, CopyableResource, DeletableResource, DigestResource {

	private static final Logger log = LoggerFactory.getLogger(StormResource.class);
	File file;
	final StormResourceFactory factory;
	final String host;
	String ssoPrefix;

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
		if (log.isTraceEnabled()) {
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

	public URI getSurl() {
		String stfnRoot = "/" + StormHTTPHelper.getRequest().getRequestURI().replaceFirst("/", "").split("/")[0];
		String fsRoot = StorageAreaManager.getInstance().getStfnToFsRoot().get(stfnRoot);
		String path = file.getPath().replaceFirst(fsRoot, stfnRoot);
		log.debug("path: " + path);
		URI surl = null;
		try {
			surl = new URI("srm", null, Configuration.stormFrontendHostname, Configuration.stormFrontendPort, path, null, null);			
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
		}
		log.debug("surl: " + surl.toASCIIString());
		return surl;
	}
	
	public ArrayList<String> getSurlAsList() {
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(getSurl().toASCIIString());
		return surls;
	}
	
	public InputStream getInputStream() {
		InputStream in;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			return null;
		}
		return in;
	}

}