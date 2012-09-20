package it.grid.storm.webdav;

import it.grid.storm.utils.Zip;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JServer {

	private static final Logger log = LoggerFactory.getLogger(JServer.class);
	private static final int defaultPort = 8085;
	private String webappsDirectory = "./webapps";
	
	private int runningPort;
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private List<WebApp> webApps = new ArrayList<WebApp>();

	public int getRunningPort() {
		return runningPort;
	}
	
	private void setRunningPort(int runningPort) {
		this.runningPort = runningPort;
	}

	public String getWebappsDirectory() {
		return webappsDirectory;
	}

	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}

	public ContextHandlerCollection getContextHandlerCollection() {
		return contextHandlerCollection;
	}

	public JServer() {
		this(JServer.defaultPort);
	}

	public JServer(int port) {
		server = new Server();
		setRunningPort(port);
		server.setStopAtShutdown(true);

		// Increase thread pool
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);

		// Initialize collection of web-applications' handlers
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
	}
		
	public void initAsHttpServer() {
		Connector connector = new SelectChannelConnector();
		connector.setPort(getRunningPort());
		connector.setMaxIdleTime(30000);
		server.setConnectors(new Connector[] { connector });
		log.info("SERVER: I'm working on HTTP");
	}

	public void initAsHttpsServer(String keystoreFilepath,
			String keystorePassword, String trustPassword) {
		SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
		ssl_connector.setPort(getRunningPort());
		ssl_connector.setKeystore(keystoreFilepath);
		ssl_connector.setKeyPassword(keystorePassword);
		ssl_connector.setTrustPassword(trustPassword);
		ssl_connector.setAllowRenegotiate(false);
		server.setConnectors(new Connector[] { ssl_connector });
		log.info("SERVER: I'm working on HTTPS");
	}

	public boolean isInitialized() {
		boolean isInit = true;
		isInit &= (server.getConnectors() != null);
		// isInit &= (webApps != null);
		return isInit;
	}

	public void start() throws Exception {
		if (!isInitialized())
			throw new Exception("server not initialized!");
		server.start();
		log.info("SERVER: STARTED on port " + this.getRunningPort());
	}

	public void stop() throws Exception {
		server.stop();
		log.info("SERVER: STOPPED");
	}

	public void join() throws Exception {
		server.join();
	}

	public boolean isDeployed(WebApp webapp) {
		return (webApps.indexOf(webapp) != -1);
	}

	public void deploy(WebApp webAppToDeploy) throws IOException, Exception {

		if (webAppToDeploy == null)
			throw new Exception("SERVER-DEPLOY: webapp is null!");
		if (isDeployed(webAppToDeploy))
			throw new Exception("SERVER-DEPLOY: webapp already deployed!");

		String contextPath = webAppToDeploy.getContextPath();
		String webappPath = getWebappsDirectory() + contextPath;
		String contextFile = webappPath
				+ "/WEB-INF/classes/applicationContext.xml";

		log.info("SERVER-DEPLOY: WEBAPP {" + contextPath + "} ... STARTED");

		log.info("SERVER-DEPLOY: decompressing template file {"
				+ webAppToDeploy.getWarFile() + "} into {"
				+ getWebappsDirectory() + "}");
		(new Zip()).unzip(webAppToDeploy.getWarFile(), webappPath);
		log.info("SERVER-DEPLOY: decompressed! ");

		buildContextFile(contextFile, webAppToDeploy);
		log.info("SERVER-DEPLOY: application context file fixed! ");

		WebAppContext context = new WebAppContext();
		context.setDescriptor(webappPath + "/WEB-INF/web.xml");
		context.setResourceBase(webappPath);
		context.setContextPath(contextPath);
		context.setParentLoaderPriority(true);
		getContextHandlerCollection().addHandler(context);
		context.start();

		log.info("SERVER-DEPLOY: WEBAPP {" + contextPath + "} ... DEPLOYED");

		this.webApps.add(webAppToDeploy);

	}

	public void undeployAll() throws Exception {
		while (!webApps.isEmpty())
			undeploy(webApps.get(0));
		deleteDirectory(new File(getWebappsDirectory()));
	}

	public void undeploy(WebApp toUndeploy) throws Exception {
		if (!isDeployed(toUndeploy))
			throw new Exception("undeploy webapp error: webapp not found!");

		String contextPath = toUndeploy.getContextPath();
		String webappPath = getWebappsDirectory() + contextPath;

		WebAppContext context = new WebAppContext();
		context.setDescriptor(webappPath + "/WEB-INF/web.xml");
		context.setResourceBase(webappPath);
		context.setContextPath(contextPath);
		context.setParentLoaderPriority(true);
		getContextHandlerCollection().removeHandler(context);

		deleteDirectory(new File(webappPath));
		webApps.remove(toUndeploy);

		log.info("SERVER-UNDEPLOY: WEBAPP {" + contextPath + "} ... UNDEPLOYED");

	}

	private void buildContextFile(String xmlfilesrc, WebApp webapp)
			throws Exception {
		try {
			SAXBuilder builder = new SAXBuilder();
			File xmlFile = new File(xmlfilesrc);
			Document doc = (Document) builder.build(xmlFile);

			/* find element with id = milton.fs.resource.factory */
			Element rootNode = doc.getRootElement();
			List<?> beans = rootNode.getChildren();
			Element current = null;
			Iterator<?> i = beans.iterator();
			while (i.hasNext()) {
				current = (Element) i.next();
				if (current.getAttributeValue("id").equals(
						"milton.fs.resource.factory"))
					break;
			}
			if (!current.getAttributeValue("id").equals(
					"milton.fs.resource.factory"))
				throw new Exception(
						"node 'milton.fs.resource.factory' not found!");

			/* find element with attribute name = root and set it with rootdir */
			List<?> properties = current.getChildren();
			Element property = null;
			i = properties.iterator();
			boolean foundRoot = false;
			boolean foundContextPath = false;
			while (i.hasNext()) {
				property = (Element) i.next();
				if (property.getAttributeValue("name").equals("root")) {
					property.setAttribute("value", webapp.getRootDirectory());
					foundRoot = true;
				} else if (property.getAttributeValue("name").equals(
						"contextPath")) {
					property.setAttribute("value", webapp.getContextPath()
							.substring(1));
					foundContextPath = true;
				}
			}
			if (!foundRoot)
				throw new Exception("attribute 'root' not found!");
			if (!foundContextPath)
				throw new Exception("attribute 'contextPath' not found!");

			/* output new file */
			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlfilesrc));

		} catch (IOException io) {
			io.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	private boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

}