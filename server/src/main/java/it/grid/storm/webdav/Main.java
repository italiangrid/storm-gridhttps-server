package it.grid.storm.webdav;

import it.grid.storm.webdav.server.*;
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

	private class DefaultConfiguration {
		public final static String STORM_GRIDHTTPS_CONTEXT_PATH = "gridhttps_webapp";
		public final static String STORM_GRIDHTTPS_CONTEXT_SPEC = "resourceMapping";
		public final static int STORM_BE_SERVICE_PORT = 9998;
		public final static int STORM_BE_PORT = 8080;
		public final static int STORM_FE_PORT = 8444;
		public final static String WEBAPPS_DIRECTORY_ROOT = "/var/lib/storm";
		public final static int STORM_GRIDHTTPS_HTTP_PORT = 8085;
		public final static int STORM_GRIDHTTPS_HTTPS_PORT = 8443;
		public final static boolean STORM_GRIDHTTPS_USE_HTTP = true;
		public final static boolean STORM_GRIDHTTPS_WANT_CLIENT_AUTH = true;
		public final static boolean STORM_GRIDHTTPS_NEED_CLIENT_AUTH = true;
	}

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
		private static String contextPath;
		private static String contextSpec;
		private static String webappsDir;
		private static ServerInfo options;
		private static final String WEBAPPS_DIRECTORY_NAME = "/webapps";
	}

	private static String webDAVTemplate;
	private static String fileTransferTemplate;
	private static String configurationFile;
	private static String logFile;

	private static File templatesDir;
	private static File webdavTemplateDir;
	private static File fileTransferTemplateDir;

	private static WebDAVServer server;

	public static void main(String[] args) {

		System.out.println("OS current temporary directory is " + System.getProperty("java.io.tmpdir"));
		
		try {
			parseCommandLine(args);
			loadConfiguration();
			initLogging();
		} catch (Exception e) {
			System.exit(1);
		}

		Object lock = new Object();
		synchronized (lock) {
			try {
				lock.wait(3 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		printConfiguration();

		try {

			initServer();
			initTmpDirectories();
			deployWebapps();
			startServer();
			clearTmpDirectories();

		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			try {
				clearTmpDirectories();
			} catch (Exception e1) {
				log.error(e1.getMessage());
				e1.printStackTrace();
			}
			try {
				stopServer();
			} catch (Exception e2) {
				log.error(e2.getMessage());
				e2.printStackTrace();
			}
			System.exit(1);
		}

		// adds an handler to CTRL-C that stops and deletes the webapps
		// directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					clearTmpDirectories();
				} catch (Exception e1) {
					log.error(e1.getMessage());
					e1.printStackTrace();
				}
				try {
					stopServer();
				} catch (Exception e2) {
					log.error(e2.getMessage());
					e2.printStackTrace();
				}
			}
		});

	}

	private static void initLogging() throws Exception {
		/* INIT LOGGING COMPONENT */
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		FileInputStream fin;
		fin = new FileInputStream(logFile);
		configurator.doConfigure(fin);
		fin.close();
		loggerContext.start();
		log = LoggerFactory.getLogger(Main.class);
		System.out.println("logger loaded successfully");
	}

	private static void initServer() throws Exception {
		log.info("Creating WebDAV server...");
		server = new WebDAVServer(StormGridhttps.options);
		server.setWebappsDirectory(StormGridhttps.webappsDir + StormGridhttps.WEBAPPS_DIRECTORY_NAME);
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
		decompress(webDAVTemplate, webdavTemplateDir.getAbsolutePath());

		fileTransferTemplateDir = new File(templatesDir.getAbsolutePath() + "/FileTransfer");
		decompress(fileTransferTemplate, fileTransferTemplateDir.getAbsolutePath());
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
		String contextPath = "filetransfer/" + SA.getStfnRoot().substring(1);
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
		webDAVTemplate = cli.getString("w");
		fileTransferTemplate = cli.getString("ftw");
		configurationFile = cli.getString("conf");
		if (cli.hasOption("dir"))
			StormGridhttps.webappsDir = cli.getString("dir");

	}

	private static String getConfigurationValue(Wini configuration, String sectionName, String fieldName) throws Exception {
		return String.valueOf(getConfigurationValue(configuration, sectionName, fieldName, String.class));
	}

	private static <T> T getConfigurationValue(Wini configuration, String sectionName, String fieldName, Class<T> classType)
			throws Exception {
		if (!configuration.keySet().contains(sectionName))
			throw new Exception("Configuration file: '" + sectionName + "' section missed!");
		if (!configuration.get(sectionName).containsKey(fieldName))
			throw new Exception("Configuration file: '" + sectionName + " > " + fieldName + "' missed!");
		return configuration.get(sectionName, fieldName, classType);
	}

	private static void loadConfiguration() throws Exception {
		Wini configuration;
		try {
			configuration = new Wini(new File(configurationFile));
		} catch (InvalidFileFormatException e) {
			throw new Exception(e.getMessage());
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
		/* LOG CONFIGURATION FILE */
		logFile = getConfigurationValue(configuration, "server", "log_configuration_file");
		/* BACKEND CONFIGURATION */
		StormBackend.hostname = getConfigurationValue(configuration, "storm_backend", "hostname");
		try {
			StormBackend.servicePort = getConfigurationValue(configuration, "storm_backend", "service_port", int.class);
		} catch (Exception e) {
			StormBackend.servicePort = DefaultConfiguration.STORM_BE_SERVICE_PORT;
		}
		try {
			StormBackend.port = getConfigurationValue(configuration, "storm_backend", "port", int.class);
		} catch (Exception e) {
			StormBackend.port = DefaultConfiguration.STORM_BE_PORT;
		}
		/* FRONTEND CONFIGURATION */
		StormFrontend.hostname = getConfigurationValue(configuration, "storm_frontend", "hostname");
		try {
			StormFrontend.port = getConfigurationValue(configuration, "storm_frontend", "port", int.class);
		} catch (Exception e) {
			StormFrontend.port = DefaultConfiguration.STORM_FE_PORT;
		}
		/* GRIDHTTPS CONFIGURATION */
		try {
			StormGridhttps.contextPath = getConfigurationValue(configuration, "gridhttps", "context_path");
		} catch (Exception e) {
			StormGridhttps.contextPath = DefaultConfiguration.STORM_GRIDHTTPS_CONTEXT_PATH;
		}
		try {
			StormGridhttps.contextSpec = getConfigurationValue(configuration, "gridhttps", "context_spec");
		} catch (Exception e) {
			StormGridhttps.contextSpec = DefaultConfiguration.STORM_GRIDHTTPS_CONTEXT_SPEC;
		}
		try {
			StormGridhttps.useHttp = getConfigurationValue(configuration, "server", "enabled_http", boolean.class);
		} catch (Exception e) {
			StormGridhttps.useHttp = DefaultConfiguration.STORM_GRIDHTTPS_USE_HTTP;
		}
		try {
			StormGridhttps.httpPort = getConfigurationValue(configuration, "server", "http_port", int.class);
		} catch (Exception e) {
			StormGridhttps.httpPort = DefaultConfiguration.STORM_GRIDHTTPS_HTTP_PORT;
		}
		try {
			StormGridhttps.httpsPort = getConfigurationValue(configuration, "server", "https_port", int.class);
		} catch (Exception e) {
			StormGridhttps.httpsPort = DefaultConfiguration.STORM_GRIDHTTPS_HTTPS_PORT;
		}
		try {
			StormGridhttps.webappsDir = getConfigurationValue(configuration, "server", "webapps_directory");
		} catch (Exception e) {
			StormGridhttps.webappsDir = DefaultConfiguration.WEBAPPS_DIRECTORY_ROOT;
		}
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		StormGridhttps.hostname = localMachine.getHostName();
		SSLOptions ssloptions = new SSLOptions();
		ssloptions.setCertificateFile(getConfigurationValue(configuration, "server", "certificate_file"));
		ssloptions.setKeyFile(getConfigurationValue(configuration, "server", "key_file"));
		ssloptions.setTrustStoreDirectory(getConfigurationValue(configuration, "server", "trust_store_directory"));
		ssloptions.setWantClientAuth(DefaultConfiguration.STORM_GRIDHTTPS_WANT_CLIENT_AUTH);
		ssloptions.setWantClientAuth(DefaultConfiguration.STORM_GRIDHTTPS_NEED_CLIENT_AUTH);
		StormGridhttps.options = new ServerInfo(StormGridhttps.hostname, StormGridhttps.httpPort, StormGridhttps.httpsPort, ssloptions,
				StormGridhttps.useHttp);
	}

	private static void printConfiguration() {
		log.debug("gridhttps hostname = " + StormGridhttps.hostname);
		log.debug("gridhttps http port = " + StormGridhttps.httpPort);
		log.debug("gridhttps https port = " + StormGridhttps.httpsPort);
		log.debug("gridhttps use http = " + StormGridhttps.useHttp);
		log.debug("gridhttps context path = " + StormGridhttps.contextPath);
		log.debug("gridhttps context spec = " + StormGridhttps.contextSpec);
		log.debug("gridhttps host certificate = " + StormGridhttps.options.getSslOptions().getCertificateFile());
		log.debug("gridhttps host certificate key = " + StormGridhttps.options.getSslOptions().getKeyFile());
		log.debug("gridhttps trust store directory = " + StormGridhttps.options.getSslOptions().getTrustStoreDirectory());
		log.debug("storm backend hostname = " + StormBackend.hostname);
		log.debug("storm backend service port = " + StormBackend.servicePort);
		log.debug("storm backend port = " + StormBackend.port);
		log.debug("storm frontend hostname = " + StormFrontend.hostname);
		log.debug("storm frontend port = " + StormFrontend.port);
	}

}
