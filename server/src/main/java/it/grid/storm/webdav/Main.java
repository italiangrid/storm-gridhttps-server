package it.grid.storm.webdav;

import it.grid.storm.webdav.server.WebApp;
import it.grid.storm.webdav.server.WebApp.WebAppException;
import it.grid.storm.webdav.server.WebDAVServer;
import it.grid.storm.webdav.server.WebDAVServer.ServerException;
import it.grid.storm.webdav.storagearea.StorageArea;
import it.grid.storm.webdav.storagearea.StorageAreaManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Main {

	private final static String hostnameBEStorm = "omii005-vm03.cnaf.infn.it"; // "etics-06-vm03.cnaf.infn.it";
	private final static int portBEStorm = 9998;
	
	private static WebDAVServer server;
	private static String warTemplateFile = "";
	
	private static List<WebApp> webapps = new ArrayList<WebApp>();;
	
	private static boolean isTest = false;

	public static void main(String[] args) {
		
		try {
			parseCommandLine(args);
		} catch (Exception e) {
		
		}
		try {	
			server = new WebDAVServer();
			server.setWebappsDirectory(getExeDirectory()+"/webapps");
			//server.start();
			if (isTest) {
				// create a test WebDAV file-system web-application on '/tmp' directory
				WebApp webDAVfsServer;			
				webDAVfsServer = new WebApp("/WebDAV-fs-server","/tmp",warTemplateFile, StorageArea.HTTP_AND_HTTPS_PROTOCOLS);
				webapps.add(webDAVfsServer);
			} else {
				// Retrieve the Storage-Area list from Storm Back-end and generate the Web-Application List
				StorageAreaManager.initFromStormBackend(hostnameBEStorm, portBEStorm);
				List<StorageArea> storageareas = StorageAreaManager.getStorageAreas();
				for (StorageArea SA : storageareas) {
					webapps.add(new WebApp(SA,warTemplateFile));
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
		
		
		//adds an handler to CTRL-C that stops and deletes the webapps directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() { /*
								 * shutdown code
								 */
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
		return (new File(Main.class.getProtectionDomain().getCodeSource()
				.getLocation().getPath())).getParent();
	}

	private static void parseCommandLine(String[] args) throws Exception {

		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption(
				"w",
				"the absolute file path of the WebDAV template webapp [necessary]",
				true, true);
		cli.addOption("test",
				"create WebDAV-fs-server test webapp on root /tmp", false,
				false);
		warTemplateFile = cli.getString("w");
		isTest = cli.hasOption("test");
	}

}
