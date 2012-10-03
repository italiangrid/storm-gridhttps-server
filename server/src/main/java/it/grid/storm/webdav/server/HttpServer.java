package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {

	private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

	private boolean enabled;
	private String name;
	private int port;
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private List<WebApp> webapps;

	public HttpServer(ServerInfo options) {
		enabled = options.isEnabled();
		contextHandlerCollection = new ContextHandlerCollection();
		webapps = new ArrayList<WebApp>();
		if (!enabled) return;
		name = options.getName();
		port = options.getPort();
		initServer();
	}

	public String getName() {
		return name;
	}
	
	public int getPort() {
		return port;
	}

	public Server getServer() {
		return server;
	}

	public List<WebApp> getWebapps() {
		return webapps;
	}
	
	private void initServer() {
		// Server:
		server = new Server(port);
		server.setStopAtShutdown(true);
		// Server Thread pool:
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);
		// Server Handler:
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
		// Server Connector:
		Connector connector = new SelectChannelConnector();
		connector.setPort(port);
		connector.setMaxIdleTime(30000);
		server.setConnectors(new Connector[] { connector });
		server.setGracefulShutdown(1000);
	}

	public void start() throws ServerException {
		if (!enabled) return;
		try {
			server.start();
			log.info(name + " > STARTED on port " + port);
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void stop() throws ServerException {
		if (!enabled) return;
		try {
			server.stop();
			log.info(name + " > STOPPED");
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void status() {
		if (!enabled) return;
		if (!server.isStarted()) {
			log.info(name + " is not running ");
			return;
		}
		log.info(name + " is running on port " + port);
		if (webapps.size() == 0) {
			log.info(" - " + webapps.size() + " webapp(s) are deployed ");
			return;
		}
		log.info(" - " + webapps.size() + " webapp(s) are deployed for the following storage areas: ");
		for (WebApp w : webapps)
			log.info("    |- '" + w.getContextPath() + "' (root = '" + w.getRootDirectory() + "')");
	}
	
	private boolean isDeployed(WebApp webapp) {
		return (webapps.indexOf(webapp) != -1);
	}

	public void deploy(WebApp webapp) throws ServerException {
		if (!enabled) return;
		if (webapp == null)
			throw new ServerException("webapp is null!");
		if (isDeployed(webapp))
			throw new ServerException("webapp already deployed!");
		
		if (webapp.getProtocol() == StorageArea.HTTP_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			WebAppContext context = new WebAppContext();
			context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
			context.setResourceBase(webapp.getFsPath());
			context.setContextPath(webapp.getContextPath());
			context.setParentLoaderPriority(true);
			this.contextHandlerCollection.addHandler(context);
			webapps.add(webapp);
//			try {
//				context.start();
//			} catch (Exception e) {
//				throw new ServerException(e.getMessage());
//			}
			
			log.info(name + " > DEPLOYED WEBAPP '" + webapp.getContextPath().substring(1) + "'");
		}
	}

	public void undeploy(WebApp webapp) throws Exception {
		if (!enabled) return;
		if (webapp == null)
			throw new ServerException("webapp is null!");
		if (!isDeployed(webapp))
			throw new ServerException("webapp already undeployed!");
		
		if (webapp.getProtocol() == StorageArea.HTTP_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			log.debug(name + ": undeploying '" + webapp.getContextPath() + "' webapp...");
			
			WebAppContext context = new WebAppContext();
			context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
			context.setResourceBase(webapp.getFsPath());
			context.setContextPath(webapp.getContextPath());
			context.setParentLoaderPriority(true);
			log.debug(name + ": removing context from handler collection...");
			this.contextHandlerCollection.removeHandler(context);
			webapps.remove(webapp);
		}				
	}
	
	public void undeployAll() throws Exception {
		if (!enabled) return;
		while (!webapps.isEmpty())
			undeploy(webapps.get(0));
	}

}