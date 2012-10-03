package it.grid.storm.webdav.server;

import java.io.File;

import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.utils.FileUtils;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class WebDAVServer {
	
	//private static final Logger log = LoggerFactory.getLogger(WebDAVServer.class);
	private String webappsDirectory = "./webapps";
	
	private HttpServer httpServer;
	private HttpsServer httpsServer;
	
	public WebDAVServer(ServerInfo httpOptions, ServerInfo httpsOptions) {
		httpServer = new HttpServer(httpOptions);
		httpsServer = new HttpsServer(httpsOptions);
	}
	
	public String getWebappsDirectory() {
		return this.webappsDirectory;
	}
	
	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}
	
	public void start() throws ServerException {
		try {
			httpServer.start();
			httpsServer.start();
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void stop() throws ServerException {	
		try {
			httpServer.stop();
			httpsServer.stop();
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}
	
	public void status() {	
		httpServer.status();
		httpsServer.status();
	}
	
	private boolean isHttpDeployed(WebApp webapp) {
		return (httpServer.getWebapps().indexOf(webapp) != -1);
	}
	
	private boolean isHttpsDeployed(WebApp webapp) {
		return (httpsServer.getWebapps().indexOf(webapp) != -1);
	}
	
	public boolean isDeployed(WebApp webapp) {
		return (isHttpDeployed(webapp) || isHttpsDeployed(webapp));
	}

	public void deploy(WebApp webapp) throws ServerException {

		if (webapp.getProtocol() == StorageArea.HTTP_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			httpServer.deploy(webapp);
		}
		if (webapp.getProtocol() == StorageArea.HTTPS_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			httpsServer.deploy(webapp);
		}
	}

	public void undeploy(WebApp webapp) throws Exception {
		if (webapp.getProtocol() == StorageArea.HTTP_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			httpServer.undeploy(webapp);			
		}
		if (webapp.getProtocol() == StorageArea.HTTPS_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			httpsServer.undeploy(webapp);
		}
		FileUtils.deleteDirectory(new File(webapp.getFsPath()));
	}
	
	public void undeployAll() throws Exception {
		httpServer.undeployAll();
		httpsServer.undeployAll();
		FileUtils.deleteDirectory(new File(this.webappsDirectory));
	}
	

}