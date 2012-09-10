package it.grid.storm;

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
		initAsHttpServer(port);
		log.info("SERVER: No keystore file defined or detected");
		log.info("SERVER: I'm working on HTTP");
	}

	public JServer(int port, String keystoreFilepath, String keystorePassword, String trustPassword) {
		initAsHttpsServer(port, keystoreFilepath, keystorePassword, trustPassword);
		log.info("SERVER: keystore file detected");
		log.info("SERVER: I'm working on HTTPS");
	}

	private void initAsHttpServer(int port) {
		server = new Server();
		this.setRunning_port(port);
		server.setStopAtShutdown(true);

		// Increase thread pool
		threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);

		// Ensure using the non-blocking connector (NIO)
		connector = new SelectChannelConnector();
		connector.setPort(this.getRunning_port());
		connector.setMaxIdleTime(30000);
		server.setConnectors(new Connector[] { connector });

		// Init collection of webapps' handlers
		hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
	}
	
	private void initAsHttpsServer(int port, String keystoreFilepath, String keystorePassword, String trustPassword) {
		server = new Server();
		this.setRunning_port(port);
		server.setStopAtShutdown(true);

		// Increase thread pool
		threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);

		SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
        ssl_connector.setPort(port);
        ssl_connector.setKeystore(keystoreFilepath);
        ssl_connector.setKeyPassword(keystorePassword);
		ssl_connector.setTrustPassword(trustPassword);
		server.setConnectors(new Connector[] { ssl_connector });
		
		// Init collection of webapps' handlers
		hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
	}
	
	public void start() throws Exception {
		server.start();
		log.info("SERVER: I'm working on port " + this.getRunning_port());
	}

	public void stop() throws Exception {
		server.stop();
		log.info("SERVER: STOPPED");
	}

	public void join() throws Exception {
		server.join();
	}

	public void deploy(String contextPath, String webappPath) throws Exception {

		log.info("SERVER: DEPLOY WEBAPP {" + contextPath + ", " + webappPath
				+ "} ... STARTED");
		WebAppContext context = new WebAppContext();
		context.setDescriptor(webappPath + "/WEB-INF/web.xml");
		context.setResourceBase(webappPath);
		context.setContextPath(contextPath);
		context.setParentLoaderPriority(true);
		contextHandlerCollection.addHandler(context);
		context.start();
		log.info("SERVER: DEPLOY WEBAPP {" + contextPath + ", " + webappPath
				+ "} ... DEPLOYED");

	}

	public void deployWar(String contextPath, String warPath) throws Exception {

		log.info("SERVER: DEPLOY WEBAPP WAR FILE {" + contextPath + ", " + warPath
				+ "} ... STARTED");

		String outputDirectory = "/tmp/temp";
		log.info("SERVER: Decompressing " + warPath + " on directory "
				+ outputDirectory);
		(new Zip()).unzip(warPath, outputDirectory);
		log.info("SERVER: Decompressing OK");

		this.deploy(contextPath, outputDirectory);

		log.info("SERVER: DEPLOY WEBAPP WAR FILE {" + contextPath + ", " + warPath
				+ "} ... DEPLOYED");

	}

	public int getRunning_port() {
		return running_port;
	}

	public void setRunning_port(int running_port) {
		this.running_port = running_port;
	}
}