package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;

import java.io.File;

public class WebApp {

	private String warFile = "";
	private String rootDirectory = "";
	private String contextPath = "";
	private int protocol = StorageArea.NONE_PROTOCOL;

	public WebApp(String contextPath, String rootDirectory, String warFile,
			int protocol) throws WebAppException {
		
		if (!(new File(warFile)).isFile())
			throw new WebAppException("template war file not found!");
		
		setWarFile(warFile);
		setRootDirectory(rootDirectory);
		setContextPath(contextPath);
		setProtocol(protocol);
	}

	public WebApp(StorageArea SA, String warFile) throws WebAppException {
		this(SA.getStfnRoot(), SA.getFSRoot(), warFile, SA.getProtocol());
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

	public int getProtocol() {
		return protocol;
	}

	private void setProtocol(int protocol) {
		this.protocol = protocol;
	}

	public class WebAppException extends Exception {

		private static final long serialVersionUID = 1L;

		public WebAppException(String description) {
			super(description);
		}
	}

}