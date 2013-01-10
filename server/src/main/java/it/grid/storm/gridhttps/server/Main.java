package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.data.StormBackend;
import it.grid.storm.gridhttps.server.data.StormFrontend;
import it.grid.storm.gridhttps.server.data.StormGridhttps;
import it.grid.storm.gridhttps.server.exceptions.InitException;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.utils.FileUtils;
import it.grid.storm.gridhttps.server.utils.MyCommandLineParser;
import it.grid.storm.storagearea.StorageAreaManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;

public class Main {

	private static Logger log;

	public static String webappFileName;
	public static String configurationFileName;

	private static StormBackend stormBackend;
	private static StormFrontend stormFrontend;
	private static StormGridhttps stormGridhttps;
	private static StormGridhttpsServer server;

	public static void main(String[] args) {

		System.out.println("StoRM Gridhttps-server");
		System.out.println("bootstrapping...");
		
		waitfor(5000);

		try {
			parseCommandLine(args);
			loadDefaultConfiguration();
			loadConfiguration();
			checkConfiguration();
			initLogging(stormGridhttps.getLogFile());
			initStorageAreas(stormBackend.getHostname(), stormBackend.getServicePort());
			printConfiguration();
			deleteWebappDirectory();
			server = new StormGridhttpsServer(stormGridhttps, stormBackend, stormFrontend);
			server.start();
			server.status();
		} catch (InitException e) {
			System.err.println(e.getMessage());
			System.exit(0);
		} catch (ServerException e) {
			log.error(e.getMessage());
			System.exit(0);
		}

		// adds an handler to CTRL-C that stops and deletes the webapp directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.info("Exiting gridhttps-server...");
				if (server != null) {
					if (server.isRunning()) {
						log.info("Shutting down server...");
						try {
							server.stop();
						} catch (Exception e) {
							log.error(e.getMessage());
						}
					}
				}
				log.info("Deleting webapp temporary directory...");
				deleteWebappDirectory();
				log.info("Bye");
			}
		});

	}

	private static void waitfor(int waitfor) {
		if (waitfor == 0) return;
		Object lock = new Object();
		System.out.println("Waiting for " + waitfor + " ms");
		synchronized (lock) {
			try {
				lock.wait(waitfor);
				System.out.println("It's time to boot!");
			} catch (InterruptedException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private static void deleteWebappDirectory() {
		File toDelete = new File(stormGridhttps.getWebappsDirectory(), DefaultConfiguration.WEBAPP_DIRECTORY_NAME);
		if (toDelete.exists()) {
			try {
				FileUtils.deleteDirectory(toDelete);
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}
	}

	private static void initStorageAreas(String hostname, int port) throws InitException {
		try {
			StorageAreaManager.init(hostname, port);
		} catch (Exception e) {
			throw new InitException(e);
		}
	}

	private static void checkConfiguration() throws InitException {
		System.out.println("checking backend configuration...");
		stormBackend.checkConfiguration();
		System.out.println("checking frontend configuration...");
		stormFrontend.checkConfiguration();
		System.out.println("checking gridhttps configuration...");
		stormGridhttps.checkConfiguration();
	}

	private static void initLogging(String logFilePath) throws InitException {
		/* INIT LOGGING COMPONENT */
		System.out.println("init logging...");
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		JoranConfigurator configurator = new JoranConfigurator();
		configurator.setContext(loggerContext);
		try {
			FileInputStream fin = new FileInputStream(logFilePath);
			configurator.doConfigure(fin);
			fin.close();
		} catch (FileNotFoundException e) {
			throw new InitException(e);
		} catch (JoranException e) {
			throw new InitException(e);
		} catch (IOException e) {
			throw new InitException(e);
		}
		loggerContext.start();
		log = LoggerFactory.getLogger(Main.class);
		System.out.println("log successfully initialized");
	}

	private static void parseCommandLine(String[] args) throws InitException {
		System.out.println("received as command line arguments: ");
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV webapp template [mandatory]", true, true);
		cli.addOption("conf", "the absolute file path of the server configuration file [mandatory]", true, true);
		try {
			webappFileName = cli.getString("w");
			System.out.println("webapp-file: " + webappFileName);
			configurationFileName = cli.getString("conf");
			System.out.println("configuration-file: " + configurationFileName);
		} catch (Exception e) {
			throw new InitException(e);
		}
	}

	private static void loadDefaultConfiguration() throws InitException {
		/* gridhttps */
		stormGridhttps = new StormGridhttps();
		java.net.InetAddress localMachine;
		try {
			localMachine = java.net.InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			throw new InitException(e);
		}
		stormGridhttps.setHostname(localMachine.getHostName());
		stormGridhttps.setWarFile(new File(webappFileName));
		/* backend */
		stormBackend = new StormBackend();
		stormBackend.setHostname(localMachine.getHostName());
		/* frontend */
		stormFrontend = new StormFrontend();
		stormFrontend.setHostname(localMachine.getHostName());
	}

	private static void loadConfiguration() throws InitException {
		System.out.println("loading configuration from file...");
		Wini configuration;
		try {
			configuration = new Wini(new File(configurationFileName));
		} catch (InvalidFileFormatException e) {
			throw new InitException(e);
		} catch (IOException e) {
			throw new InitException(e);
		}
		/* service */
		if (!configuration.keySet().contains("service"))
			throw new InitException("Configuration file 'service' section missed!");
		if (configuration.get("service").containsKey("log.configuration-file"))
			stormGridhttps.setLogFile(configuration.get("service", "log.configuration-file"));
		if (configuration.get("service").containsKey("webapp-directory"))
			stormGridhttps.setWebappsDirectory(configuration.get("service", "webapp-directory"));
		if (configuration.get("service").containsKey("webdav.context-path"))
			stormGridhttps.setWebdavContextPath(configuration.get("service", "webdav.context-path"));
		if (configuration.get("service").containsKey("filetransfer.context-path"))
			stormGridhttps.setFiletransferContextPath(configuration.get("service", "filetransfer.context-path"));

		/* connectors */
		if (!configuration.keySet().contains("connectors"))
			throw new InitException("Configuration file 'connectors' section missed!");
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
			throw new InitException("Configuration file 'backend' section missed!");
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
		if (configuration.get("backend").containsKey("root-directory"))
			stormGridhttps.setRootDirectory(new File(configuration.get("backend", "root-directory")));
		if (configuration.get("backend").containsKey("compute-checksum"))
			stormGridhttps.setComputeChecksum(configuration.get("backend", "compute-checksum", boolean.class));
		if (configuration.get("backend").containsKey("checksum-type"))
			stormGridhttps.setChecksumType(configuration.get("backend", "checksum-type"));
		
		System.out.println("configuration successfully loaded");
	}

	private static void printConfiguration() {
		log.debug("storm backend = " + stormBackend);
		log.debug("storm frontend = " + stormFrontend);
		log.debug("storm gridhttps = " + stormGridhttps);
	}
}