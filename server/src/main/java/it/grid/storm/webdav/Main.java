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
import java.util.ArrayList;
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
				if (SA.getProtocol() == StorageArea.NONE_PROTOCOL) continue;
				//SA protocol is HTTP or HTTPS or both:
				File webappDir = new File(server.getWebappsDirectory() + "/" + SA.getStfnRoot());
				log.info("Copying the template directory on '"+ webappDir.getPath() +"'...");
				FileUtils.copyFolder(templateDir, webappDir);
				File contextFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/classes/applicationContext.xml");
				log.info("Configuring the context file '"+ contextFile.getPath() +"'...");
				configureContextFile(contextFile, SA);
				File webFile = new File(webappDir.getAbsolutePath() + "/WEB-INF/web.xml");
				log.info("Configuring the web.xml file '"+ webFile.getPath() +"'...");
				configureWebFile(webFile, SA);
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
		Element resourceFactory = doc.getNodeFromKeyValue("id", "milton.fs.resource.factory");
		// set root directory:
		log.debug("setting root directory as '" + rootDirectory + "'...");
		Element rootNode = doc.getNodeFromKeyValue(resourceFactory, "index", "0");
		doc.setAttribute(rootNode, "value", rootDirectory);
		// set context path:
		log.debug("setting context path as '" + contextPath + "'...");
		Element contextPathNode = doc.getNodeFromKeyValue(resourceFactory, "index", "1");
		doc.setAttribute(contextPathNode, "value", contextPath);
		// set backend hostname:
		log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
		Element stormBackendHostname = doc.getNodeFromKeyValue(resourceFactory, "index", "2");
		doc.setAttribute(stormBackendHostname, "value", stormBEHostname);
		// set backend port:
		log.debug("setting storm backend port as '" + stormBEPort + "'...");
		Element stormBackendPort = doc.getNodeFromKeyValue(resourceFactory, "index", "3");
		doc.setAttribute(stormBackendPort, "value", String.valueOf(stormBEPort));
		doc.close();	
	}
	
	private static void configureWebFile(File webFile, StorageArea SA) throws Exception {
		// modify web.xml file
		String rootDirectory = SA.getFSRoot();
		String contextPath = SA.getStfnRoot().substring(1);
		String protocol = StorageArea.protocolToStr(SA.getProtocol());
		XML doc = new XML(webFile);
		
		ArrayList<Element> filters = doc.getChildren(doc.getRootElement(), "filter");
		for (Element filter : filters) {
			String filterName = doc.getChildren(filter, "filter-name").get(0).getValue();
			if (!filterName.equals("stormAuthorizationFilter")) continue;
			log.debug("stormAuthorizationFilter node found...");
			assert(filterName.equals("stormAuthorizationFilter"));
			ArrayList<Element> initParams = doc.getChildren(filter, "init-param");
			log.debug("setting root directory as '" + rootDirectory + "'...");
			doc.getChildren(initParams.get(0), "param-value").get(0).setText(rootDirectory);
			log.debug("setting context path as '" + contextPath + "'...");
			doc.getChildren(initParams.get(1), "param-value").get(0).setText(contextPath);
			log.debug("setting protocol as '" + protocol + "'...");
			doc.getChildren(initParams.get(2), "param-value").get(0).setText(protocol);
			log.debug("setting storm backend hostname as '" + stormBEHostname + "'...");
			doc.getChildren(initParams.get(3), "param-value").get(0).setText(stormBEHostname);
			log.debug("setting storm backend port as '" + stormBEPort + "'...");
			doc.getChildren(initParams.get(4), "param-value").get(0).setText(String.valueOf(stormBEPort));
		}
		
		doc.close();
	}
	
}
