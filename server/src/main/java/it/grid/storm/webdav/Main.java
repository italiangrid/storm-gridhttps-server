package it.grid.storm.webdav;

import it.grid.storm.webdav.server.*;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.*;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

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
	}

	private static Logger log; // = LoggerFactory.getLogger(Main.class);

	private static String warTemplateFile;

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

	private static String configurationFile;
	private static WebDAVServer server;
	private static String logFile;

	public static void main(String[] args) {

		try {
			parseCommandLine(args);
			loadConfiguration();
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}

		/* INIT LOGGING COMPONENT */
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(loggerContext);
        FileInputStream fin;
		try {
			fin = new FileInputStream(logFile);
			configurator.doConfigure(fin);
			fin.close();
		} catch (FileNotFoundException e2) {
			e2.printStackTrace();
			System.exit(1);
		} catch (JoranException e2) {
			e2.printStackTrace();
			System.exit(1);
		} catch (IOException e2) {
			e2.printStackTrace();
			System.exit(1);
		}
        loggerContext.start();
		log = LoggerFactory.getLogger(Main.class);

		System.out.println("logger loaded successfully");
		
		log.info("Configuration file loaded successfully");

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

		try {
			log.info("Creating WebDAV server...");
			// Object lock = new Object();
			// synchronized (lock) {
			// lock.wait(3 * 1000);
			// }
			server = new WebDAVServer(StormGridhttps.options);
			log.info("Setting webapps directory to '" + StormGridhttps.webappsDir + StormGridhttps.WEBAPPS_DIRECTORY_NAME + "'");
			server.setWebappsDirectory(StormGridhttps.webappsDir + StormGridhttps.WEBAPPS_DIRECTORY_NAME);
			log.info("Retrieving the Storage Area list from Storm Backend...");
			StorageAreaManager.init(StormBackend.hostname, StormBackend.servicePort);
			log.info("Deploying webapps...");
			String tempDir = server.getWebappsDirectory() + "/.tmp_" + new Timestamp((new Date()).getTime());
			log.info("Decompressing the template file '" + warTemplateFile + "' on '" + tempDir + "'...");
			File templateDir = new File(tempDir);
			templateDir.mkdir();
			(new Zip()).unzip(warTemplateFile, tempDir);
			for (StorageArea SA : StorageAreaManager.getInstance().getStorageAreas()) {
				if (SA.getProtocol() == StorageArea.NONE_PROTOCOL)
					continue;
				File webappDir = new File(server.getWebappsDirectory() + "/" + SA.getStfnRoot());
				log.info("Copying the template directory on '" + webappDir.getPath() + "'...");
				FileUtils.copyFolder(templateDir, webappDir);
				File webFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/web.xml");
				log.info("Configuring the web.xml file '" + webFile.getPath() + "'...");
				configureWebFile(webFile, SA);
				log.info("Deploying '" + SA.getName() + "' webapp...");
				server.deploy(new WebApp(webappDir, SA));
			}
			server.deployGridHTTPs(StormGridhttps.contextPath, StormGridhttps.contextSpec);
			log.info("Starting WebDAV-server...");
			server.start();
			server.status();
			FileUtils.deleteDirectory(templateDir);
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			if (server != null) {
				try {
					log.info("Undeploying all webapps...");
					server.undeployAll();
					log.info("Stopping WebDAV-server...");
					server.stop();
				} catch (Exception e1) {
					log.error(e.getMessage());
					e1.printStackTrace();
				}
			}
			System.exit(1);
		}

		// adds an handler to CTRL-C that stops and deletes the webapps
		// directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (server == null)
					return;
				try {
					log.info("Undeploying all webapps...");
					server.undeployAll();
					log.info("Stopping WebDAV-server...");
					server.stop();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});

	}

	private static void parseCommandLine(String[] args) throws Exception {
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV template webapp [mandatory]", true, true);
		cli.addOption("dir", "the absolute file path of the WebDAV webapps deployed directory", true, false);
		cli.addOption("conf", "the absolute file path of server's configuration file [mandatory]", true, true);
		if (cli.hasOption("conf"))
			configurationFile = cli.getString("conf");
		if (cli.hasOption("dir"))
			StormGridhttps.webappsDir = cli.getString("dir");
		warTemplateFile = cli.getString("w");
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
		StormGridhttps.options = new ServerInfo(StormGridhttps.hostname, StormGridhttps.httpPort, StormGridhttps.httpsPort, ssloptions,
				StormGridhttps.useHttp);

	}

	private static void configureWebFile(File webFile, StorageArea SA) throws Exception {
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

}
