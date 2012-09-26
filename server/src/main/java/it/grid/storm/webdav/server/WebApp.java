package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.utils.XML;
import it.grid.storm.webdav.utils.Zip;

import java.io.File;

import org.jdom.Element;

public class WebApp {

	private String warFile;
	private String rootDirectory;
	private String contextPath;
	private String fsPath;
	private String contextFile;
	private int protocol = StorageArea.NONE_PROTOCOL;

	public WebApp(String contextPath, String rootDirectory, String warFile,
			int protocol, String fsPath) throws WebAppException {
				
		if (!(new File(warFile)).isFile())
			throw new WebAppException("template war file not found!");
		
		this.warFile = warFile;
		this.rootDirectory = rootDirectory;
		this.contextPath = contextPath;
		this.protocol = protocol;
		this.fsPath = fsPath + contextPath;
		this.contextFile = this.fsPath + "/WEB-INF/classes/applicationContext.xml";
		
		// untar webapp war file
		try {
			(new Zip()).unzip(warFile, this.fsPath);
		} catch (Exception e) {
			throw new WebAppException(e.getMessage());
		}
		// modify application context file
		try {
			XML doc = new XML(contextFile);
			Element resourceFactory = doc.getNodeFromKeyValue("id", "milton.fs.resource.factory");
			Element rootNode = doc.getNodeFromKeyValue(resourceFactory, "name", "root");
			doc.setAttribute(rootNode, "value", rootDirectory);
			Element contextPathNode = doc.getNodeFromKeyValue(resourceFactory, "name", "contextPath");
			doc.setAttribute(contextPathNode, "value", contextPath.substring(1));
			doc.close();
		} catch (Exception e) {
			throw new WebAppException(e.getMessage());
		}
	}

	public WebApp(StorageArea SA, String warFile, String fsPath) throws WebAppException {
		this(SA.getStfnRoot(), SA.getFSRoot(), warFile, SA.getProtocol(), fsPath);
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

	public String getWarFile() {
		return warFile;
	}

	public int getProtocol() {
		return protocol;
	}

	public String toString() {
	
		String output = "(";
		output += "contextPath=" + contextPath;
		output += ",fsPath=" + fsPath;
		output += ",rootDirectory=" + rootDirectory;
		output += ",warFile=" + warFile;
		output += ",contextFile=" + contextFile;
		output += ",protocol=" + StorageArea.protocolToStr(protocol);
		return output + ")";
	}
	
	public class WebAppException extends Exception {

		private static final long serialVersionUID = 1L;

		public WebAppException(String description) {
			super(description);
		}
	}

}