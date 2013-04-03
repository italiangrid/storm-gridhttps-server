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

import it.grid.storm.gridhttps.configuration.StormGridhttps;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.mapperservlet.MapperServlet;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.ServerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormGridhttpsServer {

	private static final int MAX_IDLE_TIME = 30000;

	private static final Logger log = LoggerFactory.getLogger(StormGridhttpsServer.class);
	private StormGridhttps gridhttpsInfo;
	private Server oneServer;
	private WebAppContext webAppContext;
	private ServletContextHandler servletContext;
	private Connector davHttpsConnector, davHttpConnector, mapHttpConnector;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo) throws ServerException {
		this.gridhttpsInfo = gridhttpsInfo;
		init();
	}

	private void init() throws ServerException {
		oneServer = ServerFactory.newServer(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions());
		davHttpsConnector = oneServer.getConnectors()[0];
		oneServer.setStopAtShutdown(true);
		oneServer.setGracefulShutdown(1000);
		oneServer.setThreadPool(getThreadPool());
		/* add plain HTTP connector if enabled */
		if (gridhttpsInfo.isHTTPEnabled()) {
			davHttpConnector = getHttpConnector(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpPort());
			oneServer.addConnector(davHttpConnector);
		}
		/* add MapperServlet connector */
		mapHttpConnector = getHttpConnector(gridhttpsInfo.getHostname(), gridhttpsInfo.getMapperServlet().getPort());
		oneServer.addConnector(mapHttpConnector);
		/* deploy webapp and servlet */
		ContextHandlerCollection contexts = new ContextHandlerCollection();
		try {
			buildWebAppContext();
		} catch (IOException e) {
			throw new ServerException(e);
		}
		buildServletContext();
		contexts.setHandlers(new Handler[] { webAppContext, servletContext });
		oneServer.setHandler(contexts);
	}
	
	private WebAppContext buildWebAppContext() throws IOException{
		String webappResourceDir = this.getClass().getClassLoader().getResource("webapp").toExternalForm();
		webAppContext = new WebAppContext();
		webAppContext.setResourceBase(webappResourceDir);
		webAppContext.setContextPath(gridhttpsInfo.getWebdavContextPath());
		webAppContext.setParentLoaderPriority(true);
		if (gridhttpsInfo.isHTTPEnabled()) {
			String[] davConnectors = { davHttpsConnector.getName(), davHttpConnector.getName() };
			webAppContext.setConnectorNames(davConnectors);
		} else {
			String[] davConnectors = { davHttpsConnector.getName() };
			webAppContext.setConnectorNames(davConnectors);
		}
		return webAppContext;
	}

	private ServletContextHandler buildServletContext() {
		String contextPath = File.separator + gridhttpsInfo.getMapperServlet().getContextPath();
		String contextSpec = File.separator + gridhttpsInfo.getMapperServlet().getContextSpec();
		String[] mappingConnectors = { mapHttpConnector.getName() };
		servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletContext.setContextPath(contextPath);
		servletContext.addServlet(new ServletHolder(new MapperServlet()), contextSpec);
		servletContext.setConnectorNames(mappingConnectors);
		return servletContext;
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
		threadPool.setMaxThreads(gridhttpsInfo.getServerActiveThreadsMax());
		threadPool.setMaxQueued(gridhttpsInfo.getServerQueuedThreadsMax());
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
			log.info("gridhttps-server is listening on port " + gridhttpsInfo.getHttpsPort() + " (secure connection)");
			if (gridhttpsInfo.isHTTPEnabled())
				log.info("gridhttps-server is listening on port " + gridhttpsInfo.getHttpPort() + " (anonymous connection)");
			log.info("mapping-service is listening on port " + gridhttpsInfo.getMapperServlet().getPort());
		} else {
			log.info("gridhttps-server is not running ");
			log.info("mapping-service is not running ");
		}
	}

}