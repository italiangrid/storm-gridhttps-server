package it.grid.storm.webdav;

import it.grid.storm.storagearea.StorageArea;

import java.io.File;
import java.io.IOException;

public class WebApp {

	private String warFile = "";
	private String rootDirectory = "";
	private String contextPath = "";

	public WebApp(String contextPath, String rootDirectory, String warFile) throws Exception, IOException {
		if (!(new File(warFile)).isFile())
			throw new IOException("template war file not found!");
		setWarFile(warFile);
		setRootDirectory(rootDirectory);
		setContextPath(contextPath);
	}
	
	public WebApp(StorageArea SA, String warFile) throws Exception, IOException {
		this(SA.getStfnRoot(), SA.getFSRoot(), warFile);
	}
	
	public String getContextPath() {
		return contextPath;
	}

	private void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getRootDirectory() {
		return rootDirectory;
	}
	
	private void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public String getWarFile() {
		return warFile;
	}

	private void setWarFile(String warFile) {
		this.warFile = warFile;
	}
	
}