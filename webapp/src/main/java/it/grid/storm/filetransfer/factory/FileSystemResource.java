package it.grid.storm.filetransfer.factory;

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
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FileSystemResource implements Resource, DigestResource {

	private static final Logger log = LoggerFactory.getLogger(FileSystemResource.class);
	File file;
	final FileSystemResourceFactory factory;
	final String host;
	String ssoPrefix;
	StorageArea storageArea;
	Surl surl;

	public FileSystemResource(String host, FileSystemResourceFactory factory, File file, StorageArea storageArea) {
		this.host = host;
		this.file = file;
		this.factory = factory;
		this.storageArea = storageArea;
		this.surl = new Surl(this.file, this.storageArea);
	}
	
	public File getFile() {
		return file;
	}
	
	public Surl getSurl() {
		return surl;
	}
	
	public String getHost() {
		return host;
	}
	
	public FileSystemResourceFactory getFactory() {
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
	
	public StorageArea getStorageArea() {
		return storageArea;
	}

}
