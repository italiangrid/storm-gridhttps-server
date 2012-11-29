package it.grid.storm.webdav.webapp.factory;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.webdav.webapp.Configuration;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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

	public String getSurl() {
		String rootDir = factory.getRoot().getPath();
		String path = file.getPath().replaceFirst(rootDir, "/" + factory.getContextPath());
		String surl = "srm://" + Configuration.stormFrontendHostname + ":" + Configuration.stormFrontendPort + path;
		return surl;
	}
	
	public ArrayList<String> getSurlAsList() {
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(getSurl());
		return surls;
	}

	protected SurlInfo doLsDetailed() {	
		ArrayList<SurlInfo> infos = StormResourceHelper.doLsDetailed(this, Recursion.NONE);
		return infos != null ? infos.get(0) : null;
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
