package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.data.StormBackend;
import it.grid.storm.gridhttps.server.data.StormFrontend;
import it.grid.storm.gridhttps.server.data.StormGridhttps;
import it.grid.storm.gridhttps.server.utils.FileUtils;
import it.grid.storm.gridhttps.server.utils.Zip;

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
import org.italiangrid.utils.https.ServerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormGridhttpsServer {

	private static final int MAX_IDLE_TIME = 30000;

	private static final Logger log = LoggerFactory.getLogger(StormGridhttpsServer.class);
	private StormGridhttps gridhttpsInfo;
	private StormBackend backendInfo;
	private StormFrontend frontendInfo;
	private Server server;
	private ContextHandlerCollection contextHandlerCollection;
	private WebApp webapp;
	private MapperServlet mapperServlet;
	
	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws Exception {
		this.gridhttpsInfo = gridhttpsInfo;
		this.backendInfo = backendInfo;
		this.frontendInfo = frontendInfo;
		initServer();
	}

	private void initServer() throws Exception {
		createServer();
		createWebapp();
		deployWebapp();
		createMapperServlet();
		deployMapperServlet();
		
		
		log.debug("mapper-servlet deployed!");
		log.debug("server initialization - finished");
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
	
	private void createWebapp() throws Exception {
		webapp = new WebApp(new File(gridhttpsInfo.getWebappsDirectory(), DefaultConfiguration.WEBAPP_DIRECTORY_NAME));
		if (!webapp.getResourceBase().exists()) {
			if (webapp.getResourceBase().mkdirs()) {
				Zip.unzip(gridhttpsInfo.getWarFile().toString(), webapp.getResourceBase().toString());
				webapp.configureDescriptor(generateParams());
			} else {
				throw new Exception("Error on creation of '" + webapp.getResourceBase() + "' directory!");
			}
		} else {
			log.error(webapp.getResourceBase() + " already exists!");
		}
	}
	
	private void createMapperServlet() {
		mapperServlet = new MapperServlet();
	}
	
	private void deployWebapp() throws Exception {	
		if (webapp != null) {
			contextHandlerCollection.addHandler(webapp.getContext());
		} else {
			log.error("webapp not initialized - unable to deploy it!");
		}
	}

	private void deployMapperServlet() {	
		if (mapperServlet != null) {
			ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
			context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
			context.addServlet(new ServletHolder(mapperServlet), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
			contextHandlerCollection.addHandler(context);
		} else {
			log.error("mapperServlet not initialized - unable to deploy it!");
		}
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

	private void undeployWebapp() throws Exception {
		log.debug(" - undeploying webapp...");
		log.debug(" - removing context from handler collection...");
		contextHandlerCollection.removeHandler(webapp.getContext());
		FileUtils.deleteDirectory(webapp.getResourceBase());
	}

	public void undeployAll() throws Exception {
		undeployWebapp();
	}

	private Map<String,String> generateParams() {
		Map<String, String> params = new HashMap<String, String>();
		params.put("BACKEND_HOSTNAME", backendInfo.getHostname());
		params.put("BACKEND_PORT", String.valueOf(backendInfo.getPort()));
		params.put("BACKEND_SERVICE_PORT", String.valueOf(backendInfo.getServicePort()));
		params.put("FRONTEND_HOSTNAME", frontendInfo.getHostname());
		params.put("FRONTEND_PORT", String.valueOf(frontendInfo.getPort()));
		params.put("GPFS_ROOT_DIRECTORY", gridhttpsInfo.getRootDirectory().getAbsolutePath());
		params.put("WEBDAV_CONTEXTPATH", gridhttpsInfo.getWebdavContextPath());
		params.put("FILETRANSFER_CONTEXTPATH", gridhttpsInfo.getFiletransferContextPath());
		params.put("COMPUTE_CHECKSUM", String.valueOf(gridhttpsInfo.isComputeChecksum()));
		params.put("CHECKSUM_TYPE", gridhttpsInfo.getChecksumType());
		return params;
	}
	
}