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
	private static String stormFEHostname;
	private static int stormBEServicePort;
	private static int stormBEPort;
	private static int stormFEPort;

	private static String configurationFile;
	private static WebDAVServer server;
	private static ServerInfo options;
	private static String hostname;
	private static String webappsDir = "/webapps";
	private static boolean useHttp;
	private static List<StorageArea> storageareas;

//	static final String hostname2 = "omii005-vm03.cnaf.infn.it";
//	static final Long xmlrpcPort = new Long(8080);
//	static final String userDN = "/C=IT/O=INFN/OU=Personal Certificate/L=CNAF/CN=Enrico Vianello";
//	static final String userFQAN = "/dteam/Role=NULL/Capability=NULL";
//	static final String userFQAN2 = "/dteam/NGI_IT/Role=NULL/Capability=NULL";
//	static final ArrayList<String> userFQANS = new ArrayList<String>();
//	static {
//		userFQANS.add(userFQAN);
//		userFQANS.add(userFQAN2);
//	}

	public static void main(String[] args) {

//		BackendApi client;
//		try {
//			client = new BackendApi(hostname2, xmlrpcPort);
//			PingOutputData output = client.ping(userDN, userFQANS);
//			log.debug("isSuccess: " + output.isSuccess());
//			log.debug(" - " + output.getBeOs() + " - " + output.getBeVersion() + " - " + output.getVersionInfo());
//		} catch (IllegalArgumentException e2) {
//			e2.printStackTrace();
//		} catch (ApiException e2) {
//			e2.printStackTrace();
//		}
		
		
		
		try {
			parseCommandLine(args);
			loadConfiguration(configurationFile);
			log.info("Configuration file loaded successfully");
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(1);
		}
		try {
			log.info("Creating WebDAV server...");
			server = new WebDAVServer(options);
			log.info("Setting webapps directory to '" + getExeDirectory() + webappsDir + "'");
			server.setWebappsDirectory(getExeDirectory() + webappsDir);
			log.info("Retrieving the Storage Area list from Storm Backend...");
			storageareas = StorageAreaManager.retrieveStorageAreasFromStormBackend(stormBEHostname, stormBEServicePort);
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
		hostname = getConfigurationValue(configuration, "server", "hostname");
		useHttp = getConfigurationValue(configuration, "server", "enabled_http", boolean.class);
		int httpPort = getConfigurationValue(configuration, "server", "http_port", int.class);
		int httpsPort = getConfigurationValue(configuration, "server", "https_port", int.class);
		
		SSLOptions ssloptions = new SSLOptions();
		ssloptions.setCertificateFile(getConfigurationValue(configuration, "server", "certificate_file"));
		ssloptions.setKeyFile(getConfigurationValue(configuration, "server", "key_file"));
		ssloptions.setTrustStoreDirectory(getConfigurationValue(configuration, "server", "trust_store_directory"));
		
		options = new ServerInfo(hostname, httpPort, httpsPort, ssloptions, useHttp);
		
		log.debug("stormBEHostname = " + stormBEHostname);
		log.debug("stormBEServicePort = " + stormBEServicePort);
		log.debug("stormBEPort = " + stormBEPort);
		
		log.debug("stormFEHostname = " + stormFEHostname);
		log.debug("stormFEPort = " + stormFEPort);
		
		log.debug("hostname = " + hostname);
		log.debug("useHttp = " + useHttp);
		log.debug("httpPort = " + httpPort);
		log.debug("httpsPort = " + httpsPort);
		log.debug("ssloptions.certificateFile = " + ssloptions.getCertificateFile());
		log.debug("ssloptions.KeyFile = " + ssloptions.getKeyFile());
		log.debug("ssloptions.TrustStoreDirectory = " + ssloptions.getTrustStoreDirectory());
	
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
		log.debug("setting storm backend service port as '" + stormBEServicePort + "'...");
		((Element) arguments.item(4)).setAttribute("value", String.valueOf(stormBEServicePort));
		log.debug("setting storm frontend hostname as '" + stormFEHostname + "'...");
		((Element) arguments.item(5)).setAttribute("value", stormFEHostname);
		log.debug("setting storm frontend port as '" + stormFEPort + "'...");
		((Element) arguments.item(6)).setAttribute("value", String.valueOf(stormFEPort));
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
		log.debug("setting storm backend service port as '" + stormBEServicePort + "'...");
		((Element) initParams.item(5)).setTextContent(String.valueOf(stormBEServicePort));
		log.debug("setting storm frontend hostname as '" + stormFEHostname + "'...");
		((Element) initParams.item(6)).setTextContent(stormFEHostname);
		log.debug("setting storm frontend port as '" + stormFEPort + "'...");
		((Element) initParams.item(7)).setTextContent(String.valueOf(stormFEPort));
		doc.save();
	}

}
