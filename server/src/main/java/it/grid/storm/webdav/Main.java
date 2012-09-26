package it.grid.storm.webdav;

import it.grid.storm.webdav.server.ServerInfo;
import it.grid.storm.webdav.server.WebApp;
import it.grid.storm.webdav.server.WebApp.WebAppException;
import it.grid.storm.webdav.server.WebDAVServer;
import it.grid.storm.webdav.server.WebDAVServer.ServerException;
import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.storagearea.StorageAreaManager;
import it.grid.storm.webdav.utils.MyCommandLineParser;

import org.italiangrid.utils.https.SSLOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;

public class Main {

	private static WebDAVServer server;
	private static String warTemplateFile;
	// private static String defaultConfigurationFile;
	private static String configurationFile;
	private static List<WebApp> webapps = new ArrayList<WebApp>();;
	private static boolean isTest = false;

	private static ServerInfo httpOptions, httpsOptions;
	private static String stormBEHostname;
	private static int stormBEPort;
	private static String hostname;
	private static String webappsDir;

	public static void main(String[] args) {

		try {
			parseCommandLine(args);
			loadConfiguration(configurationFile);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			server = new WebDAVServer(httpOptions, httpsOptions);
			server.setWebappsDirectory(getExeDirectory() + webappsDir);
			if (isTest) {
				// create a test WebDAV file-system web-application on '/tmp'
				// directory
				WebApp webDAVfsServer;
				webDAVfsServer = new WebApp("/WebDAV-fs-server", "/tmp", warTemplateFile, StorageArea.HTTPS_PROTOCOL);
				webapps.add(webDAVfsServer);
			} else {
				// Retrieve the Storage-Area list from Storm Back-end and
				// generate the Web-Application List
				StorageAreaManager.initFromStormBackend(stormBEHostname, stormBEPort);
				for (StorageArea SA : StorageAreaManager.getStorageAreas()) {
					webapps.add(new WebApp(SA, warTemplateFile));
				}
			}
			// deploy web-applications
			for (WebApp webapp : webapps)
				server.deploy(webapp);
			server.start();
		} catch (ServerException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (WebAppException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

		// adds an handler to CTRL-C that stops and deletes the webapps
		// directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				try {
					server.undeployAll();
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
		// else
		// configurationFile = defaultConfigurationFile;
		isTest = cli.hasOption("test");
	}

	private static void loadConfiguration(String filename) throws InvalidFileFormatException, IOException {
		Wini configuration = new Wini(new File(filename));
		// Web-Applications directory
		webappsDir = configuration.get("options", "webapps_directory");
		// This hostname
		hostname = configuration.get("options", "hostname");
		// Storm BE hostname and port
		stormBEHostname = configuration.get("storm_backend", "hostname");
		stormBEPort = configuration.get("storm_backend", "port", int.class);
		// Http server info
		httpOptions = new ServerInfo(configuration.get("http", "name"), hostname, configuration.get("http", "port", int.class));
		// Https server info
		SSLOptions options = new SSLOptions();
		options.setCertificateFile(configuration.get("https", "certificate_file"));
		options.setKeyFile(configuration.get("https", "key_file"));
		options.setTrustStoreDirectory(configuration.get("https", "trust_store_directory"));
		httpsOptions = new ServerInfo(configuration.get("https", "name"), hostname, configuration.get("https", "port", int.class), options);
	}
}
