package it.grid.storm.webdav;

import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.webdav.server.*;
import it.grid.storm.webdav.server.data.StormBackend;
import it.grid.storm.webdav.server.data.StormFrontend;
import it.grid.storm.webdav.server.data.StormGridhttps;
import it.grid.storm.webdav.utils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;

public class Main {

	private static Logger log;

	public static String webappFileName;
	public static String configurationFileName;

	private static StormBackend stormBackend;
	private static StormFrontend stormFrontend;
	private static StormGridhttps stormGridhttps;
	private static StormGridhttpsServer server;

	public static void main(String[] args) {

		Object lock = new Object();
		synchronized (lock) {
			try {
				lock.wait(5 * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		try {
			parseCommandLine(args);
			loadDefaultConfiguration();
			loadConfiguration();
			checkConfiguration();
			initLogging();
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		printConfiguration();
		
		try {
			StorageAreaManager.init(stormBackend.getHostname(), stormBackend.getServicePort());
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
			}
		});

	}

	private static void checkConfiguration() throws Exception {
		stormBackend.checkConfiguration();
		stormFrontend.checkConfiguration();
		stormGridhttps.checkConfiguration();
	}

	private static void initLogging() throws Exception {
		/* INIT LOGGING COMPONENT */
		System.out.println("init logger");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		FileInputStream fin = new FileInputStream(stormGridhttps.getLogFile());
		configurator.doConfigure(fin);
		fin.close();
		loggerContext.start();
		log = LoggerFactory.getLogger(Main.class);
		System.out.println("logger loaded successfully");
	}

	private static void initServer() throws Exception {
		log.info("Start storm-gridhttps-server initialization");
		server = new StormGridhttpsServer(stormGridhttps, stormBackend, stormFrontend);
		log.debug("Server created");
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

	private static void parseCommandLine(String[] args) throws Exception {
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV webapp template [mandatory]", true, true);
		cli.addOption("conf", "the absolute file path of the server configuration file [mandatory]", true, true);
		webappFileName = cli.getString("w");
		System.out.println("webdav-webapp:         " + webappFileName);
		configurationFileName = cli.getString("conf");
		System.out.println("server-configuration:  " + configurationFileName);
	}

	private static void loadDefaultConfiguration() throws UnknownHostException {
		/* gridhttps */
		stormGridhttps = new StormGridhttps();
		java.net.InetAddress localMachine = java.net.InetAddress.getLocalHost();
		stormGridhttps.setHostname(localMachine.getHostName());
		stormGridhttps.setWarFile(new File(webappFileName));
		/* backend */
		stormBackend = new StormBackend();
		stormBackend.setHostname(localMachine.getHostName());
		/* frontend */
		stormFrontend = new StormFrontend();
		stormFrontend.setHostname(localMachine.getHostName());
	}

	private static void loadConfiguration() throws Exception {
		Wini configuration;
		try {
			configuration = new Wini(new File(configurationFileName));
		} catch (InvalidFileFormatException e) {
			throw new Exception(e.getMessage());
		} catch (IOException e) {
			throw new Exception(e.getMessage());
		}
		/* service */
		if (!configuration.keySet().contains("service"))
			throw new Exception("Configuration file 'service' section missed!");
		if (configuration.get("service").containsKey("log.configuration-file"))
			stormGridhttps.setLogFile(configuration.get("service", "log.configuration-file"));
		if (configuration.get("service").containsKey("webapp-directory"))
			stormGridhttps.setWebappsDirectory(configuration.get("service", "webapp-directory"));

		/* connectors */
		if (!configuration.keySet().contains("connectors"))
			throw new Exception("Configuration file 'connectors' section missed!");
		if (configuration.get("connectors").containsKey("http.enabled"))
			stormGridhttps.setEnabledHttp(configuration.get("connectors", "http.enabled", boolean.class));
		if (configuration.get("connectors").containsKey("http.port"))
			stormGridhttps.setHttpPort(configuration.get("connectors", "http.port", int.class));
		if (configuration.get("connectors").containsKey("https.port"))
			stormGridhttps.setHttpsPort(configuration.get("connectors", "https.port", int.class));
		if (configuration.get("connectors").containsKey("x509.host-certificate"))
			stormGridhttps.getSsloptions().setCertificateFile(configuration.get("connectors", "x509.host-certificate"));
		if (configuration.get("connectors").containsKey("x509.host-key"))
			stormGridhttps.getSsloptions().setKeyFile(configuration.get("connectors", "x509.host-key"));
		if (configuration.get("connectors").containsKey("x509.truststore.directory"))
			stormGridhttps.getSsloptions().setTrustStoreDirectory(configuration.get("connectors", "x509.truststore.directory"));
		if (configuration.get("connectors").containsKey("x509.truststore.refresh-interval"))
			stormGridhttps.getSsloptions().setTrustStoreRefreshIntervalInMsec(
					configuration.get("connectors", "x509.truststore.refresh-interval", long.class));

		/* backend */
		if (!configuration.keySet().contains("backend"))
			throw new Exception("Configuration file 'backend' section missed!");
		if (configuration.get("backend").containsKey("backend.hostname"))
			stormBackend.setHostname(configuration.get("backend", "backend.hostname"));
		if (configuration.get("backend").containsKey("backend.authorization-service.port"))
			stormBackend.setServicePort(configuration.get("backend", "backend.authorization-service.port", int.class));
		if (configuration.get("backend").containsKey("backend.srm-service.port"))
			stormBackend.setPort(configuration.get("backend", "backend.srm-service.port", int.class));
		if (configuration.get("backend").containsKey("srm.endpoint")) {
			stormFrontend.setHostname(configuration.get("backend", "srm.endpoint").split(":")[0]);
			stormFrontend.setPort(Integer.valueOf(configuration.get("backend", "srm.endpoint").split(":")[1]));
		}
	}

	private static void printConfiguration() {
		log.debug("storm backend = " + stormBackend);
		log.debug("storm frontend = " + stormFrontend);
		log.debug("storm gridhttps = " + stormGridhttps);
	}
}