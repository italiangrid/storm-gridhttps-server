package it.grid.storm;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	private static final int default_port = 8085;
	
	private String webappsDirectory = "./webapps";
	private int running_port;
	
	private Server server;
	private QueuedThreadPool threadPool;
	private HandlerCollection hc;
	private ContextHandlerCollection contextHandlerCollection;
	
	
	public JServer() {
		this(JServer.default_port);
	}
	
	public JServer(int port) {
		server = new Server();
		this.setRunning_port(port);
		server.setStopAtShutdown(true);
		
		// Increase thread pool
		threadPool = new QueuedThreadPool();
		threadPool.setMaxThreads(100);
		server.setThreadPool(threadPool);
		
		// Init collection of webapps' handlers
		hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);	
	}

	public void initAsHttpServer() {
		Connector connector = new SelectChannelConnector();
		connector.setPort(this.getRunning_port());
		connector.setMaxIdleTime(30000);
		server.setConnectors(new Connector[] { connector });
		log.info("SERVER: initialized for working on HTTP");
	}
	
	public void initAsHttpsServer(String keystoreFilepath, String keystorePassword, String trustPassword) {
		SslSelectChannelConnector ssl_connector = new SslSelectChannelConnector();
        ssl_connector.setPort(this.getRunning_port());
        ssl_connector.setKeystore(keystoreFilepath);
        ssl_connector.setKeyPassword(keystorePassword);
		ssl_connector.setTrustPassword(trustPassword);
		ssl_connector.setAllowRenegotiate(false);
		server.setConnectors(new Connector[] { ssl_connector });
		log.info("SERVER: initialized for working on HTTPS");
	}
	
	public void start() throws Exception {
		if (server.getConnectors() == null)
			throw new Exception("server not initialized!");
		server.start();
		log.info("SERVER: I'm working on port " + this.getRunning_port());
		log.info("SERVER: STARTED");
	}

	public void stop() throws Exception {
		server.stop();
		log.info("SERVER: STOPPED");
	}

	public void join() throws Exception {
		server.join();
	}

	public void deploy(WebApp webAppToDeploy) throws IOException, Exception {

		if (!webAppToDeploy.isReadyToDeploy())
			throw new Exception("SERVER: the webapp " + webAppToDeploy.toString() + " is not ready to deploy!");
		
		String webAppPath = this.getWebappsDirectory() + "/" + webAppToDeploy.getName();
		
		log.info("SERVER-DEPLOY: decompressing webapp template file {" + webAppToDeploy.getWarFile() + "} into {"+ this.getWebappsDirectory() +"}");
		(new Zip()).unzip(webAppToDeploy.getWarFile(), webAppPath);
		log.info("SERVER-DEPLOY: decompressed! ");
	
		String applicationContextFile = webAppPath + "/WEB-INF/classes/applicationContext.xml";	
		log.info("SERVER-DEPLOY: root directory has to be modified ... ");
		this.setRootDir(applicationContextFile, webAppToDeploy.getRootDirectory());
		log.info("SERVER-DEPLOY: root directory fixed! ");
				
		WebAppContext context = new WebAppContext();
		String contextPath = "/" + webAppToDeploy.getName();
		context.setDescriptor(webAppPath + "/WEB-INF/web.xml");
		context.setResourceBase(webAppPath);
		context.setContextPath(contextPath);
		context.setParentLoaderPriority(true);		
		contextHandlerCollection.addHandler(context);
		context.start();
		
		log.info("SERVER-DEPLOY: WEBAPP {" + contextPath + ", " + webAppPath + "} ... DEPLOYED");

	}
	
	public int getRunning_port() {
		return running_port;
	}

	public void setRunning_port(int running_port) {
		this.running_port = running_port;
	}
	
	private void setRootDir(String xmlfilesrc, String rootdir) throws Exception {
		try {			
			System.out.println("xmlfilesrc: " + xmlfilesrc);
			SAXBuilder builder = new SAXBuilder();
			File xmlFile = new File(xmlfilesrc);
			Document doc = (Document) builder.build(xmlFile);

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
			if (!current.getAttributeValue("id").equals("milton.fs.resource.factory"))
				throw new Exception("node 'milton.fs.resource.factory' not found!");
			
			List<?> properties = current.getChildren();
			Element property = null;
			i = properties.iterator();
			while (i.hasNext()) {
				property = (Element) i.next();
				if (property.getAttributeValue("name").equals("root"))
					break;
			}
			if (!property.getAttributeValue("name").equals("root"))
				throw new Exception("attribute 'root' not found!");
			
			property.setAttribute("value", rootdir);

			XMLOutputter xmlOutput = new XMLOutputter();

			// display nice nice
			xmlOutput.setFormat(Format.getPrettyFormat());
			xmlOutput.output(doc, new FileWriter(xmlfilesrc));

		} catch (IOException io) {
			io.printStackTrace();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	public String getWebappsDirectory() {
		return webappsDirectory;
	}

	public void setWebappsDirectory(String webappsDirectory) {
		this.webappsDirectory = webappsDirectory;
	}
	
	public boolean deleteWebApps() {
		return deleteDirectory(new File(this.getWebappsDirectory()));
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