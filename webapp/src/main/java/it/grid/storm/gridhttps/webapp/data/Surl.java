package it.grid.storm.gridhttps.webapp.data;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Surl {
	
	private static final Logger log = LoggerFactory.getLogger(Surl.class);
	
	private URI surl;
	private final String scheme = "srm";
	
	public Surl(String path) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), path);
	}
	
	public Surl(String feHostname, int fePort, String path) {
		buildURI(feHostname, fePort, path);
	}
	
	public Surl(String feHostname, int fePort, File resource) {
		StorageArea storageArea = StorageAreaManager.getMatchingSA(resource);
		buildURI(feHostname, fePort, resource.getPath().replaceFirst(storageArea.getFSRoot(), storageArea.getStfnRoot()));
	}
	
	public Surl(File resource) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), resource);
	}
	
	public Surl(String feHostname, int fePort, File resource, StorageArea storageArea) {
		buildURI(feHostname, fePort, resource.getPath().replaceFirst(storageArea.getFSRoot(), storageArea.getStfnRoot()));
	}
	
	public Surl(File resource, StorageArea storageArea) {
		this(Configuration.getFrontendHostname(), Configuration.getFrontendPort(), resource, storageArea);
	}
	
	public Surl(Surl baseSurl, String childName) {
		buildURI(baseSurl.asURI().getHost(), baseSurl.asURI().getPort(), baseSurl.asURI().getPath() + File.separator + childName);
	}
	
	private void buildURI(String feHostname, int fePort, String path) {
		try {
			surl = new URI(scheme, null, feHostname, fePort, path, null, null);
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
		}
	}
	
	public String asString() {
		return this.surl.toASCIIString();
	}
	
	public URI asURI() {
		return this.surl;
	}
	
}