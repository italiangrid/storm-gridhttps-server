package it.grid.storm;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jetty.util.log.Log;

public class Main {

	private static JServer server;
	private static WebAppBuilder builder;

	private static String warTemplateFile = "";
	private static String webappsDirectory = "./webapps";
	
	/*
	 * The keystore default-file has been generated by the following command:
	 * keytool -keystore keystore -alias jetty -genkey -keyalg RSA
	 * (from: http://docs.codehaus.org/display/JETTY/How+to+configure+SSL)
	 * If you want to use your own key and certificate you have to generate your own.
	 */
	private static String keystoreFile = getExeDirectory() + "/classes/keystore";;
	private static int serverPort = 8085;

	private static String getExeDirectory() {
		return (new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParent();
	}
	
	private static void parseCommandLine(String[] args) {
		
		// Parsing of command line's options
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV template webapp [necessary]", true, true);
		cli.addOption("p", "the server port [default = " + serverPort + "]", true, false);
		try {
			warTemplateFile = cli.getString("w");
		} catch (Exception e) {
		}
		try {
			serverPort = cli.getInteger("p");
		} catch (Exception e) {
		}

	}

	static public boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory()) {
					deleteDirectory(files[i]);
				} else {
					files[i].delete();
				}
			}
		}
		return (path.delete());
	}

	public static void main(String[] args) {

		parseCommandLine(args);
		
		if (!(new File(keystoreFile)).exists()) {
			// HTTP server
			server = new JServer(serverPort);
		} else {
			// HTTPS server
			server = new JServer(serverPort, keystoreFile, "password", "password");
		}

		builder = new WebAppBuilder();

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("rootd", "/tmp");
		attributes.put("name", "WebDAV-fs-server");
		attributes.put("outputd", webappsDirectory + "/" + attributes.get("name"));
		attributes.put("template", warTemplateFile);

		try {
			server.start();
			builder.addWebApp(attributes);
			server.deploy("/" + attributes.get("name"),
					attributes.get("outputd"));

		} catch (Exception e) {
			e.printStackTrace();
			try {
				server.stop();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			// remove webapps dir
			deleteDirectory(new File(webappsDirectory));
		}

		//adds an handler to CTRL-C that stops and deletes the webapps directory
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() { /*
								 * shutdown code
								 */
				try {
					server.stop();
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				// remove webapps dir
				Log.info("SERVER: Removing webapps temporary directory ["+webappsDirectory+"] ");
				deleteDirectory(new File(webappsDirectory));
				Log.info("SERVER: Removed!");
			}
		});

	}

}
