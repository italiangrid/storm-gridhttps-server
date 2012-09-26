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
import org.italiangrid.utils.https.ServerFactory;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDAVServer {
	
	private static final Logger log = LoggerFactory.getLogger(WebDAVServer.class);
	private String webappsDirectory;
	
	private ServerInfo httpOptions, httpsOptions;
	private Server httpServer, httpsServer;
	private ContextHandlerCollection httpContext, httpsContext;
	private List<WebApp> httpWebApps, httpsWebApps;
	
	public WebDAVServer(ServerInfo httpOptions, ServerInfo httpsOptions) {
		log.info("HTTP-OPTIONS: " + httpOptions.toString());
		log.info("HTTPS-OPTIONS: " + httpsOptions.toString());
		this.httpOptions = httpOptions;
		this.httpsOptions = httpsOptions;
		this.initHttpServer();
		this.initHttpsServer();
	}
	
	private void initHttpServer() {
		
		httpServer = new Server(httpOptions.getPort());
		httpServer.setStopAtShutdown(true);
		
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		httpServer.setThreadPool(threadPool);
		
		HandlerCollection hc = new HandlerCollection();
		httpContext = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { httpContext });
		httpServer.setHandler(hc);
		
		Connector connector = new SelectChannelConnector();
		connector.setPort(httpOptions.getPort());
		connector.setMaxIdleTime(30000);
		httpServer.setConnectors(new Connector[] { connector });
		
		httpWebApps = new ArrayList<WebApp>();
	}

	private void initHttpsServer() {
				
		httpsServer = ServerFactory.newServer(httpsOptions.getHostname(), httpsOptions.getPort(), httpsOptions.getSslOptions());
		httpsServer.setStopAtShutdown(true);
		
		HandlerCollection hc = new HandlerCollection();
		httpsContext = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { httpsContext });
		httpsServer.setHandler(hc);
		
		httpsWebApps = new ArrayList<WebApp>();
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
			log.info(httpOptions.getName() + " > STARTED on port " + httpOptions.getPort());
			httpsServer.start();
			log.info(httpsOptions.getName() + " > STARTED on port " + httpsOptions.getPort());
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void stop() throws ServerException {	
		try {
			httpServer.stop();
			log.info(httpOptions.getName() + " > STOPPED ");
			httpsServer.stop();
			log.info(httpsOptions.getName() + " > STOPPED ");
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}
	
	private boolean isHttpDeployed(WebApp webapp) {
		return (httpWebApps.indexOf(webapp) != -1);
	}
	
	private boolean isHttpsDeployed(WebApp webapp) {
		return (httpsWebApps.indexOf(webapp) != -1);
	}
	
	public boolean isDeployed(WebApp webapp) {
		return (isHttpDeployed(webapp) || isHttpsDeployed(webapp));
	}

	private void doDeploy(WebApp webAppToDeploy) throws ServerException {
	
		String contextPath = webAppToDeploy.getContextPath();
		String webappPath = getWebappsDirectory() + contextPath;
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
			httpContext.addHandler(context);
			httpWebApps.add(webAppToDeploy);
			try {
				context.start();
			} catch (Exception e) {
				throw new ServerException(e.getMessage());
			}
			log.info(httpOptions.getName() + " > WEBAPP {" + contextPath + "} DEPLOYED! ");
		}
		
		if (webAppToDeploy.getProtocol() == StorageArea.HTTPS_PROTOCOL || webAppToDeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			
			// STEP 3b: create and add to the server the WebAppContext handler for HTTPS
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpsContext.addHandler(context);
			httpsWebApps.add(webAppToDeploy);			
			try {
				context.start();
			} catch (Exception e) {
				throw new ServerException(e.getMessage());
			}
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
		
		if (isDeployed(webAppToDeploy))
			throw new ServerException("WEBDAV-SERVER > DEPLOY ERROR: webapp already deployed!");
		doDeploy(webAppToDeploy);
		
	}

	public void undeployAll() throws ServerException {
		
		for (WebApp webapp : httpWebApps) {
			doUndeploy(webapp);
		}
		httpWebApps.clear();
		for (WebApp webapp : httpsWebApps) {
			doUndeploy(webapp);
		}
		httpsWebApps.clear();
		FileUtils.deleteDirectory(new File(getWebappsDirectory()));

	}

	public void undeploy(WebApp toUndeploy) throws ServerException {
		
		if (toUndeploy == null)
			throw new ServerException("WEBDAV-SERVER > UNDEPLOY ERROR: webapp is null!");
		
		if (toUndeploy.getProtocol() == StorageArea.NONE_PROTOCOL) {
			log.info("WEBDAV-SERVER > webapp {" + toUndeploy.getContextPath() + "} : nothing to undeploy ");
			return;
		}
		
		if (!isDeployed(toUndeploy))
			throw new ServerException("WEBDAV-SERVER > UNDEPLOY ERROR: webapp already undeployed!");
		doUndeploy(toUndeploy);
		
	}
		
	private void doUndeploy(WebApp toUndeploy) throws ServerException {
		
		String contextPath = toUndeploy.getContextPath();
		String webappPath = getWebappsDirectory() + contextPath;
		
		if (toUndeploy.getProtocol() == StorageArea.HTTP_PROTOCOL || toUndeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {
			
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpContext.removeHandler(context);
			log.info(httpOptions.getName() + " > WEBAPP {" + contextPath + "} UNDEPLOYED!");
		}
		
		if (toUndeploy.getProtocol() == StorageArea.HTTPS_PROTOCOL || toUndeploy.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			WebAppContext context = new WebAppContext();
			context.setDescriptor(webappPath + "/WEB-INF/web.xml");
			context.setResourceBase(webappPath);
			context.setContextPath(contextPath);
			context.setParentLoaderPriority(true);
			httpsContext.removeHandler(context);
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
	
	public class ServerException extends Exception {

		private static final long serialVersionUID = 1L;

		public ServerException(String description) {
			super(description);
		}
	}
	
	
	
	

}