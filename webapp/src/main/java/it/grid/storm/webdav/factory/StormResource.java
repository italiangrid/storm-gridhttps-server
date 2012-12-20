package it.grid.storm.webdav.factory;

import io.milton.http.*;
import io.milton.http.Request.Method;
import io.milton.http.http11.auth.DigestResponse;
import io.milton.resource.*;
import it.grid.storm.data.Surl;
import it.grid.storm.storagearea.StorageArea;

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
	private File file;
	private StormResourceFactory factory;
	private String host;
	private Surl surl;
	private StorageArea storageArea;
	String ssoPrefix;

	public StormResource(String host, StormResourceFactory factory, File file, StorageArea storageArea) {
		setHost(host);
		setFile(file);
		setFactory(factory);
		setStorageArea(storageArea);
		setSurl(new Surl(getFile(), getStorageArea()));
	}

	protected void setHost(String host) {
		this.host = host;
	}
	
	protected void setFactory(StormResourceFactory factory) {
		this.factory = factory;
	}

	protected void setSurl(Surl surl) {
		this.surl = surl;
	}

	protected void setStorageArea(StorageArea storageArea) {
		this.storageArea = storageArea;
	}

	protected void setFile(File newFile) {
		this.file = newFile;
	}
		
	public String getHost() {
		return host;
	}
	
	public File getFile() {
		return file;
	}

	public StormResourceFactory getFactory() {
		return factory;
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

	public Surl getSurl() {
		return surl;
	}
	
	public ArrayList<String> getSurlAsList() {
		ArrayList<String> surls = new ArrayList<String>();
		surls.add(getSurl().asString());
		return surls;
	}
	
	public InputStream getInputStream() {
		InputStream in;
		try {
			in = new FileInputStream(getFile());
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			return null;
		}
		return in;
	}
	
	public StorageArea getStorageArea() {
		return storageArea;
	}

}