package it.grid.storm.webdav;

import it.grid.storm.webdav.server.*;
import it.grid.storm.gridhttps.servlet.MapperServlet;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.*;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.UUID;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

public class Main {

	private static Logger log;

	private static class StormBackend {
		public static String hostname;
		public static int port;
		public static int servicePort;
	}

	private static class StormFrontend {
		public static String hostname;
		public static int port;
	}

	private static class StormGridhttps {
		public static String hostname;
		public static int httpPort;
		public static int httpsPort;
		public static boolean useHttp;
		public static String contextPath;
		public static String contextSpec;
		public static String webappsDir;
		public static SSLOptions ssloptions;
		public static String logFile;
		public static String webdavWebapp;
		public static String fileTransferWebapp;
		public static String configurationFile;
		
		public static ServerInfo getServerInfo() {
			return new ServerInfo(hostname, httpPort, httpsPort, ssloptions, useHttp);
		}
	}

	private static File webdavTemplateDir;
	private static File fileTransferTemplateDir;
	private static File templatesDir;
	
	private static WebDAVServer server;

	public static void main(String[] args) {
		
		loadDefaultConfiguration();
		try {
			parseCommandLine(args);
			loadConfiguration();
			initLogging();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

//		Object lock = new Object();
//		synchronized (lock) {
//			try {
//				lock.wait(3 * 1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//		}

		printConfiguration();

		try {

			initServer();
			startServer();

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			try {
				stopServer();
			} catch (Exception e2) {
				log.error(e2.getMessage());
				e2.printStackTrace();
			}
			try {
				clearTmpDirectories();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			System.exit(1);
		}

		// adds an handler to CTRL-C that stops and deletes the webapps
		// directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					stopServer();
				} catch (Exception e2) {
					log.error(e2.getMessage());
					e2.printStackTrace();
				}
				try {
					clearTmpDirectories();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

	}

	private static void initLogging() throws Exception {
		/* INIT LOGGING COMPONENT */
		System.out.println("init logger");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		FileInputStream fin = new FileInputStream(StormGridhttps.logFile);
		configurator.doConfigure(fin);
		fin.close();
		loggerContext.start();
		log = LoggerFactory.getLogger(Main.class);
		System.out.println("logger loaded successfully");
	}

	private static void initServer() throws Exception {
		log.info("Creating WebDAV server...");
		server = new WebDAVServer(StormGridhttps.getServerInfo());
		server.setWebappsDirectory(StormGridhttps.webappsDir);
		initTmpDirectories();
		deployWebapps();
	}

	private static void startServer() throws Exception {
		log.info("Starting WebDAV-server...");
		server.start();
		server.status();
	}

	private static void stopServer() throws Exception {
		if (server == null)
			return;
		log.info("Undeploying all webapps...");
		server.undeployAll();
		log.info("Stopping WebDAV-server...");
		server.stop();
	}

	private static void initTmpDirectories() throws Exception {
		(new File(server.getWebappsDirectory())).mkdir();

		templatesDir = new File(server.getWebappsDirectory() + "/.tmp_" + UUID.randomUUID());

		webdavTemplateDir = new File(templatesDir.getAbsolutePath() + "/WebDAV");
		decompress(StormGridhttps.webdavWebapp, webdavTemplateDir.getAbsolutePath());

		fileTransferTemplateDir = new File(templatesDir.getAbsolutePath() + "/FileTransfer");
		decompress(StormGridhttps.fileTransferWebapp, fileTransferTemplateDir.getAbsolutePath());
	}

	private static void decompress(String fromPath, String toPath) throws Exception {
		log.info("Decompress '" + fromPath + "' on '" + toPath + "'...");
		(new Zip()).unzip(fromPath, toPath);
	}

	private static void copyFolder(String fromFolderPath, String toFolderPath) throws Exception {
		log.info("Copy '" + fromFolderPath + "' on '" + toFolderPath + "'...");
		FileUtils.copyFolder(new File(fromFolderPath), new File(toFolderPath));
	}

	private static void deployWebapps() throws Exception {
		/* init storage area list */
		StorageAreaManager.init(StormBackend.hostname, StormBackend.servicePort);
		for (StorageArea SA : StorageAreaManager.getInstance().getStorageAreas()) {
			if (SA.getProtocol() == StorageArea.NONE_PROTOCOL)
				continue;
			/* webdav webapp */
			String webdavWebappPath = server.getWebappsDirectory() + "/WebDAV" + SA.getStfnRoot();
			copyFolder(webdavTemplateDir.getPath(), webdavWebappPath);
			String webPath = webdavWebappPath + "/WEB-INF/web.xml";
			configureWebFile(webPath, SA);
			server.deploy(new WebDAVWebApp(new File(webdavWebappPath), SA));
			/* file transfer webapp */
			String ftWebappPath = server.getWebappsDirectory() + "/FileTransfer" + SA.getStfnRoot();
			copyFolder(fileTransferTemplateDir.getPath(), ftWebappPath);
			String ftWebPath = ftWebappPath + "/WEB-INF/web.xml";
			String contextPath = ftWebappPath + "/WEB-INF/classes/applicationContext.xml";
			configureWebFile(ftWebPath, SA);
			configureContextFile(contextPath, SA);
			server.deploy(new FileTransferWebApp(new File(ftWebappPath), SA));
		}
		/* Mapper Servlet */
		server.deployGridHTTPs(StormGridhttps.contextPath, StormGridhttps.contextSpec);
	}

	private static void clearTmpDirectories() throws Exception {
		FileUtils.deleteDirectory(webdavTemplateDir);
		FileUtils.deleteDirectory(fileTransferTemplateDir);
		FileUtils.deleteDirectory(templatesDir);
	}

	private static void configureWebFile(String filePath, StorageArea SA) throws Exception {
		File webFile = new File(filePath);
		log.info("Configuring '" + webFile.getPath() + "'...");
		// modify web.xml file
		String rootDirectory = SA.getFSRoot();
		String contextPath = SA.getStfnRoot().substring(1);
		String protocol = StorageArea.protocolToStr(SA.getProtocol());
		XML doc = new XML(webFile);
		String query = "/j2ee:web-app/j2ee:filter[@id='stormAuthorizationFilter']/j2ee:init-param/j2ee:param-value";
		NodeList initParams = doc.getNodes(query, new WebNamespaceContext(null));
		log.debug("setting root directory as '" + rootDirectory + "'...");
		((Element) initParams.item(0)).setTextContent(rootDirectory);
		log.debug("setting context path as '" + contextPath + "'...");
		((Element) initParams.item(1)).setTextContent(contextPath);
		log.debug("setting protocol as '" + protocol + "'...");
		((Element) initParams.item(2)).setTextContent(protocol);
		log.debug("setting storm backend hostname as '" + StormBackend.hostname + "'...");
		((Element) initParams.item(3)).setTextContent(StormBackend.hostname);
		log.debug("setting storm backend port as '" + StormBackend.port + "'...");
		((Element) initParams.item(4)).setTextContent(String.valueOf(StormBackend.port));
		log.debug("setting storm backend service port as '" + StormBackend.servicePort + "'...");
		((Element) initParams.item(5)).setTextContent(String.valueOf(StormBackend.servicePort));
		log.debug("setting storm frontend hostname as '" + StormFrontend.hostname + "'...");
		((Element) initParams.item(6)).setTextContent(StormFrontend.hostname);
		log.debug("setting storm frontend port as '" + StormFrontend.port + "'...");
		((Element) initParams.item(7)).setTextContent(String.valueOf(StormFrontend.port));
		doc.save();
	}

	private static void configureContextFile(String filePath, StorageArea SA) throws Exception {
		File contextFile = new File(filePath);
		log.info("Configuring '" + contextFile.getPath() + "'...");
		// modify applicationContext.xml file
		String rootDirectory = SA.getFSRoot();
		String contextPath = MapperServlet.MAPPER_SERVLET_CONTEXT_PATH.substring(1) + SA.getStfnRoot();
		XML doc = new XML(contextFile);
		String query = "/spring:beans/spring:bean[@id='milton.fs.resource.factory']/spring:constructor-arg";
		NodeList arguments = doc.getNodes(query, new AppNamespaceContext(null));
		log.debug("setting root directory as '" + rootDirectory + "'...");
		((Element) arguments.item(0)).setAttribute("value", rootDirectory);
		log.debug("setting context path as '" + contextPath + "'...");
		((Element) arguments.item(1)).setAttribute("value", contextPath);
		doc.save();
	}

	private static void parseCommandLine(String[] args) throws Exception {
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV webapp template [mandatory]", true, true);
		cli.addOption("ftw", "the absolute file path of the file transfer webapp template [mandatory]", true, true);
		cli.addOption("conf", "the absolute file path of the server configuration file [mandatory]", true, true);
		cli.addOption("dir", "the absolute file path of the deployed webapps directory", true, false);
		StormGridhttps.webdavWebapp = cli.getString("w");
		System.out.println("webdav-webapp:         " + StormGridhttps.webdavWebapp);
		StormGridhttps.fileTransferWebapp = cli.getString("ftw");
		System.out.println("file-transfer-webapp:  " + StormGridhttps.fileTransferWebapp);
		StormGridhttps.configurationFile = cli.getString("conf");
		System.out.println("server-configuration:  " + StormGridhttps.configurationFile);
		if (cli.hasOption("dir")) {
			StormGridhttps.webappsDir = cli.getString("dir");
			System.out.println("webapps-directory:     " + StormGridhttps.webappsDir);
		} else {
			StormGridhttps.webappsDir = DefaultConfiguration.WEBAPPS_DIRECTORY;
		}
	}

	private static void loadConfiguration() throws Exception {
		Wini configuration;
		try {
			configuration = new Wini(new File(StormGridhttps.configurationFile));
		} catch (InvalidFileFormatException e) {
			throw new Exception(e.getMessage());
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
		/* storm_gridhttps */
		if (!configuration.keySet().contains("storm_gridhttps"))
			throw new Exception("Configuration file 'storm_gridhttps' section missed!");
		if (configuration.get("storm_gridhttps").containsKey("log_configuration_file"))
			StormGridhttps.logFile = configuration.get("storm_gridhttps", "log_configuration_file");
		if (configuration.get("storm_gridhttps").containsKey("http_port"))
			StormGridhttps.httpPort = configuration.get("storm_gridhttps", "http_port", int.class);
		if (configuration.get("storm_gridhttps").containsKey("https_port"))
			StormGridhttps.httpsPort = configuration.get("storm_gridhttps", "https_port", int.class);
		if (configuration.get("storm_gridhttps").containsKey("use_http"))
			StormGridhttps.useHttp = configuration.get("storm_gridhttps", "use_http", boolean.class);
		if (configuration.get("storm_gridhttps").containsKey("webapps_directory"))
			StormGridhttps.webappsDir = configuration.get("storm_gridhttps", "webapps_directory");
		if (configuration.get("storm_gridhttps").containsKey("certificate_file"))
			StormGridhttps.ssloptions.setCertificateFile(configuration.get("storm_gridhttps", "certificate_file"));
		if (configuration.get("storm_gridhttps").containsKey("key_file"))
			StormGridhttps.ssloptions.setKeyFile(configuration.get("storm_gridhttps", "key_file"));
		if (configuration.get("storm_gridhttps").containsKey("trust_store_directory"))
			StormGridhttps.ssloptions.setTrustStoreDirectory(configuration.get("storm_gridhttps", "trust_store_directory"));

		/* storm_backend */
		if (!configuration.keySet().contains("storm_backend"))
			throw new Exception("Configuration file 'storm_backend' section missed!");
		if (configuration.get("storm_backend").containsKey("hostname"))
			StormBackend.hostname = configuration.get("storm_backend", "hostname");
		if (configuration.get("storm_backend").containsKey("service_port"))
			StormBackend.servicePort = configuration.get("storm_backend", "service_port", int.class);
		if (configuration.get("storm_backend").containsKey("port"))
			StormBackend.port = configuration.get("storm_backend", "port", int.class);

		/* storm_frontend */
		if (!configuration.keySet().contains("storm_frontend"))
			throw new Exception("Configuration file 'storm_frontend' section missed!");
		if (configuration.get("storm_frontend").containsKey("hostname"))
			StormFrontend.hostname = configuration.get("storm_frontend", "hostname");
		if (configuration.get("storm_frontend").containsKey("port"))
			StormFrontend.port = configuration.get("storm_frontend", "port", int.class);
	}

	private static void loadDefaultConfiguration() {
		/* gridhttps */
		StormGridhttps.contextPath = DefaultConfiguration.MAPPER_SERVLET_CONTEXT_PATH;
		StormGridhttps.contextSpec = DefaultConfiguration.MAPPER_SERVLET_CONTEXT_SPEC;
		StormGridhttps.httpPort = DefaultConfiguration.STORM_GRIDHTTPS_HTTP_PORT;
		StormGridhttps.httpsPort = DefaultConfiguration.STORM_GRIDHTTPS_HTTPS_PORT;
		StormGridhttps.useHttp = DefaultConfiguration.STORM_GRIDHTTPS_USE_HTTP;
		StormGridhttps.httpsPort = DefaultConfiguration.STORM_GRIDHTTPS_HTTPS_PORT;
		StormGridhttps.logFile = DefaultConfiguration.LOG_FILE;
		StormGridhttps.ssloptions = new SSLOptions();
		StormGridhttps.ssloptions.setCertificateFile(DefaultConfiguration.HTTPS_CERTIFICATE_FILE);
		StormGridhttps.ssloptions.setKeyFile(DefaultConfiguration.HTTPS_KEY_FILE);
		StormGridhttps.ssloptions.setTrustStoreDirectory(DefaultConfiguration.HTTPS_TRUST_STORE_DIRECTORY);
		StormGridhttps.ssloptions.setNeedClientAuth(DefaultConfiguration.STORM_GRIDHTTPS_HTTPS_NEED_CLIENT_AUTH);
		StormGridhttps.ssloptions.setWantClientAuth(DefaultConfiguration.STORM_GRIDHTTPS_HTTPS_WANT_CLIENT_AUTH);
		/* backend */
		StormBackend.port = DefaultConfiguration.STORM_BE_PORT;
		StormBackend.servicePort = DefaultConfiguration.STORM_BE_SERVICE_PORT;
		/* frontend */
		StormFrontend.port = DefaultConfiguration.STORM_FE_PORT;
	}
	
	private static void printConfiguration() {
		log.debug("gridhttps hostname = " + StormGridhttps.hostname);
		log.debug("gridhttps http port = " + StormGridhttps.httpPort);
		log.debug("gridhttps https port = " + StormGridhttps.httpsPort);
		log.debug("gridhttps use http = " + StormGridhttps.useHttp);
		log.debug("gridhttps context path = " + StormGridhttps.contextPath);
		log.debug("gridhttps context spec = " + StormGridhttps.contextSpec);
		log.debug("gridhttps host certificate = " + StormGridhttps.ssloptions.getCertificateFile());
		log.debug("gridhttps host certificate key = " + StormGridhttps.ssloptions.getKeyFile());
		log.debug("gridhttps trust store directory = " + StormGridhttps.ssloptions.getTrustStoreDirectory());
		log.debug("storm backend hostname = " + StormBackend.hostname);
		log.debug("storm backend service port = " + StormBackend.servicePort);
		log.debug("storm backend port = " + StormBackend.port);
		log.debug("storm frontend hostname = " + StormFrontend.hostname);
		log.debug("storm frontend port = " + StormFrontend.port);
	}

}
