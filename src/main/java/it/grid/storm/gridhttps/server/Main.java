/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.configuration.exceptions.InitException;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.utils.CommandLineArgsParser;
import it.grid.storm.storagearea.StorageAreaManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Main {

	private static Logger log = LoggerFactory.getLogger(Main.class);
	private static StormGridhttpsServer server;
	private static String configurationFileName;

	public static void main(String[] args) {

		System.out.println("StoRM Gridhttps-server");
		System.out.println("bootstrapping...");

		waitfor(5000);

		try {
			parseCommandLine(args);
			Configuration.loadDefaultConfiguration();
			Configuration.loadConfigurationFromFile(new File(configurationFileName));
			Configuration.checkConfiguration();
			initLogging(Configuration.getGridhttpsInfo().getLogFile());
			initStorageAreas(Configuration.getBackendInfo().getHostname(), Configuration.getBackendInfo().getServicePort());
			server = new StormGridhttpsServer(Configuration.getGridhttpsInfo());
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
				log.debug("Exiting gridhttps-server...");
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
				log.info("Bye");
			}
		});

	}

	private static void waitfor(int waitfor) {
		if (waitfor == 0)
			return;
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

	private static void initStorageAreas(String hostname, int port) throws InitException {
		try {
			StorageAreaManager.init(hostname, port);
		} catch (Exception e) {
			throw new InitException(e);
		}
	}

	private static void initLogging(String logFilePath) throws InitException {
		/* INIT LOGGING COMPONENT */
		System.out.println("init logging...");

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

		try {
			FileInputStream fin = new FileInputStream(logFilePath);
			JoranConfigurator configurator = new JoranConfigurator();
			configurator.setContext(context);
			context.reset();
			configurator.doConfigure(fin);
			fin.close();
		} catch (JoranException je) {
			// StatusPrinter will handle this
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		StatusPrinter.printInCaseOfErrorsOrWarnings(context);
		log.info("log successfully initialized");
		System.out.println("log successfully initialized");
	}

	private static void parseCommandLine(String[] args) throws InitException {
		System.out.println("received as command line arguments: ");
		CommandLineArgsParser cli = new CommandLineArgsParser(args);
		cli.addOption("conf", "the absolute file path of the server configuration file [mandatory]", true, true);
		try {
			configurationFileName = cli.getString("conf");
			System.out.println("configuration-file: " + configurationFileName);
		} catch (Exception e) {
			throw new InitException(e);
		}
	}
}