/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.configuration.exceptions.InitException;
import it.grid.storm.gridhttps.server.exceptions.ServerException;
import it.grid.storm.gridhttps.server.utils.CommandLineArgsParser;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import eu.emi.security.authn.x509.impl.CertificateUtils;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);
  private static StormGridhttpsServer server;
  private static String configurationFileName;

  public static void main(String[] args) {

    CertificateUtils.configureSecProvider();

    log.info("StoRM Gridhttps-server - bootstrapping...");

    try {
      parseCommandLine(args);
      Configuration.loadDefaultConfiguration();
      Configuration.loadConfigurationFromFile(new File(configurationFileName));
      Configuration.checkConfiguration();
      initLogging(Configuration.getGridhttpsInfo().getLogFile());
      initStorageAreas(Configuration.getBackendInfo().getHostname(),
        Configuration.getBackendInfo().getServicePort());
      server = new StormGridhttpsServer(Configuration.getGridhttpsInfo());
      server.start();
      server.status();
    } catch (InitException e) {
      log.error(e.getMessage(),e);
      System.exit(1);
    } catch (ServerException e) {
      log.error(e.getMessage(),e);
      System.exit(1);
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
              log.error(e.getMessage(),e);
            }
          }
        }
        log.info("Bye");
      }
    });

  }

  private static void initStorageAreas(String hostname, int port)
    throws InitException {

    try {
      StorageAreaManager.init(hostname, port);
    } catch (Exception e) {
      throw new InitException(e);
    }
  }

  private static void initLogging(String logFilePath) throws InitException {

    log.info("Configuring logging from {}", logFilePath);

    File f = new File(logFilePath);
    if (!f.exists() || !f.canRead()) {
      String message = String.format("Error loading logging configuration: "
        + "'%s' does not exist or is not readable.", logFilePath);
      throw new RuntimeException(message);
    }

    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
    JoranConfigurator configurator = new JoranConfigurator();

    configurator.setContext(lc);
    lc.reset();

    try {
      configurator.doConfigure(logFilePath);
    } catch (JoranException e) {
      throw new RuntimeException(e);
    }

    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
  }

  private static void parseCommandLine(String[] args) throws InitException {

    log.info("received as command line arguments: ");
    CommandLineArgsParser cli = new CommandLineArgsParser(args);
    cli.addOption("conf",
      "the absolute file path of the server configuration file [mandatory]",
      true, true);
    try {
      configurationFileName = cli.getString("conf");
      log.info("configuration-file: {}" , configurationFileName);
    } catch (Exception e) {
      throw new InitException(e);
    }
  }
}
