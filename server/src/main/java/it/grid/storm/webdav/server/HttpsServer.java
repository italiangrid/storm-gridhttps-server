package it.grid.storm.webdav.server;

import it.grid.storm.webdav.storagearea.StorageArea;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.SSLOptions;
import org.italiangrid.utils.https.ServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpsServer {

	private static final Logger log = LoggerFactory.getLogger(HttpServer.class);

	private String name;
	private int port;
	private Server server;
	private SSLOptions sslOptions;
	private String hostname;
	private ContextHandlerCollection contextHandlerCollection;
	private List<WebApp> webapps;

	public HttpsServer(ServerInfo options) {
		name = options.getName();
		port = options.getPort();
		sslOptions = options.getSslOptions();
		hostname = options.getHostname();
		contextHandlerCollection = new ContextHandlerCollection();
		webapps = new ArrayList<WebApp>();
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
	
	public String getHostname() {
		return hostname;
	}

	public SSLOptions getSSLOptions() {
		return sslOptions;
	}

	private void initServer() {
		// Server:
		server = ServerFactory.newServer(hostname, port, sslOptions);
		server.setStopAtShutdown(true);
		// Handler:
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
	}

	public void start() throws ServerException {
		try {
			server.start();
			log.info(name + " > STARTED on port " + port);
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	public void stop() throws ServerException {
		try {
			server.stop();
			log.info(name + " > STOPPED");
		} catch (Exception e) {
			throw new ServerException(e.getMessage());
		}
	}

	private boolean isDeployed(WebApp webapp) {
		return (webapps.indexOf(webapp) != -1);
	}

	public void deploy(WebApp webapp) throws ServerException {

		if (webapp == null)
			throw new ServerException("webapp is null!");
		if (isDeployed(webapp))
			throw new ServerException("webapp already deployed!");
		
		if (webapp.getProtocol() == StorageArea.HTTPS_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			WebAppContext context = new WebAppContext();
			context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
			context.setResourceBase(webapp.getFsPath());
			context.setContextPath(webapp.getContextPath());
			context.setParentLoaderPriority(true);
			this.contextHandlerCollection.addHandler(context);
			webapps.add(webapp);
			try {
				context.start();
			} catch (Exception e) {
				throw new ServerException(e.getMessage());
			}
			
			log.info(name + " > DEPLOYED WEBAPP: " + webapp.toString());
		}
	}

	public void undeploy(WebApp webapp) throws ServerException {

		if (webapp == null)
			throw new ServerException("webapp is null!");
		if (!isDeployed(webapp))
			throw new ServerException("webapp already undeployed!");
		
		if (webapp.getProtocol() == StorageArea.HTTPS_PROTOCOL || webapp.getProtocol() == StorageArea.HTTP_AND_HTTPS_PROTOCOLS) {

			WebAppContext context = new WebAppContext();
			context.setDescriptor(webapp.getFsPath() + "/WEB-INF/web.xml");
			context.setResourceBase(webapp.getFsPath());
			context.setContextPath(webapp.getContextPath());
			context.setParentLoaderPriority(true);
			this.contextHandlerCollection.removeHandler(context);
		}				
	}

	public void undeployAll() throws ServerException {
		for (WebApp webapp : webapps)
			undeploy(webapp);
	}
	
}