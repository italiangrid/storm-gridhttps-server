package it.grid.storm.webdav.server;

import it.grid.storm.gridhttps.servlet.MapperServlet;
import it.grid.storm.webdav.DefaultConfiguration;
import it.grid.storm.webdav.server.data.StormBackend;
import it.grid.storm.webdav.server.data.StormFrontend;
import it.grid.storm.webdav.server.data.StormGridhttps;
import it.grid.storm.webdav.utils.FileUtils;
import it.grid.storm.webdav.utils.WebNamespaceContext;
import it.grid.storm.webdav.utils.XML;
import it.grid.storm.webdav.utils.Zip;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ajax.JSON;
import org.italiangrid.utils.https.SSLOptions;
import org.italiangrid.utils.https.ServerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class StormGridhttpsServer {

	private static final int MAX_IDLE_TIME = 30000;

	private static final Logger log = LoggerFactory.getLogger(StormGridhttpsServer.class);
	private StormGridhttps gridhttpsInfo;
	private StormBackend backendInfo;
	private StormFrontend frontendInfo;
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private WebApp webapp;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws Exception {
		this.gridhttpsInfo = gridhttpsInfo;
		this.backendInfo = backendInfo;
		this.frontendInfo = frontendInfo;
		initServer();
	}

	private WebApp getWebapp() {
		return webapp;
	}

	private void setWebapp(WebApp webapp) {
		this.webapp = webapp;
	}

	private ContextHandlerCollection getContextHandlerCollection() {
		return contextHandlerCollection;
	}

	private void createServer(String hostname, int httpsPort, SSLOptions sslOptions) {
		server = ServerFactory.newServer(hostname, httpsPort, sslOptions);
		server.setStopAtShutdown(true);
		server.setGracefulShutdown(1000);
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
	}

	private Connector getHttpConnector(int port, int maxIdleTime) {
		Connector connector = new SelectChannelConnector();
		connector.setPort(port);
		connector.setMaxIdleTime(maxIdleTime);
		return connector;
	}

	private void initServer() throws Exception {
		log.debug("server initialization - started");
		createServer(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions());
		log.debug("jetty server with ssl-connector created");
		if (gridhttpsInfo.isEnabledHttp()) {
			server.addConnector(getHttpConnector(gridhttpsInfo.getHttpPort(), MAX_IDLE_TIME));
			log.debug("http-connector added");
		}
		setWebapp(new WebApp("webdav-webapp", new File(gridhttpsInfo.getWebappsDirectory(), DefaultConfiguration.WEBAPP_DIRECTORY_NAME)));
		log.debug("webapp created");
		getWebapp().getResourceBase().mkdirs();
		if (!getWebapp().getResourceBase().exists())
			throw new Exception("Error on creation of '" + getWebapp().getResourceBase() + "' directory!");
		log.debug("webapp base directory created");
		// deploy webdav-ft webapp
		Zip.unzip(gridhttpsInfo.getWarFile().toString(), getWebapp().getResourceBase().toString());
		log.debug("extracted " + gridhttpsInfo.getWarFile() + "' on '" + getWebapp().getResourceBase() + "'");
		configureWebFile(getWebapp().getDescriptorFile());
		log.debug("descriptor file " + getWebapp().getDescriptorFile() + " configured");
		getContextHandlerCollection().addHandler(getWebapp().getContext());
		log.debug(getWebapp().getName() + " has been deployed");
		// deploy mapper servlet
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
		context.addServlet(new ServletHolder(new MapperServlet()), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
		getContextHandlerCollection().addHandler(context);
		log.debug("mapper-servlet deployed!");
		log.debug("server initialization - finished");
	}

	public void start() throws Exception {
		server.start();
		log.info("server started ");
	}

	public void stop() throws Exception {
		server.stop();
		log.info("server stopped ");
	}

	public void status() {
		if (!server.isStarted()) {
			log.info("server is not running ");
			return;
		}
		log.info("server is running over ssl on port " + gridhttpsInfo.getHttpsPort());
		if (gridhttpsInfo.isEnabledHttp())
			log.info("server supports HTTP connections on port " + gridhttpsInfo.getHttpPort());
	}

	public boolean isWebappDeployed() {
		return getWebapp() != null;
	}

	private void undeployWebapp() throws Exception {
		if (!isWebappDeployed())
			throw new Exception("webapp is not deployed!");
		log.debug(" - undeploying '" + getWebapp().getName() + "' webapp...");
		log.debug(" - removing context from handler collection...");
		getContextHandlerCollection().removeHandler(getWebapp().getContext());
		FileUtils.deleteDirectory(getWebapp().getResourceBase());
	}

	public void undeployAll() throws Exception {
		undeployWebapp();
	}

	private Map<String,Object> generateParams() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("BACKEND_HOSTNAME", backendInfo.getHostname());
		params.put("BACKEND_PORT", backendInfo.getPort());
		params.put("BACKEND_SERVICE_PORT", backendInfo.getServicePort());
		params.put("FRONTEND_HOSTNAME", frontendInfo.getHostname());
		params.put("FRONTEND_PORT", frontendInfo.getPort());
		params.put("GPFS_ROOT_DIRECTORY", gridhttpsInfo.getRootDirectory());
		params.put("WEBDAV_CONTEXTPATH", gridhttpsInfo.getWebdavContextPath());
		params.put("FILETRANSFER_CONTEXTPATH", gridhttpsInfo.getFiletransferContextPath());
		params.put("COMPUTE_CHECKSUM", gridhttpsInfo.isComputeChecksum());
		params.put("CHECKSUM_TYPE", gridhttpsInfo.getChecksumType());
		return params;
	}
	
	private void configureWebFile(File webFile) throws Exception {
		XML doc = new XML(webFile);
		String query = "/j2ee:web-app/j2ee:filter[@id='stormAuthorizationFilter']/j2ee:init-param/j2ee:param-value";
		NodeList initParams = doc.getNodes(query, new WebNamespaceContext(null));
		String jsonStr = JSON.toString(generateParams());
		log.debug("json string: " + jsonStr);
		((Element) initParams.item(0)).setTextContent(jsonStr);
		doc.save();
	}

}