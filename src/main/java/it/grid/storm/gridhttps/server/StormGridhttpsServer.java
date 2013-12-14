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
import it.grid.storm.gridhttps.server.statushandler.StatusHandler;

import java.io.File;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.WebAppContext;
import org.italiangrid.utils.https.ServerFactory;
import org.italiangrid.utils.https.impl.canl.CANLListener;
import org.italiangrid.voms.util.CertificateValidatorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;

public class StormGridhttpsServer {

	private static final int MAX_IDLE_TIME = 30000;

	private static final Logger log = LoggerFactory.getLogger(StormGridhttpsServer.class);
	private StormGridhttps gridhttpsInfo;
	private Server oneServer;
	private WebAppContext webdavContext, filetransferContext;
	private ServletContextHandler mappingContext;
	private ContextHandler statusContext;
	private Connector httpsConnector, httpConnector, mapHttpConnector;

	public StormGridhttpsServer(StormGridhttps gridhttpsInfo) throws ServerException {
		this.gridhttpsInfo = gridhttpsInfo;
		initServer();
		initContextHandlers();
	}

	private void initServer() {
		CANLListener l = new CANLListener();
		X509CertChainValidatorExt validator = CertificateValidatorBuilder
			.buildCertificateValidator(gridhttpsInfo.getSsloptions()
				.getTrustStoreDirectory(), l, l, gridhttpsInfo.getSsloptions()
				.getTrustStoreRefreshIntervalInMsec(), false);
		oneServer = ServerFactory.newServer(gridhttpsInfo.getHostname(),
			gridhttpsInfo.getHttpsPort(), gridhttpsInfo.getSsloptions(), validator,
			ServerFactory.MAX_CONNECTIONS, ServerFactory.MAX_REQUEST_QUEUE_SIZE);
		oneServer.setStopAtShutdown(true);
		oneServer.setGracefulShutdown(1000);
		oneServer.setThreadPool(getThreadPool());
		httpsConnector = oneServer.getConnectors()[0];
		initConnectors();
	}
	
	private void initConnectors() {
		if (gridhttpsInfo.isHTTPEnabled()) {
			httpConnector = getHttpConnector(gridhttpsInfo.getHostname(), gridhttpsInfo.getHttpPort());
			oneServer.addConnector(httpConnector);
		}
		mapHttpConnector = getHttpConnector(gridhttpsInfo.getHostname(), gridhttpsInfo.getMapperServlet().getPort());
		oneServer.addConnector(mapHttpConnector);
	}
	
	private void initContextHandlers() throws ServerException {

		ContextHandlerCollection contexts = new ContextHandlerCollection();
		try {
			initWebDAVContext();
			initFileTransferContext();
			initMappingServletContext();
			initStatusHandlerContext();
		} catch (Exception e) {
			throw new ServerException(e);
		}
		contexts.setHandlers(new Handler[] { webdavContext, filetransferContext, mappingContext, statusContext, new DefaultHandler() });
		oneServer.setHandler(contexts);
	}
	
	private void initStatusHandlerContext() {

		statusContext = new ContextHandler();
		statusContext.setContextPath("/");
		statusContext.setResourceBase(".");
		statusContext.setHandler(new StatusHandler());
	}

	private void initWebDAVContext() {
		String webappResourceDir = this.getClass().getClassLoader().getResource("webapp").toExternalForm();
		webdavContext = new WebAppContext();
		webdavContext.setResourceBase(webappResourceDir);
		webdavContext.setDescriptor(webappResourceDir + "/WEB-INF/webdav.xml");
		webdavContext.setContextPath(File.separator + gridhttpsInfo.getWebdavContextPath());
		webdavContext.setParentLoaderPriority(true);
		webdavContext.setThrowUnavailableOnStartupException(true);
		if (gridhttpsInfo.isHTTPEnabled()) {
			webdavContext.setConnectorNames(new String[] { httpsConnector.getName(), httpConnector.getName() });
		} else {
			webdavContext.setConnectorNames(new String[] { httpsConnector.getName() });
		}
		webdavContext.setCompactPath(true);
	}
	
	private void initFileTransferContext() {
		String webappResourceDir = this.getClass().getClassLoader().getResource("webapp").toExternalForm();
		filetransferContext = new WebAppContext();
		filetransferContext.setResourceBase(webappResourceDir);
		filetransferContext.setDescriptor(webappResourceDir + "/WEB-INF/filetransfer.xml");
		filetransferContext.setContextPath(File.separator + gridhttpsInfo.getFiletransferContextPath());
		filetransferContext.setParentLoaderPriority(true);
		filetransferContext.setThrowUnavailableOnStartupException(true);
		if (gridhttpsInfo.isHTTPEnabled()) {
			filetransferContext.setConnectorNames(new String[] { httpsConnector.getName(), httpConnector.getName() });
		} else {
			filetransferContext.setConnectorNames(new String[] { httpsConnector.getName() });
		}
		filetransferContext.setCompactPath(true);
	}

	private void initMappingServletContext() {
		String contextPath = File.separator + gridhttpsInfo.getMapperServlet().getContextPath();
		String contextSpec = File.separator + gridhttpsInfo.getMapperServlet().getContextSpec();
		String[] mappingConnectors = { mapHttpConnector.getName() };
		mappingContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
		mappingContext.setContextPath(contextPath);
		mappingContext.addServlet(new ServletHolder(new MapperServlet()), contextSpec);
		mappingContext.setConnectorNames(mappingConnectors);
		mappingContext.setCompactPath(true);
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
		if (!oneServer.isFailed()) {
			log.info("gridhttps-server started ");
	  } else {
			log.error("gridhttps-server failed ");
			throw new ServerException("gridhttps-server failed!");
	  }
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