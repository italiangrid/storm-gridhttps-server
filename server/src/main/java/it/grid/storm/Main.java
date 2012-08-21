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
	private static String keystoreFile = "";
	private static int serverPort = 8085;

	private static void parseCommandLine(String[] args) {

		// Parsing of command line's options
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the war template absolute file path", true, true);
		cli.addOption("k", "the keystore absolute file path", true, false);
		cli.addOption("p", "the server port", true, false);
		try {
			warTemplateFile = cli.getString("w");
		} catch (Exception e) {
		}
		try {
			keystoreFile = cli.getString("k");
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

		if (keystoreFile.compareTo("") == 0) {
			// HTTP server
			server = new JServer(serverPort);
		} else {
			// HTTPS server
			server = new JServer(serverPort, keystoreFile, "password",
					"password");
		}

		builder = new WebAppBuilder();

		Map<String, String> attributes = new HashMap<String, String>();
		attributes.put("rootd", "/tmp");
		attributes.put("name", "WebDAV-fs-server");
		attributes.put("outputd",
				webappsDirectory + "/" + attributes.get("name"));
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
				Log.info("Removing webapps temporary directory ["+webappsDirectory+"] ");
				deleteDirectory(new File(webappsDirectory));
				Log.info("Removed!");
			}
		});

	}

}
