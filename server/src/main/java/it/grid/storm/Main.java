package it.grid.storm;

import java.util.HashMap;
import java.util.Map;

public class Main {

	private static JServer server;
	private static WebAppBuilder builder;

	private static String warTemplateFile = "/Users/eVianello/Desktop/WebDAV-ciccio.war";
	private static String webappsDirectory = "./webapps";

	private static void parseCommandLine(String[] args) {

		// Parsing of command line's options
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the war template absolute file path", true, false);
		try {
			warTemplateFile = cli.getString("w");
		} catch (Exception e) {}

	}

	public static void main(String[] args) {

		parseCommandLine(args);

		server = new JServer(8085,"server/target/classes/keystore","password","password");
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
