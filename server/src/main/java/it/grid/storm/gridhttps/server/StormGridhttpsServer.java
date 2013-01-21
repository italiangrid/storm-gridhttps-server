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
	private Server davServer;
	private Server mapServer;
	private ContextHandlerCollection contextHandlerCollection;
	private WebApp webapp;
	private MapperServlet mapperServlet;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo, StormBackend backendInfo, StormFrontend frontendInfo) throws ServerException {
		setGridhttpsInfo(gridhttpsInfo);
		setBackendInfo(backendInfo);
		setFrontendInfo(frontendInfo);
		createDavServer();
		createMapServer();
		initDavServer();
		initMapServer();
	}

	private void createMapServer() {
		mapServer = new Server(DefaultConfiguration.STORM_GHTTPS_MAPPER_SERVLET_PORT);		
	}

	private void createDavServer() {
		davServer = ServerFactory.newServer(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions());
		davServer.setStopAtShutdown(true);
		davServer.setGracefulShutdown(1000);
		HandlerCollection hc = new HandlerCollection();
		contextHandlerCollection = new ContextHandlerCollection();
		hc.setHandlers(new Handler[] { contextHandlerCollection });
		davServer.setHandler(hc);
		if (gridhttpsInfo.isEnabledHttp()) {
			Connector connector = new SelectChannelConnector();
			connector.setPort(gridhttpsInfo.getHttpPort());
			connector.setMaxIdleTime(MAX_IDLE_TIME);
			davServer.addConnector(connector);
		}
	}

	private void initDavServer() throws ServerException {
		initWebapp();
	}

	private void initMapServer() throws ServerException {
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

	private WebAppContext getWebappContext() {
		WebAppContext context = new WebAppContext();
		context.setDescriptor(webapp.getDescriptorFile().toString());
		context.setResourceBase(webapp.getResourceBase().getAbsolutePath());
		context.setParentLoaderPriority(true);
		return context;
	}

	private void initMapperServlet() throws ServerException {
		mapperServlet = new MapperServlet();
		if (mapperServlet != null) {
			mapServer.setHandler(getMapperServletContext());
		} else {
			log.error("Error on mapper-servlet creation - mapper-servlet is null!");
			throw new ServerException("Error on mapper-servlet creation - mapper-servlet is null!");
		}
	}

	private ServletContextHandler getMapperServletContext() {
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(File.separator + gridhttpsInfo.getMapperServlet().getContextPath());
		context.addServlet(new ServletHolder(mapperServlet), File.separator + gridhttpsInfo.getMapperServlet().getContextSpec());
		return context;
	}
	
	public void start() throws ServerException {
		try {
			davServer.start();
			mapServer.start();
		} catch (Exception e) {
			throw new ServerException(e);
		}
		log.info("gridhttps-server started ");
	}

	public boolean isRunning() {
		return (davServer.isRunning() && mapServer.isRunning());
	}

	public void stop() throws ServerException {
		if (isRunning()) {
			undeploy();
			try {
				davServer.stop();
				mapServer.stop();
			} catch (Exception e) {
				throw new ServerException(e);
			}
			log.info("gridhttps-server stopped ");
		}
	}

	public void status() {
		if (isRunning()) {
			log.info("gridhttps-server is listening on port " + gridhttpsInfo.getHttpsPort() + " (secure connection)");
			if (gridhttpsInfo.isEnabledHttp())
				log.info("gridhttps-server is listening on port " + gridhttpsInfo.getHttpPort() + " (anonymous connection)");
			log.info("mapping-service is listening on port " + gridhttpsInfo.getMapperServlet().getPort());
		} else {
			log.info("gridhttps-server is not running ");
			log.info("mapping-service is not running ");
		}
	}

	private void undeploy() throws ServerException {
		log.debug(" - undeploying webapp...");
		contextHandlerCollection.removeHandler(getWebappContext());
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