package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebApp {

	private static final Logger log = LoggerFactory.getLogger(WebApp.class);
	
	private String rootDirectory;
	private String contextPath;
	private String fsPath;
	private int protocol = StorageArea.NONE_PROTOCOL;

	public WebApp(File baseDirectory, String contextPath, String rootDirectory, int protocol) throws Exception {
				
		if ((baseDirectory == null) || (!baseDirectory.exists()))
			throw new Exception("Directory '" + baseDirectory.getPath() + "' does not exist! ");
		
		this.setRootDirectory(rootDirectory);
		this.setContextPath(contextPath);
		this.setProtocol(protocol);
		this.setFsPath(baseDirectory.getPath());
		
	}

	public WebApp(File baseDirectory, StorageArea SA) throws Exception {
		this(baseDirectory, SA.getStfnRoot(), SA.getFSRoot(), SA.getProtocol());
	}

	public String getContextPath() {
		return contextPath;
	}

	public String getFsPath() {
		return fsPath;
	}

	public String getRootDirectory() {
		return rootDirectory;
	}
	
	public int getProtocol() {
		return protocol;
	}
	
	private void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
		log.debug("rootDirectory = '" + rootDirectory +"'");
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
		log.debug("contextPath = '" + contextPath +"'");
	}

	private void setFsPath(String fsPath) {
		this.fsPath = fsPath;
		log.debug("fsPath = '" + fsPath +"'");
	}

	private void setProtocol(int protocol) {
		this.protocol = protocol;
		log.debug("protocol = '" + StorageArea.protocolToStr(protocol) +"'");
	}
	
}