package it.grid.storm;

import java.io.File;

public class Main {

	private static JServer server;
	private static String warTemplateFile = "";
	private static boolean isHttps = false;
	/*
	 * The keystore default-file has been generated by the following command:
	 * keytool -keystore keystore -alias jetty -genkey -keyalg RSA
	 * (from: http://docs.codehaus.org/display/JETTY/How+to+configure+SSL)
	 * If you want to use your own key and certificate you have to generate your own.
	 */
	private static String keystoreFile = getExeDirectory() + "/classes/keystore";;
	private static int serverPort = 8085;

	public static void main(String[] args) {
		
		try {
			parseCommandLine(args);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		server = new JServer(serverPort);
		if ((new File(keystoreFile)).exists() && isHttps) {
			server.initAsHttpsServer(keystoreFile, "password", "password");
		} else {
			server.initAsHttpServer();
		}
		server.setWebappsDirectory(getExeDirectory()+"/webapps");
		
		WebApp webDAVfsServer = new WebApp();
		webDAVfsServer.setName("WebDAV-fs-server");
		webDAVfsServer.setWarFile(warTemplateFile);
		webDAVfsServer.setRootDirectory("/tmp");
		
		try {
			server.start();
			server.deploy(webDAVfsServer);
			
		} catch (Exception e) {
			e.printStackTrace();
			try {
				server.undeployAll();
				server.stop();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
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
		return (new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath())).getParent();
	}
	
	private static void parseCommandLine(String[] args) throws Exception {
		
		MyCommandLineParser cli = new MyCommandLineParser(args);
		cli.addOption("w", "the absolute file path of the WebDAV template webapp [necessary]", true, true);
		cli.addOption("p", "the server port [default = " + serverPort + "]", true, false);
		cli.addOption("ssl", "if keystore exists server works on https", false, false);
		warTemplateFile = cli.getString("w");
		serverPort = cli.getInteger("p");
		isHttps = cli.hasOption("ssl");			
	}

	

}
