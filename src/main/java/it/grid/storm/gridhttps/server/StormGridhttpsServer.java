/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.data.StormBackend;
import it.grid.storm.gridhttps.server.data.StormFrontend;
import it.grid.storm.gridhttps.server.data.StormGridhttps;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.mapperservlet.MapperServlet;
import it.grid.storm.gridhttps.server.utils.WebNamespaceContext;
import it.grid.storm.gridhttps.server.utils.XML;
import it.grid.storm.gridhttps.server.utils.Zip;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

//import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
//import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
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
	// private Server davServer;
	// private Server mapServer;
	private Server oneServer;
//	private ContextHandlerCollection contextHandlerCollection;
	private WebApp webapp;
	private MapperServlet mapperServlet;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws ServerException {
		setGridhttpsInfo(gridhttpsInfo);
		setBackendInfo(backendInfo);
		setFrontendInfo(frontendInfo);
		// createDavServer();
		// createMapServer();
		createServer();
//		initDavServer();
//		initMapServer();
		// initServer();
	}

	private void createServer() throws ServerException {
		oneServer = ServerFactory.newServer(getGridhttpsInfo().getHostname(), getGridhttpsInfo().getHttpsPort(), getGridhttpsInfo()
				.getSsloptions());
		oneServer.setStopAtShutdown(true);
		oneServer.setGracefulShutdown(1000);

		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxIdleTimeMs(20000);
		threadPool.setMaxThreads(getGridhttpsInfo().getDavActiveThreadsMax());
		threadPool.setMaxQueued(getGridhttpsInfo().getDavQueuedThreadsMax());
		oneServer.setThreadPool(threadPool);

		/* add plain HTTP connector if enabled */
		if (getGridhttpsInfo().isEnabledHttp()) {
			Connector connector = new SelectChannelConnector();
			connector.setPort(getGridhttpsInfo().getHttpPort());
			connector.setMaxIdleTime(MAX_IDLE_TIME);
			connector.setHost(getGridhttpsInfo().getHostname());
			oneServer.addConnector(connector);
		}

		/* add MapperServlet connector */
		Connector connector2 = new SelectChannelConnector();
		connector2.setPort(getGridhttpsInfo().getMapperServlet().getPort());
		connector2.setMaxIdleTime(MAX_IDLE_TIME);
		connector2.setHost(getGridhttpsInfo().getHostname());
		oneServer.addConnector(connector2);
		
		for (Connector conn : oneServer.getConnectors())
			log.info("connector-name: " + conn.getName());
		
//		HandlerCollection hc = new HandlerCollection();
//		contextHandlerCollection = new ContextHandlerCollection();
//		hc.setHandlers(new Handler[] { contextHandlerCollection });
//		oneServer.setHandler(hc);
		
		String[] mappingConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getMapperServlet().getPort() };
		mapperServlet = new MapperServlet();
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
		context.addServlet(new ServletHolder(mapperServlet), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
		context.setConnectorNames(mappingConnectors);
		
		initWebapp();
		
		String[] webappConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpPort(),
				getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpsPort() };
		log.info("webapp-connectors: " + webappConnectors[0] + ", " + webappConnectors[1]);
		WebAppContext waContext = new WebAppContext();
		waContext.setDescriptor(webapp.getDescriptorFile().toString());
		waContext.setResourceBase(webapp.getResourceBase().getAbsolutePath());
		waContext.setConnectorNames(webappConnectors);
		waContext.setParentLoaderPriority(true);
		waContext.setContextPath("/");
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { context, waContext });
 
        oneServer.setHandler(contexts);
		
		
	}

	// private void createMapServer() {
	// mapServer = new Server(gridhttpsInfo.getMapperServlet().getPort());
	// mapServer.setStopAtShutdown(true);
	// mapServer.setGracefulShutdown(1000);
	//
	// QueuedThreadPool threadPool = new QueuedThreadPool();
	// threadPool.setMaxIdleTimeMs(20000);
	// threadPool.setMinThreads(gridhttpsInfo.getMapActiveThreadsMin());
	// threadPool.setMaxThreads(gridhttpsInfo.getMapActiveThreadsMax());
	// threadPool.setMaxQueued(gridhttpsInfo.getMapQueuedThreadsMax());
	// mapServer.setThreadPool(threadPool);
	// }

	// private void createDavServer() {
	// davServer = ServerFactory.newServer(gridhttpsInfo.getHostname(),
	// gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions());
	// davServer.setStopAtShutdown(true);
	// davServer.setGracefulShutdown(1000);
	//
	// QueuedThreadPool threadPool = new QueuedThreadPool();
	// threadPool.setMaxIdleTimeMs(20000);
	// threadPool.setMaxThreads(gridhttpsInfo.getDavActiveThreadsMax());
	// threadPool.setMaxQueued(gridhttpsInfo.getDavQueuedThreadsMax());
	// davServer.setThreadPool(threadPool);
	//
	// HandlerCollection hc = new HandlerCollection();
	// contextHandlerCollection = new ContextHandlerCollection();
	// hc.setHandlers(new Handler[] { contextHandlerCollection });
	// davServer.setHandler(hc);
	// if (gridhttpsInfo.isEnabledHttp()) {
	// Connector connector = new SelectChannelConnector();
	// connector.setPort(gridhttpsInfo.getHttpPort());
	// connector.setMaxIdleTime(MAX_IDLE_TIME);
	// davServer.addConnector(connector);
	// }
	// }

//	private void initServer() throws ServerException {
//		String[] webappConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpPort(),
//				getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpsPort() };
//		String[] mappingConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getMapperServlet().getPort() };
//		/* init WebDAV webapp */
//		WebAppContext waContext = new WebAppContext();
//		waContext.setDescriptor(waContext + "/WEB-INF/web.xml");
//		waContext.setResourceBase(".");
//		waContext.setParentLoaderPriority(true);
//		waContext.setConnectorNames(webappConnectors);
//		contextHandlerCollection.addHandler(waContext);
//		/* init Mapper Servlet */
//		mapperServlet = new MapperServlet();
//		ServletContextHandler msContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
//		msContext.setContextPath(File.separator + getGridhttpsInfo().getMapperServlet().getContextPath());
//		msContext.addServlet(new ServletHolder(mapperServlet), File.separator + getGridhttpsInfo().getMapperServlet().getContextSpec());
//		msContext.setConnectorNames(mappingConnectors);
//		contextHandlerCollection.addHandler(msContext);
//	}

//	private void initDavServer() throws ServerException {
//		initWebapp();
//	}
//
//	private void initMapServer() throws ServerException {
//		initMapperServlet();
//	}

	private String getMappingContextPath() {
		return File.separator + gridhttpsInfo.getMapperServlet().getContextPath() + File.separator
				+ gridhttpsInfo.getMapperServlet().getContextSpec();
	}

	private String getFileTransferContextPath() {
		return File.separator + gridhttpsInfo.getFiletransferContextPath();
	}

	private void initWebapp() throws ServerException {
		webapp = new WebApp(new File(getGridhttpsInfo().getWebappsDirectory(), DefaultConfiguration.SERVER_PATH_TO_WEBAPP));
		if (webapp != null) {
			if (!webapp.getResourceBase().exists()) {
				if (webapp.getResourceBase().mkdirs()) {
					try {
						Zip.unzip(getGridhttpsInfo().getWarFile().toString(), webapp.getResourceBase().toString());
					} catch (IOException e) {
						throw new ServerException(e);
					}
					String[] davExcluded = { "/index.jsp", getFileTransferContextPath(), getMappingContextPath() };
					String[] ftExcluded = { "/index.jsp", getMappingContextPath() };
					configureDescriptor(webapp.getDescriptorFile(), generateParams(), davExcluded, ftExcluded);
//					contextHandlerCollection.addHandler(getWebappContext());
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

//	private WebAppContext getWebappContext() {
//		String[] webappConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpPort(),
//				getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpsPort() };
//		log.info("webapp-connectors: " + webappConnectors[0] + ", " + webappConnectors[1]);
//		WebAppContext waContext = new WebAppContext();
//		// waContext.setDescriptor(waContext + "/WEB-INF/web.xml");
//		// waContext.setResourceBase(".");
//		waContext.setDescriptor(webapp.getDescriptorFile().toString());
//		waContext.setResourceBase(webapp.getResourceBase().getAbsolutePath());
//		waContext.setConnectorNames(webappConnectors);
//		for (Connector conn : oneServer.getConnectors()) {
//			if (conn.getName().equals(webappConnectors[0])) {
//				log.debug("found connector '" + webappConnectors[0] +"'");
//			}
//			if (conn.getName().equals(webappConnectors[1])) {
//				log.debug("found connector '" + webappConnectors[1] +"'");
//			}
//		}
//		waContext.setParentLoaderPriority(true);
//		waContext.setContextPath("/");
//		return waContext;
//	}

//	private void initMapperServlet() throws ServerException {
//		mapperServlet = new MapperServlet();
//		if (mapperServlet != null) {
//			oneServer.setHandler(getMapperServletContext());
//		} else {
//			log.error("Error on mapper-servlet creation - mapper-servlet is null!");
//			throw new ServerException("Error on mapper-servlet creation - mapper-servlet is null!");
//		}
//	}

//	private ServletContextHandler getMapperServletContext() {
//		String[] mappingConnectors = { getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getMapperServlet().getPort() };
//		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
//		context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
//		context.addServlet(new ServletHolder(mapperServlet), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
//		context.setConnectorNames(mappingConnectors);
//		return context;
//	}

	public void start() throws ServerException {
		try {
			// davServer.start();
			// mapServer.start();
			oneServer.start();
		} catch (Exception e) {
			throw new ServerException(e);
		}
		log.info("gridhttps-server started ");
	}

	public boolean isRunning() {
		// return (davServer.isRunning() && mapServer.isRunning());
		return (oneServer.isRunning() && oneServer.isRunning());
	}

	public void stop() throws ServerException {
		if (isRunning()) {
//			undeploy();
			try {
				// davServer.stop();
				// mapServer.stop();
				oneServer.stop();
			} catch (Exception e) {
				throw new ServerException(e);
			}
			log.info("gridhttps-server stopped ");
		}
	}

	public void status() {
		if (isRunning()) {
			log.info("gridhttps-server is listening on port " + getGridhttpsInfo().getHttpsPort() + " (secure connection)");
			if (getGridhttpsInfo().isEnabledHttp())
				log.info("gridhttps-server is listening on port " + getGridhttpsInfo().getHttpPort() + " (anonymous connection)");
			log.info("mapping-service is listening on port " + getGridhttpsInfo().getMapperServlet().getPort());
		} else {
			log.info("gridhttps-server is not running ");
			log.info("mapping-service is not running ");
		}
	}

//	private void undeploy() throws ServerException {
//		log.debug(" - undeploying webapp...");
//		// WebAppContext waContext = new WebAppContext();
//		// waContext.setDescriptor(waContext + "/WEB-INF/web.xml");
//		// waContext.setResourceBase(".");
//		// waContext.setParentLoaderPriority(true);
//		// contextHandlerCollection.removeHandler(waContext);
//		contextHandlerCollection.removeHandler(getWebappContext());
//	}

	private void configureDescriptor(File descriptorFile, Map<String, String> params, String[] davExcluded, String[] ftExcluded)
			throws ServerException {
		String query1 = "/j2ee:web-app/j2ee:filter[@id='stormAuthorizationFilter']/j2ee:init-param/j2ee:param-value";
		String query2 = "/j2ee:web-app/j2ee:filter[@id='webdavSpringMiltonFilter']/j2ee:init-param/j2ee:param-value";
		String query3 = "/j2ee:web-app/j2ee:filter[@id='fileTransferSpringMiltonFilter']/j2ee:init-param/j2ee:param-value";
		try {
			XML doc = new XML(descriptorFile);
			NodeList nodes;
			nodes = doc.getNodes(query1, new WebNamespaceContext(null));
			((Element) nodes.item(0)).setTextContent(JSON.toString(params));
			nodes = doc.getNodes(query2, new WebNamespaceContext(null));
			String result = Arrays.asList(davExcluded).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", ",");
			((Element) nodes.item(0)).setTextContent(result);
			nodes = doc.getNodes(query3, new WebNamespaceContext(null));
			result = Arrays.asList(ftExcluded).toString().replaceAll("(^\\[|\\]$)", "").replace(", ", ",");
			((Element) nodes.item(0)).setTextContent(result);
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