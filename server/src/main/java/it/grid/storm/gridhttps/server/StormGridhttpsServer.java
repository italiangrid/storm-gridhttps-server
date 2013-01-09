package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.data.StormBackend;
import it.grid.storm.gridhttps.server.data.StormFrontend;
import it.grid.storm.gridhttps.server.data.StormGridhttps;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.utils.WebNamespaceContext;
import it.grid.storm.gridhttps.server.utils.XML;
import it.grid.storm.gridhttps.server.utils.Zip;

import java.io.File;
import java.io.IOException;
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
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.ServerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class StormGridhttpsServer {

	private static final int MAX_IDLE_TIME = 30000;

	private static final Logger log = LoggerFactory.getLogger(StormGridhttpsServer.class);
	private StormBackend backendInfo;
	private StormFrontend frontendInfo;
	private StormGridhttps gridhttpsInfo;
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private WebApp webapp;
	private MapperServlet mapperServlet;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws ServerException {
		setGridhttpsInfo(gridhttpsInfo);
		setBackendInfo(backendInfo);
		setFrontendInfo(frontendInfo);
		createServer();
		initServer();
	}

	private void createServer() {
		server = ServerFactory.newServer(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions());
		server.setStopAtShutdown(true);
		server.setGracefulShutdown(1000);
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		server.setHandler(hc);
		if (gridhttpsInfo.isEnabledHttp()) {
			Connector connector = new SelectChannelConnector();
			connector.setPort(gridhttpsInfo.getHttpPort());
			connector.setMaxIdleTime(MAX_IDLE_TIME);
			server.addConnector(connector);
		}
	}

	private void initServer() throws ServerException {
		initWebapp();
		initMapperServlet();
	}

	private void initWebapp() throws ServerException {
		webapp = new WebApp(new File(gridhttpsInfo.getWebappsDirectory(), DefaultConfiguration.WEBAPP_DIRECTORY_NAME));
		if (webapp != null) {
			if (!webapp.getResourceBase().exists()) {
				if (webapp.getResourceBase().mkdirs()) {
					try {
						Zip.unzip(gridhttpsInfo.getWarFile().toString(), webapp.getResourceBase().toString());
					} catch (IOException e) {
						throw new ServerException(e);
					}
					configureDescriptor(webapp.getDescriptorFile(), generateParams());
					contextHandlerCollection.addHandler(getWebappContext());
				} else {
					log.error("Error on creation of '" + webapp.getResourceBase() + "' directory!");
					throw new ServerException("Error on creation of '" + webapp.getResourceBase() + "' directory!");
				}
			} else {
				log.error("'" + webapp.getResourceBase() + "' already exists!");
				throw new ServerException("'" + webapp.getResourceBase() + "' already exists!");
			}
		} else {
			log.error("Error on webapp creation - webapp is null!");
			throw new ServerException("Error on webapp creation - webapp is null!");
		}
	}

	private void initMapperServlet() throws ServerException {
		mapperServlet = new MapperServlet();
		if (mapperServlet != null) {
			contextHandlerCollection.addHandler(getMapperServletContext());
		} else {
			log.error("Error on mapper-servlet creation - mapper-servlet is null!");
			throw new ServerException("Error on mapper-servlet creation - mapper-servlet is null!");
		}
	}

	private WebAppContext getWebappContext() {
		WebAppContext context = new WebAppContext();
		context.setDescriptor(webapp.getDescriptorFile().toString());
		context.setResourceBase(webapp.getResourceBase().getAbsolutePath());
		context.setParentLoaderPriority(true);
		return context;
	}

	private ServletContextHandler getMapperServletContext() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
		context.addServlet(new ServletHolder(mapperServlet), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
		return context;
	}

	public void start() throws ServerException {
		try {
			server.start();
		} catch (Exception e) {
			throw new ServerException(e);
		}
		log.info("server started ");
	}

	public boolean isRunning() {
		return server.isRunning();
	}

	public void stop() throws ServerException {
		if (isRunning()) {
			undeploy();
			try {
				server.stop();
			} catch (Exception e) {
				throw new ServerException(e);
			}
			log.info("server stopped ");
		}
	}

	public void status() {
		if (isRunning()) {
			if (gridhttpsInfo.isEnabledHttp())
				log.info("server is running on ports " + gridhttpsInfo.getHttpsPort() + "(https) and " +gridhttpsInfo.getHttpPort() + "(http)");
			else 
				log.info("server is running on port " + gridhttpsInfo.getHttpsPort() + "(https)");
		} else {
			log.info("server is not running ");
		}
	}

	private void undeploy() throws ServerException {
		log.debug(" - undeploying webapp...");
		contextHandlerCollection.removeHandler(getWebappContext());
		log.debug(" - undeploying mapper-servlet...");
		contextHandlerCollection.removeHandler(getMapperServletContext());
		log.debug(" - clearing webapp directory...");
	}

	private void configureDescriptor(File descriptorFile, Map<String, String> params) throws ServerException {
		String query = "/j2ee:web-app/j2ee:filter[@id='stormAuthorizationFilter']/j2ee:init-param/j2ee:param-value";
		try {
			XML doc = new XML(descriptorFile);
			NodeList initParams = doc.getNodes(query, new WebNamespaceContext(null));
			((Element) initParams.item(0)).setTextContent(JSON.toString(params));
			doc.save();
		} catch (Exception e) {
			throw new ServerException(e);
		}
	}

	private Map<String, String> generateParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("BACKEND_HOSTNAME", getBackendInfo().getHostname());
		params.put("BACKEND_PORT", String.valueOf(getBackendInfo().getPort()));
		params.put("BACKEND_SERVICE_PORT", String.valueOf(getBackendInfo().getServicePort()));
		params.put("FRONTEND_HOSTNAME", getFrontendInfo().getHostname());
		params.put("FRONTEND_PORT", String.valueOf(getFrontendInfo().getPort()));
		params.put("GPFS_ROOT_DIRECTORY", getGridhttpsInfo().getRootDirectory().getAbsolutePath());
		params.put("WEBDAV_CONTEXTPATH", getGridhttpsInfo().getWebdavContextPath());
		params.put("FILETRANSFER_CONTEXTPATH", getGridhttpsInfo().getFiletransferContextPath());
		params.put("COMPUTE_CHECKSUM", String.valueOf(getGridhttpsInfo().isComputeChecksum()));
		params.put("CHECKSUM_TYPE", getGridhttpsInfo().getChecksumType());
		return params;
	}

	private StormGridhttps getGridhttpsInfo() {
		return gridhttpsInfo;
	}

	private void setGridhttpsInfo(StormGridhttps gridhttpsInfo) {
		this.gridhttpsInfo = gridhttpsInfo;
	}

	private StormBackend getBackendInfo() {
		return backendInfo;
	}

	private void setBackendInfo(StormBackend backendInfo) {
		this.backendInfo = backendInfo;
	}

	private StormFrontend getFrontendInfo() {
		return frontendInfo;
	}

	private void setFrontendInfo(StormFrontend frontendInfo) {
		this.frontendInfo = frontendInfo;
	}

}