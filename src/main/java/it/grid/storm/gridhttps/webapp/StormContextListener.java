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
package it.grid.storm.gridhttps.webapp;

import java.util.concurrent.TimeUnit;

import it.grid.storm.gridhttps.configuration.Configuration;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.italiangrid.voms.ac.VOMSACValidator;
import org.italiangrid.voms.ac.impl.DefaultVOMSValidator;
import org.italiangrid.voms.util.CachingCertificateValidator;
import org.italiangrid.voms.util.CertificateValidatorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.emi.security.authn.x509.X509CertChainValidatorExt;

/**
 * Handles Gridhttps webapp startup/shutdown events.
 * 
 * 
 */
public class StormContextListener implements ServletContextListener {

  public static final long VOMS_CACHE_LIFETIME = TimeUnit.MINUTES.toMillis(1);

  public static final Logger log = LoggerFactory
    .getLogger(StormContextListener.class);

  private void initializeVOMSValidator(ServletContextEvent sce) {

    X509CertChainValidatorExt certVal = new CertificateValidatorBuilder()
      .lazyAnchorsLoading(false).build();

    if (Configuration.getGridhttpsInfo().isVomsCachingEnabled()){
      log.info("VOMS AA certificates validation cache enabled.");
      certVal = new CachingCertificateValidator(certVal, VOMS_CACHE_LIFETIME);
    }

    try {

      VOMSACValidator validator = new DefaultVOMSValidator.Builder()
        .validationListener(new VOMSValidationListener())
        .certChainValidator(certVal).build();

      sce.getServletContext().setAttribute(HttpHelper.VOMS_VALIDATOR_KEY,
        validator);

      log.debug("VOMS validator initialized succesfully.");

    } catch (Throwable t) {

      String msg = String.format("Error initializing VOMS validator: %s",
        t.getMessage());
      log.error(msg, t);

      throw new RuntimeException(msg, t);
    }
  }

  private void shutdownVOMSValidator(ServletContextEvent sce) {

    try {

      VOMSACValidator validator = (VOMSACValidator) sce.getServletContext()
        .getAttribute(HttpHelper.VOMS_VALIDATOR_KEY);

      if (validator != null) {
        validator.shutdown();
        log.debug("VOMS validator stopped succesfully.");
      } else {
        log.warn("VOMS validator not found in application context. Bug?!");
      }

    } catch (Throwable t) {
      String msg = String.format("Error shutting down VOMS validator: %s",
        t.getMessage());

      log.error(msg, t);

      throw new RuntimeException(msg, t);
    }
  }

  private void initializeHTTPClient() {

    StormHTTPClient.INSTANCE.init(200);
  }

  private void disposeHTTPClient() {

    StormHTTPClient.INSTANCE.dispose();
  }

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    log.info("Storm Gridhttps webapp rooted at {} starting...", sce
      .getServletContext().getContextPath());

    initializeVOMSValidator(sce);
    initializeHTTPClient();

    log.info("Storm Gridhttps webapp rooted at {} started.", sce
      .getServletContext().getContextPath());
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

    log.info("Storm Gridhttps webapp rooted at {} stopping...", sce
      .getServletContext().getContextPath());

    shutdownVOMSValidator(sce);
    disposeHTTPClient();

    log.info("Storm Gridhttps webapp rooted at {} stopped.", sce
      .getServletContext().getContextPath());
  }
}
