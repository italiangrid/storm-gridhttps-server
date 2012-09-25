package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.utils.FileUtils;
import it.grid.storm.webdav.utils.XML;
import it.grid.storm.webdav.utils.Zip;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.SSLOptions;
import org.italiangrid.utils.https.ServerFactory;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDAVServer {
	
	private static final Logger log = LoggerFactory.getLogger(WebDAVServer.class);
	private String webappsDirectory = "./webapps";
	
	private ServerInfo httpOptions, httpsOptions;
	
	public WebDAVServer() {	
		initHttpServer();
		initHttpsServer();
	}
	
	private void initHttpServer() {
		
		httpOptions = new ServerInfo("HTTP-SERVER", 8085);
		httpOptions.setServer(new Server(httpOptions.getPort()));
		httpOptions.getServer().setStopAtShutdown(true);
		
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		httpOptions.getServer().setThreadPool(threadPool);
		
		HandlerCollection hc = new HandlerCollection();
		hc.setHandlers(new Handler[] { httpOptions.getContextHandlerCollection() });
		httpOptions.getServer().setHandler(hc);
		
		Connector connector = new SelectChannelConnector();
		connector.setPort(httpOptions.getPort());
		connector.setMaxIdleTime(30000);
		httpOptions.getServer().setConnectors(new Connector[] { connector });
		
	}

	private void initHttpsServer() {
	
		SSLOptions options = new SSLOptions();
		options.setCertificateFile("/etc/grid-security/hostcert.pem");
		options.setKeyFile("/etc/grid-security/hostkey.pem");
		options.setTrustStoreDirectory("/etc/grid-security/certificates");
		String hostname = "omii006-vm03.cnaf.infn.it";
		
		httpsOptions = new ServerInfo("HTTPS-SERVER", 8443);
		httpsOptions.setServer(ServerFactory.newServer(hostname, httpsOptions.getPort(), options));
		httpsOptions.getServer().setStopAtShutdown(true);
		
		HandlerCollection hc = new HandlerCollection();
		hc.setHandlers(new Handler[] { httpsOptions.getContextHandlerCollection() });
		httpsOptions.getServer().setHandler(hc);
		
	}
	
	public String getWebappsDirectory() {
		return this.webappsDirectory;
	}
	
	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}
	
	public void start() throws ServerException {
		try {
			httpOptions.getServer().start();
			log.info(httpOptions.getName() + " > STARTED on port " + httpOptions.getPort());
			httpsOptions.getServer().start();
			log.info(httpsOptions.getName() + " > STARTED on port " + httpsOptions.getPort());
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void stop() throws ServerException {	
		try {
			httpOptions.getServer().stop();
			log.info(httpOptions.getName() + " > STOPPED ");
			httpsOptions.getServer().stop();
			log.info(httpsOptions.getName() + " > STOPPED ");
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}
	
	private boolean isHttpDeployed(WebApp webapp) {
		return (httpOptions.getWebApps().indexOf(webapp) != -1);
	}
	
	private boolean isHttpsDeployed(WebApp webapp) {
		return (httpsOptions.getWebApps().indexOf(webapp) != -1);
	}
	
	public boolean isDeployed(WebApp webapp) {
		return (isHttpDeployed(webapp) || isHttpsDeployed(webapp));
	}

	private void doDeploy(ServerInfo options, WebApp webAppToDeploy) throws ServerException {
	
		String contextPath = webAppToDeploy.getContextPath();
		String webappPath = this.getWebappsDirectory() + contextPath;
		String contextFile = webappPath + "/WEB-INF/classes/applicationContext.xml";

		log.info("WEBDAV-SERVER > WEBAPP {" + contextPath + "} DEPLOY STARTED ...");
		
		
		try {
			// STEP 1: uncompress war file
			(new Zip()).unzip(webAppToDeploy.getWarFile(), webappPath);
		} catch (IOException e) {
			throw new ServerException(e.getMessage());
		}
		// STEP 2: modify application context file
		buildContextFile(contextFile, webAppToDeploy);
			
		if (webAppToDeploy.getProtocol() == StorageArea.HTTP_PROTOCOL || webAppToDeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			// STEP 3a: create and add to the server the WebAppContext handler for HTTP
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpOptions.getContextHandlerCollection().addHandler(context);
			try {
				context.start();
			} catch (Exception e) {
				throw new ServerException(e.getMessage());
			}
			httpOptions.webApps.add(webAppToDeploy);
			
			log.info(httpOptions.getName() + " > WEBAPP {" + contextPath + "} DEPLOYED! ");
			
		}
		
		if (webAppToDeploy.getProtocol() == StorageArea.HTTPS_PROTOCOL || webAppToDeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			
			// STEP 3b: create and add to the server the WebAppContext handler for HTTPS
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpsOptions.getContextHandlerCollection().addHandler(context);
			try {
				context.start();
			} catch (Exception e) {
				throw new ServerException(e.getMessage());
			}
			httpsOptions.webApps.add(webAppToDeploy);
			
			log.info(httpsOptions.getName() + " > WEBAPP {" + contextPath + "} DEPLOYED! ");
			
		}
		
		
	}
	
	public void deploy(WebApp webAppToDeploy) throws ServerException {

		if (webAppToDeploy == null)
			throw new ServerException("WEBDAV-SERVER > DEPLOY ERROR: webapp is null!");
		
		if (webAppToDeploy.getProtocol() == StorageArea.NONE_PROTOCOL) {
			log.info("WEBDAV-SERVER > webapp {" + webAppToDeploy.getContextPath() + "} has not to be deployed ");
			return;
		}
		
		if (isHttpDeployed(webAppToDeploy))
			throw new ServerException(httpOptions.getName() + " > DEPLOY ERROR: webapp already deployed!");
		doDeploy(httpOptions, webAppToDeploy);
		
	}

	
	
	
	
	
	
	
	public void undeployAll() throws ServerException {
		
		for (WebApp webapp : httpOptions.getWebApps()) {
			if (httpsOptions.getWebApps().contains(webapp)) {
				httpsOptions.getWebApps().remove(webapp);
			}
			doUndeploy(webapp);
		}
		for (WebApp webapp : httpsOptions.getWebApps()) {
			doUndeploy(webapp);
		}
		FileUtils.deleteDirectory(new File(this.getWebappsDirectory()));

	}

	public void undeploy(WebApp toUndeploy) throws ServerException {
		
		if (toUndeploy == null)
			throw new ServerException("WEBDAV-SERVER > UNDEPLOY ERROR: webapp is null!");
		
		if (toUndeploy.getProtocol() == StorageArea.NONE_PROTOCOL) {
			log.info("WEBDAV-SERVER > webapp {" + toUndeploy.getContextPath() + "} : nothing to undeploy ");
			return;
		}
		
		if (!isHttpDeployed(toUndeploy))
			throw new ServerException(httpOptions.getName() + " > UNDEPLOY ERROR: webapp already undeployed!");
		doUndeploy(toUndeploy);
		
	}
		
	private void doUndeploy(WebApp toUndeploy) throws ServerException {
		
		String contextPath = toUndeploy.getContextPath();
		String webappPath = this.getWebappsDirectory() + contextPath;

		if (toUndeploy.getProtocol() == StorageArea.HTTP_PROTOCOL || toUndeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpOptions.getContextHandlerCollection().removeHandler(context);
			
			httpOptions.getWebApps().remove(toUndeploy);

			log.info(httpOptions.getName() + " > WEBAPP {" + contextPath + "} UNDEPLOYED!");
		}
		
		if (toUndeploy.getProtocol() == StorageArea.HTTPS_PROTOCOL || toUndeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpsOptions.getContextHandlerCollection().removeHandler(context);
			httpsOptions.getWebApps().remove(toUndeploy);

			log.info(httpsOptions.getName() + " > WEBAPP {" + contextPath + "} UNDEPLOYED!");
			
		}
		
		FileUtils.deleteDirectory(new File(webappPath));

	}

	private void buildContextFile(String xmlfilesrc, WebApp webapp)
			throws ServerException {
		
		try {
			XML doc = new XML(xmlfilesrc);
			Element resourceFactory = doc.getNodeFromKeyValue("id", "milton.fs.resource.factory");
			Element rootNode = doc.getNodeFromKeyValue(resourceFactory, "name", "root");
			doc.setAttribute(rootNode, "value", webapp.getRootDirectory());
			Element contextPathNode = doc.getNodeFromKeyValue(resourceFactory, "name", "contextPath");
			doc.setAttribute(contextPathNode, "value", webapp.getContextPath().substring(1));
			doc.close();
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}
	
	
	
	private class ServerInfo {
		private int port;
		private String name;
		private Server server;
		private ContextHandlerCollection contextHandlerCollection;
		private List<WebApp> webApps = new ArrayList<WebApp>();
		
		public ServerInfo(String name, int port) {
			this.name = name;
			this.port = port;
			this.contextHandlerCollection = new ContextHandlerCollection();
		}
		
		public ContextHandlerCollection getContextHandlerCollection() {
			return contextHandlerCollection;
		}
		
//		public void setContextHandlerCollection(
//				ContextHandlerCollection contextHandlerCollection) {
//			this.contextHandlerCollection = contextHandlerCollection;
//		}
		
		public List<WebApp> getWebApps() {
			return webApps;
		}
		
//		public void setWebApps(List<WebApp> webApps) {
//			this.webApps = webApps;
//		}
		
		public int getPort() {
			return port;
		}
		
		public String getName() {
			return name;
		}
		
		public Server getServer() {
			return server;
		}
		
		public void setServer(Server server) {
			this.server = server;
		}
		
	}
	
	
	public class ServerException extends Exception {

		private static final long serialVersionUID = 1L;

		public ServerException(String description) {
			super(description);
		}
	}
	
	
	
	

}