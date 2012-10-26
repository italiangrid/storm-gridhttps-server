package it.grid.storm.webdav.server;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import it.grid.storm.gridhttps.servlet.MapperServlet;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.webdav.utils.FileUtils;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebDAVServer {

	private static final Logger log = LoggerFactory.getLogger(WebDAVServer.class);
	private String webappsDirectory = "./webapps";
	private ServerInfo options;
	private String name = "WebDAV-SERVER";
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private List<WebApp> webapps;

	public WebDAVServer(ServerInfo options) {
		this.options = options;
		contextHandlerCollection = new ContextHandlerCollection();
		webapps = new ArrayList<WebApp>();
		initServer();
	}

	private void initServer() {
		
		// Server:
		server = ServerFactory.newServer(options.getHostname(), options.getHttpsPort(), options.getSslOptions());
		log.debug("httpsPort: " + options.getHttpsPort());
		server.setStopAtShutdown(true);
		
		if (options.isHttpEnabled()) {
			// Add HTTP Connector:
			Connector connector = new SelectChannelConnector();
			log.debug("httpPort: " + options.getHttpPort());
			connector.setPort(options.getHttpPort());
			connector.setMaxIdleTime(30000);
			server.addConnector(connector);
		}	
		
		// Handler:
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
		server.setGracefulShutdown(1000);
	}

	public String getWebappsDirectory() {
		return this.webappsDirectory;
	}

	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}

	public void start() throws Exception {
		server.start();
		log.info(name + " > STARTED");
	}

	public void stop() throws Exception {
		server.stop();
		log.info(name + " > STOPPED");
	}

	public void status() {
		if (!server.isStarted()) {
			log.info(name + " is not running ");
			return;
		}
		log.info(name + " is running on HTTPS on port " + options.getHttpsPort());
		if (options.isHttpEnabled())
			log.info(name + " supports HTTP connections on port " + options.getHttpPort());
		log.info(name + " has " + webapps.size() + " webapp(s) deployed ");
		for (WebApp w : webapps)
			log.info(" - '" + w.getContextPath() + "' (root = '" + w.getRootDirectory() + "')");
	}

	public boolean isDeployed(WebApp webapp) {
		return (webapps.indexOf(webapp) != -1);
	}

	public void deploy(WebApp webapp) throws Exception {

		if (webapp == null)
			throw new Exception("webapp is null!");
		if (isDeployed(webapp))
			throw new Exception("webapp already deployed!");
		if (webapp.getProtocol() == StorageArea.NONE_PROTOCOL)
			return;

		WebAppContext context = new WebAppContext();
		context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
		context.setResourceBase(webapp.getFsPath());
		context.setContextPath(webapp.getContextPath());
		context.setParentLoaderPriority(true);
		this.contextHandlerCollection.addHandler(context);
		webapps.add(webapp);
		log.info(name + " > DEPLOYED WEBAPP '" + webapp.getContextPath().substring(1) + "'");
	}
	
	public void deployGridHTTPs(String contextPath, String pathSpec) {
		log.debug("deploying gridhttps webapp...");
    
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(File.separator + contextPath);
        context.addServlet(new ServletHolder(new MapperServlet()), File.separator + pathSpec);
        contextHandlerCollection.addHandler(context);
        
		log.info(name + " > DEPLOYED GRIDHTTPS WEBAPP");
	}

	public void undeploy(WebApp webapp) throws Exception {
		
		if (webapp == null)
			throw new Exception("webapp is null!");
		if (!isDeployed(webapp))
			throw new Exception("webapp not deployed!");
		
		log.debug(name + ": undeploying '" + webapp.getContextPath() + "' webapp...");

		WebAppContext context = new WebAppContext();
		context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
		context.setResourceBase(webapp.getFsPath());
		context.setContextPath(webapp.getContextPath());
		context.setParentLoaderPriority(true);
		log.debug(name + ": removing context from handler collection...");
		this.contextHandlerCollection.removeHandler(context);
		webapps.remove(webapp);
		
		FileUtils.deleteDirectory(new File(webapp.getFsPath()));
	}

	public void undeployAll() throws Exception {
		while (!webapps.isEmpty())
			undeploy(webapps.get(0));
		FileUtils.deleteDirectory(new File(this.webappsDirectory));
	}

}