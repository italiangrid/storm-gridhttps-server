package it.grid.storm.webdav;

import it.grid.storm.webdav.server.*;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.*;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	private static String warTemplateFile;
	private static String stormBEHostname;
	private static String stormFEHostname;
	private static int stormBEServicePort;
	private static int stormBEPort;
	private static int stormFEPort;
	private static String gridhttpsContextPath;
	private static String gridhttpsContextSpec;
	
	
	private static String configurationFile;
	private static WebDAVServer server;
	private static ServerInfo options;
	private static String webappsFolderDir = "/var/lib/storm";
	private static final String WEBAPPS_FOLDER = "/webapps";
	private static boolean useHttp;
	
	public static void main(String[] args) {
	
		try {
			parseCommandLine(args);
			loadConfiguration(configurationFile);
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}
		log.info("Configuration file loaded successfully");
		
		try {
			log.info("Creating WebDAV server...");
//			Object lock = new Object();
//			synchronized (lock) {
//				lock.wait(3 * 1000);
//			}
			server = new WebDAVServer(options);
			log.info("Setting webapps directory to '" + webappsFolderDir + WEBAPPS_FOLDER + "'");
			server.setWebappsDirectory(webappsFolderDir + WEBAPPS_FOLDER);
			log.info("Retrieving the Storage Area list from Storm Backend...");
			StorageAreaManager.init(stormBEHostname, stormBEServicePort);
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
			server.deployGridHTTPs(gridhttpsContextPath, gridhttpsContextSpec);
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
				if (server == null) return;
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

//	private static String getExeDirectory() {
//		return (new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParent();
//	}

	private static void parseCommandLine(String[] args) throws Exception {
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV template webapp [mandatory]", true, true);
		cli.addOption("dir", "the absolute file path of the WebDAV webapps deployed directory", true, false);
		cli.addOption("conf", "the absolute file path of server's configuration file [mandatory]", true, true);
		warTemplateFile = cli.getString("w");
		if (cli.hasOption("dir"))
			webappsFolderDir = cli.getString("dir");
		if (cli.hasOption("conf"))
			configurationFile = cli.getString("conf");
	}

	private static String getConfigurationValue(Wini configuration, String sectionName, String fieldName) throws Exception {
		return String.valueOf(getConfigurationValue(configuration, sectionName, fieldName, String.class));
	}
	
	private static <T> T getConfigurationValue(Wini configuration, String sectionName, String fieldName, Class<T> classType) throws Exception {
		if (!configuration.keySet().contains(sectionName))
			throw new Exception("Configuration file: '" + sectionName + "' section missed!");
		if (!configuration.get(sectionName).containsKey(fieldName))
			throw new Exception("Configuration file: '" + sectionName + " > " + fieldName + "' missed!");
		return configuration.get(sectionName, fieldName, classType);
	}
	    
	private static void loadConfiguration(String filename) throws Exception {
		Wini configuration;
		try {
			configuration = new Wini(new File(filename));
		} catch (InvalidFileFormatException e) {
			throw new Exception(e.getMessage());
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
		stormBEHostname = getConfigurationValue(configuration, "storm_backend", "hostname");
		stormBEServicePort = getConfigurationValue(configuration, "storm_backend", "service_port", int.class);
		stormBEPort = getConfigurationValue(configuration, "storm_backend", "port",  int.class);
		stormFEHostname = getConfigurationValue(configuration, "storm_frontend", "hostname");
		stormFEPort = getConfigurationValue(configuration, "storm_frontend", "port",  int.class);
		gridhttpsContextPath = getConfigurationValue(configuration, "gridhttps", "context_path");
		gridhttpsContextSpec = getConfigurationValue(configuration, "gridhttps", "context_spec");
		useHttp = getConfigurationValue(configuration, "server", "enabled_http", boolean.class);
		int httpPort = getConfigurationValue(configuration, "server", "http_port", int.class);
		int httpsPort = getConfigurationValue(configuration, "server", "https_port", int.class);
		SSLOptions ssloptions = new SSLOptions();
		ssloptions.setCertificateFile(getConfigurationValue(configuration, "server", "certificate_file"));
		ssloptions.setKeyFile(getConfigurationValue(configuration, "server", "key_file"));
		ssloptions.setTrustStoreDirectory(getConfigurationValue(configuration, "server", "trust_store_directory"));
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		log.debug("Local hostname = " + localMachine.getHostName());
		options = new ServerInfo(localMachine.getHostName(), httpPort, httpsPort, ssloptions, useHttp);
		log.debug("stormBEHostname = " + stormBEHostname);
		log.debug("stormBEServicePort = " + stormBEServicePort);
		log.debug("stormBEPort = " + stormBEPort);
		log.debug("stormFEHostname = " + stormFEHostname);
		log.debug("stormFEPort = " + stormFEPort);
		log.debug("gridhttpsContextPath = " + gridhttpsContextPath);
		log.debug("gridhttpsContextSpec = " + gridhttpsContextSpec);
		log.debug("useHttp = " + useHttp);
		log.debug("httpPort = " + httpPort);
		log.debug("httpsPort = " + httpsPort);
		log.debug("ssloptions.certificateFile = " + ssloptions.getCertificateFile());
		log.debug("ssloptions.KeyFile = " + ssloptions.getKeyFile());
		log.debug("ssloptions.TrustStoreDirectory = " + ssloptions.getTrustStoreDirectory());
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
		log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
		((Element) initParams.item(3)).setTextContent(stormBEHostname);
		log.debug("setting storm backend port as '" + stormBEPort + "'...");
		((Element) initParams.item(4)).setTextContent(String.valueOf(stormBEPort));
		log.debug("setting storm backend service port as '" + stormBEServicePort + "'...");
		((Element) initParams.item(5)).setTextContent(String.valueOf(stormBEServicePort));
		log.debug("setting storm frontend hostname as '" + stormFEHostname + "'...");
		((Element) initParams.item(6)).setTextContent(stormFEHostname);
		log.debug("setting storm frontend port as '" + stormFEPort + "'...");
		((Element) initParams.item(7)).setTextContent(String.valueOf(stormFEPort));
		doc.save();
	}

}
