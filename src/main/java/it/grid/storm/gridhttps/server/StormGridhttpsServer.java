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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
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
	private Server oneServer;
	private List<String> servletConnectors = new ArrayList<String>();
	private List<String> webappConnectors = new ArrayList<String>();

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws ServerException {
		setGridhttpsInfo(gridhttpsInfo);
		setBackendInfo(backendInfo);
		setFrontendInfo(frontendInfo);
		init();
	}

	private void init() throws ServerException {
		servletConnectors.clear();
		webappConnectors.clear();
		oneServer = ServerFactory.newServer(getGridhttpsInfo().getHostname(), getGridhttpsInfo().getHttpsPort(), getGridhttpsInfo()
				.getSsloptions());
		oneServer.setStopAtShutdown(true);
		oneServer.setGracefulShutdown(1000);
		oneServer.setThreadPool(getThreadPool());
		webappConnectors.add(getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpsPort());
		/* add plain HTTP connector if enabled */
		if (getGridhttpsInfo().isEnabledHttp()) {
			oneServer.addConnector(getHttpConnector(getGridhttpsInfo().getHostname(), getGridhttpsInfo().getHttpPort()));
			webappConnectors.add(getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getHttpPort());
		}
		/* add MapperServlet connector */
		oneServer.addConnector(getHttpConnector(getGridhttpsInfo().getHostname(), getGridhttpsInfo().getMapperServlet().getPort()));
		servletConnectors.add(getGridhttpsInfo().getHostname() + ":" + getGridhttpsInfo().getMapperServlet().getPort());
		/* deploy */
		ServletContextHandler context = deployServlet();
		WebAppContext waContext = deployWebApp();
		/* init context */
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		contexts.setHandlers(new Handler[] { context, waContext });
		oneServer.setHandler(contexts);

	}

	private WebAppContext deployWebApp() throws ServerException {
		File resourceBase = new File(getGridhttpsInfo().getWebappsDirectory(), DefaultConfiguration.SERVER_PATH_TO_WEBAPP);
		WebApp webapp = new WebApp(resourceBase);
		try {
			webapp.init(getGridhttpsInfo().getWarFile());
		} catch (IOException e) {
			log.error("Error on webapp initialization: " + e.getMessage());
			throw new ServerException(e);
		}
		String mappingContextPath = File.separator + getGridhttpsInfo().getMapperServlet().getContextPath() + File.separator
				+ getGridhttpsInfo().getMapperServlet().getContextSpec();
		String fileTransferContextPath = File.separator + getGridhttpsInfo().getFiletransferContextPath();
		String[] davExcluded = { "/index.jsp", fileTransferContextPath, mappingContextPath };
		String[] ftExcluded = { "/index.jsp", mappingContextPath };
		configureDescriptor(webapp.getDescriptorFile(), generateParams(), davExcluded, ftExcluded);
		String[] connectors = webappConnectors.toArray(new String[webappConnectors.size()]);
		return getWebAppContext(webapp.getDescriptorFile().toString(), webapp.getResourceBase().getAbsolutePath(), connectors);
	}

	private ServletContextHandler deployServlet() {
		String contextPath = File.separator + getGridhttpsInfo().getMapperServlet().getContextPath();
		String contextSpec = File.separator + getGridhttpsInfo().getMapperServlet().getContextSpec();
		String[] connectors = servletConnectors.toArray(new String[servletConnectors.size()]);
		ServletContextHandler context = getMapperServletContext(new MapperServlet(), contextPath, contextSpec, connectors);
		return context;
	}

	private WebAppContext getWebAppContext(String descriptorPath, String resourceBase, String[] webappConnectors) {
		WebAppContext waContext = new WebAppContext();
		waContext.setDescriptor(descriptorPath);
		waContext.setResourceBase(resourceBase);
		waContext.setConnectorNames(webappConnectors);
		waContext.setParentLoaderPriority(true);
		waContext.setContextPath("/");
		return waContext;
	}

	private ServletContextHandler getMapperServletContext(MapperServlet mapperServlet, String contextPath, String contextSpec,
			String[] mappingConnectors) {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);
		context.addServlet(new ServletHolder(mapperServlet), contextSpec);
		context.setConnectorNames(mappingConnectors);
		return context;
	}

	private Connector getHttpConnector(String hostname, int httpPort) {
		Connector connector = new SelectChannelConnector();
		connector.setPort(httpPort);
		connector.setMaxIdleTime(MAX_IDLE_TIME);
		connector.setHost(hostname);
		return connector;
	}

	private QueuedThreadPool getThreadPool() {
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setMaxIdleTimeMs(20000);
		threadPool.setMaxThreads(getGridhttpsInfo().getDavActiveThreadsMax());
		threadPool.setMaxQueued(getGridhttpsInfo().getDavQueuedThreadsMax());
		return threadPool;
	}

	public void start() throws ServerException {
		try {
			oneServer.start();
		} catch (Exception e) {
			throw new ServerException(e);
		}
		log.info("gridhttps-server started ");
	}

	public boolean isRunning() {
		return (oneServer.isRunning() && oneServer.isRunning());
	}

	public void stop() throws ServerException {
		if (isRunning()) {
			try {
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