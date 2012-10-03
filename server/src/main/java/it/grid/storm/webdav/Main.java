package it.grid.storm.webdav;

import it.grid.storm.webdav.server.ServerInfo;
import it.grid.storm.webdav.server.WebApp;
import it.grid.storm.webdav.server.WebDAVServer;
import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.FileUtils;
import it.grid.storm.webdav.utils.MyCommandLineParser;
import it.grid.storm.webdav.utils.XML;
import it.grid.storm.webdav.utils.Zip;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);
	
	private static String warTemplateFile;
	private static String stormBEHostname;
	private static int stormBEPort;
	
	private static String configurationFile;
	private static WebDAVServer server;
	private static ServerInfo httpOptions, httpsOptions;
	private static String hostname;
	private static String webappsDir = "/webapps";
	private static boolean useHttp, useHttps;
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
			server = new WebDAVServer(httpOptions, httpsOptions);
			log.info("Setting webapps directory to '"+ getExeDirectory() + webappsDir +"'");
			server.setWebappsDirectory(getExeDirectory() + webappsDir);
			log.info("Retrieving the Storage Area list from Storm Backend...");
			storageareas = StorageAreaManager.retrieveStorageAreasFromStormBackend(stormBEHostname, stormBEPort);
			log.info("Deploying webapps...");
			String tempDir = server.getWebappsDirectory() + "/.tmp_" + new Timestamp((new Date()).getTime());
			log.info("Decompressing the template file '" + warTemplateFile + "' on '"+tempDir+"'...");
			File templateDir = new File(tempDir);
			templateDir.mkdir();
			(new Zip()).unzip(warTemplateFile, tempDir);
			for (StorageArea SA : storageareas) {
				File webappDir = new File(server.getWebappsDirectory() + "/" + SA.getStfnRoot());
				log.info("Copying the template directory on '"+ webappDir.getPath() +"'...");
				FileUtils.copyFolder(templateDir, webappDir);
				File contextFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/classes/applicationContext.xml");
				log.info("Configuring the context file '"+ contextFile.getPath() +"'...");
				configureContextFile(contextFile, SA);
				log.info("Deploying '"+ SA.getName() +"' webapp...");
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
		cli.addOption("test", "create WebDAV-fs-server test webapp on root /tmp", false, false);
		cli.addOption("conf", "the absolute file path of server's configuration file", true, true);
		warTemplateFile = cli.getString("w");
		if (cli.hasOption("conf"))
			configurationFile = cli.getString("conf");
	}

	private static void loadConfiguration(String filename) throws InvalidFileFormatException, IOException {
		Wini configuration = new Wini(new File(filename));
		// This hostname
		hostname = configuration.get("options", "hostname");
		// Storm BE hostname and port
		stormBEHostname = configuration.get("storm_backend", "hostname");
		stormBEPort = configuration.get("storm_backend", "port", int.class);
		// Http server
		useHttp = configuration.get("http", "enabled", boolean.class);
		if (useHttp)
			httpOptions = new ServerInfo(configuration.get("http", "name"), hostname, configuration.get("http", "port", int.class));
		else 
			httpOptions = new ServerInfo();
		// Https server
		useHttps = configuration.get("https", "enabled", boolean.class);
		if (useHttps) {
			SSLOptions options = new SSLOptions();
			options.setCertificateFile(configuration.get("https", "certificate_file"));
			options.setKeyFile(configuration.get("https", "key_file"));
			options.setTrustStoreDirectory(configuration.get("https", "trust_store_directory"));
			httpsOptions = new ServerInfo(configuration.get("https", "name"), hostname, configuration.get("https", "port", int.class), options);
		} else
			httpsOptions = new ServerInfo();
	}
	
	private static void configureContextFile(File contextFile, StorageArea SA) throws Exception {
		// modify application context file
		String rootDirectory = SA.getFSRoot();
		String contextPath = SA.getStfnRoot().substring(1);
		XML doc = new XML(contextFile);
		Element resourceFactory = doc.getNodeFromKeyValue("id", "milton.fs.resource.factory");
		// set root directory:
		log.debug("setting root directory as '" + rootDirectory + "'...");
		Element rootNode = doc.getNodeFromKeyValue(resourceFactory, "name", "root");
		doc.setAttribute(rootNode, "value", rootDirectory);
		// set context path:
		log.debug("setting context path as '" + contextPath + "'...");
		Element contextPathNode = doc.getNodeFromKeyValue(resourceFactory, "name", "contextPath");
		doc.setAttribute(contextPathNode, "value", contextPath);
		// set backend hostname:
		log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
		Element stormBackendHostname = doc.getNodeFromKeyValue(resourceFactory, "name", "stormBackendHostname");
		doc.setAttribute(stormBackendHostname, "value", stormBEHostname);
		// set backend port:
		log.debug("setting storm backend port as '" + stormBEPort + "'...");
		Element stormBackendPort = doc.getNodeFromKeyValue(resourceFactory, "name", "stormBackendPort");
		doc.setAttribute(stormBackendPort, "value", String.valueOf(stormBEPort));
		doc.close();	
	}
	
}
