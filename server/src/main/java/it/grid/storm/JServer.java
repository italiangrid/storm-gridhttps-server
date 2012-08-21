package org.example;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JServer {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static final int default_port = 8085;

	private int running_port;
	private Server server;
	private QueuedThreadPool threadPool;
	private Connector connector;
	private HandlerCollection hc;
	private ContextHandlerCollection contextHandlerCollection;

	public JServer() {
		this(JServer.default_port);
	}

	public JServer(int port) {
		server = new Server();
		this.setRunning_port(port);
		server.setStopAtShutdown(true);
		// server.setGracefulShutdown(2000);

		// Increase thread pool
		threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);

		// Ensure using the non-blocking connector (NIO)
		connector = new SelectChannelConnector();
		connector.setPort(this.getRunning_port());
		connector.setMaxIdleTime(30000);
		//server.setConnectors(new Connector[] { connector });

		SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
        ssl_connector.setPort(8443);
        ssl_connector.setKeystore("classes/keystore");
        ssl_connector.setKeyPassword("password");
		ssl_connector.setTrustPassword("password");
        
		server.setConnectors(new Connector[] { connector, ssl_connector });

		// Init collection of webapps' handlers
		hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);

	}

	public void start() throws Exception {
		server.start();
		log.info("SERVER JETTY STARTED ON " + this.getRunning_port());
	}

	public void stop() throws Exception {
		server.stop();
		log.info("SERVER JETTY STOPPED");
	}

	public void join() throws Exception {
		server.join();
	}

	public void deploy(String contextPath, String webappPath) throws Exception {

		log.info("DEPLOY WEBAPP {" + contextPath + ", " + webappPath
				+ "} ... STARTING");
		WebAppContext context = new WebAppContext();
		context.setDescriptor(webappPath + "/WEB-INF/web.xml");
		context.setResourceBase(webappPath);
		context.setContextPath(contextPath);
		context.setParentLoaderPriority(true);
		contextHandlerCollection.addHandler(context);
		context.start();
		log.info("DEPLOY WEBAPP {" + contextPath + ", " + webappPath
				+ "} ... DEPLOYED");

	}

	public void deployWar(String contextPath, String warPath) throws Exception {

		log.info("DEPLOY WEBAPP WAR FILE {" + contextPath + ", " + warPath
				+ "} ... STARTING");

		String outputDirectory = "/tmp/temp";
		log.info("Decompressing " + warPath + " on directory "
				+ outputDirectory);
		(new Zip()).unzip(warPath, outputDirectory);
		log.info("Decompressing OK");

		this.deploy(contextPath, outputDirectory);

		log.info("DEPLOY WEBAPP WAR FILE {" + contextPath + ", " + warPath
				+ "} ... DEPLOYED");

	}

	public int getRunning_port() {
		return running_port;
	}

	public void setRunning_port(int running_port) {
		this.running_port = running_port;
	}
}