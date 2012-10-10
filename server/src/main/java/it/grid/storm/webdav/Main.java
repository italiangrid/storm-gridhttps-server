package it.grid.storm.webdav;

import it.grid.storm.webdav.server.*;
import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.*;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

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
	private static int stormBEPort;

	private static String configurationFile;
	private static WebDAVServer server;
	private static ServerInfo options;
	private static String hostname;
	private static String webappsDir = "/webapps";
	private static boolean useHttp;
	private static List<StorageArea> storageareas;

	public static void main(String[] args) {

		try {
			parseCommandLine(args);
			loadConfiguration(configurationFile);
			log.info("Configuration file loaded successfully");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			log.info("Creating WebDAV server...");
			server = new WebDAVServer(options);
			log.info("Setting webapps directory to '" + getExeDirectory() + webappsDir + "'");
			server.setWebappsDirectory(getExeDirectory() + webappsDir);
			log.info("Retrieving the Storage Area list from Storm Backend...");
			storageareas = StorageAreaManager.retrieveStorageAreasFromStormBackend(stormBEHostname, stormBEPort);
			log.info("Deploying webapps...");
			String tempDir = server.getWebappsDirectory() + "/.tmp_" + new Timestamp((new Date()).getTime());
			log.info("Decompressing the template file '" + warTemplateFile + "' on '" + tempDir + "'...");
			File templateDir = new File(tempDir);
			templateDir.mkdir();
			(new Zip()).unzip(warTemplateFile, tempDir);
			for (StorageArea SA : storageareas) {
				if (SA.getProtocol() == StorageArea.NONE_PROTOCOL)
					continue;
				// SA protocol is HTTP or HTTPS or both:
				File webappDir = new File(server.getWebappsDirectory() + "/" + SA.getStfnRoot());
				log.info("Copying the template directory on '" + webappDir.getPath() + "'...");
				FileUtils.copyFolder(templateDir, webappDir);
				File contextFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/classes/applicationContext.xml");
				log.info("Configuring the context file '" + contextFile.getPath() + "'...");
				configureContextFile(contextFile, SA);
				File webFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/web.xml");
				log.info("Configuring the web.xml file '" + webFile.getPath() + "'...");
				configureWebFile(webFile, SA);
				log.info("Deploying '" + SA.getName() + "' webapp...");
				server.deploy(new WebApp(webappDir, SA));
			}
			log.info("Starting WebDAV-server...");
			server.start();
			server.status();
			FileUtils.deleteDirectory(templateDir);
		} catch (Exception e) {
			e.printStackTrace();
			try {
				log.info("Undeploying all webapps...");
				server.undeployAll();
				log.info("Stopping WebDAV-server...");
				server.stop();
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

	private static String getExeDirectory() {
		return (new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParent();
	}

	private static void parseCommandLine(String[] args) throws Exception {

		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV template webapp [necessary]", true, true);
		cli.addOption("conf", "the absolute file path of server's configuration file", true, true);
		warTemplateFile = cli.getString("w");
		if (cli.hasOption("conf"))
			configurationFile = cli.getString("conf");
	}

	private static void loadConfiguration(String filename) throws InvalidFileFormatException, IOException {
		Wini configuration = new Wini(new File(filename));
		SSLOptions ssloptions = new SSLOptions();
		// Storm BE hostname and port
		stormBEHostname = configuration.get("storm_backend", "hostname");
		stormBEPort = configuration.get("storm_backend", "port", int.class);
		// Server
		hostname = configuration.get("server", "hostname");
		useHttp = configuration.get("server", "enabledHttp", boolean.class);
		int httpPort = configuration.get("server", "httpPort", int.class);
		int httpsPort = configuration.get("server", "httpsPort", int.class);
		ssloptions.setCertificateFile(configuration.get("server", "certificate_file"));
		ssloptions.setKeyFile(configuration.get("server", "key_file"));
		ssloptions.setTrustStoreDirectory(configuration.get("server", "trust_store_directory"));
		options = new ServerInfo(hostname, httpPort, httpsPort, ssloptions, useHttp);
	}

	private static void configureContextFile(File contextFile, StorageArea SA) throws Exception {
		// modify application context file
		String rootDirectory = SA.getFSRoot();
		String contextPath = SA.getStfnRoot().substring(1);
		XML doc = new XML(contextFile);
		String query = "/spring:beans/spring:bean[@id='milton.fs.resource.factory']/spring:constructor-arg";
		NodeList arguments = doc.getNodes(query, new AppNamespaceContext(null));
		log.debug("setting root directory as '" + rootDirectory + "'...");
		((Element) arguments.item(0)).setAttribute("value", rootDirectory);
		log.debug("setting context path as '" + contextPath + "'...");
		((Element) arguments.item(1)).setAttribute("value", contextPath);
		log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
		((Element) arguments.item(2)).setAttribute("value", stormBEHostname);
		log.debug("setting storm backend port as '" + stormBEPort + "'...");
		((Element) arguments.item(3)).setAttribute("value", String.valueOf(stormBEPort));
		doc.save();
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
		doc.save();
	}

}
