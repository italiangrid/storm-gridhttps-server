package it.grid.storm;

import java.util.HashMap;
import java.util.Map;

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
		} catch (Exception e) {}
		try {
			keystoreFile = cli.getString("k");
		} catch (Exception e) {}
		try {
			serverPort = cli.getInteger("p");
		} catch (Exception e) {}

	}

	public static void main(String[] args) {

		parseCommandLine(args);

		if (keystoreFile.compareTo("")==0) {
			//HTTP server
			server = new JServer(serverPort);
		} else {
			//HTTPS server
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
		}

	}

}
