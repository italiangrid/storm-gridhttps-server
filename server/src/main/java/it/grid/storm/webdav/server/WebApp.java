package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.utils.XML;
import it.grid.storm.webdav.utils.Zip;

import java.io.File;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebApp {

	private static final Logger log = LoggerFactory.getLogger(WebApp.class);
	
	private String warFile;
	private String rootDirectory;
	private String contextPath;
	private String fsPath;
	private String contextFile;
	private int protocol = StorageArea.NONE_PROTOCOL;
	private String stormBEHostname;
	private int stormBEPort;

	public WebApp(String contextPath, String rootDirectory, String warFile,
			int protocol, String fsPath, String stormBEHostname, int stormBEPort) throws WebAppException {
				
		if (!(new File(warFile)).isFile())
			throw new WebAppException("template war file not found!");
		
		this.setWarFile(warFile);
		this.setRootDirectory(rootDirectory);
		this.setContextPath(contextPath);
		this.setProtocol(protocol);
		this.setFsPath(fsPath + contextPath);
		this.setContextFile(fsPath + contextPath + "/WEB-INF/classes/applicationContext.xml");
		this.setStormBEHostname(stormBEHostname);
		this.setStormBEPort(stormBEPort);
		
		// untar webapp war file
		try {
			log.debug("unzipping '" + warFile + "' into '" + this.getFsPath() + "'...");
			(new Zip()).unzip(warFile, this.getFsPath());
		} catch (Exception e) {
			throw new WebAppException(e.getMessage());
		}
		// modify application context file
		log.debug("customizing '" + this.getContextFile() + "'...");
		try {
			XML doc = new XML(contextFile);
			Element resourceFactory = doc.getNodeFromKeyValue("id", "milton.fs.resource.factory");
			// set root directory:
			log.debug("setting root directory as '" + rootDirectory + "'...");
			Element rootNode = doc.getNodeFromKeyValue(resourceFactory, "name", "root");
			doc.setAttribute(rootNode, "value", rootDirectory);
			// set context path:
			log.debug("setting context path as '" + contextPath.substring(1) + "'...");
			Element contextPathNode = doc.getNodeFromKeyValue(resourceFactory, "name", "contextPath");
			doc.setAttribute(contextPathNode, "value", contextPath.substring(1));
			// set backend hostname:
			log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
			Element stormBackendHostname = doc.getNodeFromKeyValue(resourceFactory, "name", "stormBackendHostname");
			doc.setAttribute(stormBackendHostname, "value", stormBEHostname);
			// set backend port:
			log.debug("setting storm backend port as '" + stormBEPort + "'...");
			Element stormBackendPort = doc.getNodeFromKeyValue(resourceFactory, "name", "stormBackendPort");
			doc.setAttribute(stormBackendPort, "value", String.valueOf(stormBEPort));
			doc.close();
		} catch (Exception e) {
			throw new WebAppException(e.getMessage());
		}
	}

	public WebApp(StorageArea SA, String warFile, String fsPath, String stormBEHostname, int stormBEPort) throws WebAppException {
		this(SA.getStfnRoot(), SA.getFSRoot(), warFile, SA.getProtocol(), fsPath, stormBEHostname, stormBEPort);
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
	
	public String getStormBEHostname() {
		return stormBEHostname;
	}
	
	public int getStormBEPort() {
		return stormBEPort;
	}
	
	public String getContextFile() {
		return contextFile;
	}

	private void setContextFile(String contextFile) {
		this.contextFile = contextFile;
		log.debug("contextFile = '" + contextFile +"'");
	}

	private void setWarFile(String warFile) {
		this.warFile = warFile;
		log.debug("warFile = '" + warFile +"'");
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

	private void setStormBEHostname(String stormBEHostname) {
		this.stormBEHostname = stormBEHostname;
		log.debug("stormBEHostname = '" + stormBEHostname +"'");
	}

	private void setStormBEPort(int stormBEPort) {
		this.stormBEPort = stormBEPort;
		log.debug("stormBEPort = '" + stormBEPort +"'");
	}

}