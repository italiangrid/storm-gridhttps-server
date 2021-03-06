package it.grid.storm.gridhttps.configuration;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.ini4j.InvalidFileFormatException;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.configuration.exceptions.InitException;

public class Configuration {
	
	private static final Logger log = LoggerFactory.getLogger(Configuration.class);
	
	private static StormGridhttps gridhttpsInfo;
	private static StormBackend backendInfo;
	private static StormFrontend frontendInfo;
	
	public static void loadDefaultConfiguration() throws InitException {

		setGridhttpsInfo(new StormGridhttps());
		setBackendInfo(new StormBackend());
		setFrontendInfo(new StormFrontend());
		
		try {
		  String localhost  = InetAddress.getLocalHost().getCanonicalHostName();
		  
		  log.debug("Localhost resolved as: {}", localhost);
		  
		  getBackendInfo().setHostname(localhost);
		  getFrontendInfo().setHostname(localhost);

		} catch (UnknownHostException e) {
		  log.error(e.getMessage(),e);
			throw new InitException(e);
		}
		
	}

	public static void loadConfigurationFromFile(File conf) throws InitException {
		log.info("loading configuration from file: {}" , conf.toString());
		Wini configuration;
		try {
			configuration = new Wini(conf);
		} catch (InvalidFileFormatException e) {
			throw new InitException(e);
		} catch (IOException e) {
			throw new InitException(e);
		}
		/* service */
		if (!configuration.keySet().contains("service"))
			throw new InitException("Configuration file 'service' section missed!");
		if (configuration.get("service").containsKey("log.configuration-file"))
			getGridhttpsInfo().setLogFile(configuration.get("service", "log.configuration-file"));
		
		if (configuration.get("service").containsKey("max.active.threads"))
			getGridhttpsInfo().setServerActiveThreadsMax(configuration.get("service", "max.active.threads", int.class));
		if (configuration.get("service").containsKey("max.queued.threads"))
			getGridhttpsInfo().setServerQueuedThreadsMax(configuration.get("service", "max.queued.threads", int.class));
		
		if (configuration.get("service").containsKey("voms_caching.enabled"))
		  getGridhttpsInfo().setVomsCachingEnabled(configuration.get("service",
		    "voms_caching.enabled", boolean.class));
		
		if (configuration.get("service").containsKey("hostname")){
		  getGridhttpsInfo().setHostname(configuration.get("service", "hostname"));
		}
		
		/* connectors */
		if (!configuration.keySet().contains("connectors"))
			throw new InitException("Configuration file 'connectors' section missed!");
		if (configuration.get("connectors").containsKey("http.enabled"))
			getGridhttpsInfo().setEnabledHttp(configuration.get("connectors", "http.enabled", boolean.class));
		if (configuration.get("connectors").containsKey("http.port"))
			getGridhttpsInfo().setHttpPort(configuration.get("connectors", "http.port", int.class));
		if (configuration.get("connectors").containsKey("https.port"))
			getGridhttpsInfo().setHttpsPort(configuration.get("connectors", "https.port", int.class));
		if (configuration.get("connectors").containsKey("mapper.servlet.port"))
			getGridhttpsInfo().getMapperServlet().setPort(configuration.get("connectors", "mapper.servlet.port", int.class));
		if (configuration.get("connectors").containsKey("x509.host-certificate"))
			getGridhttpsInfo().getSsloptions().setCertificateFile(configuration.get("connectors", "x509.host-certificate"));
		if (configuration.get("connectors").containsKey("x509.host-key"))
			getGridhttpsInfo().getSsloptions().setKeyFile(configuration.get("connectors", "x509.host-key"));
		if (configuration.get("connectors").containsKey("x509.truststore.directory"))
			getGridhttpsInfo().getSsloptions().setTrustStoreDirectory(configuration.get("connectors", "x509.truststore.directory"));
		if (configuration.get("connectors").containsKey("x509.truststore.refresh-interval"))
			getGridhttpsInfo().getSsloptions().setTrustStoreRefreshIntervalInMsec(
					configuration.get("connectors", "x509.truststore.refresh-interval", long.class));

		/* backend */
		if (!configuration.keySet().contains("backend"))
			throw new InitException("Configuration file 'backend' section missed!");
		if (configuration.get("backend").containsKey("backend.hostname"))
			getBackendInfo().setHostname(configuration.get("backend", "backend.hostname"));
		if (configuration.get("backend").containsKey("backend.authorization-service.port"))
			getBackendInfo().setServicePort(configuration.get("backend", "backend.authorization-service.port", int.class));
		if (configuration.get("backend").containsKey("backend.srm-service.port"))
			getBackendInfo().setPort(configuration.get("backend", "backend.srm-service.port", int.class));
		if (configuration.get("backend").containsKey("backend.xmlrpc.token"))
			getBackendInfo().setToken(configuration.get("backend", "backend.xmlrpc.token", String.class));
		if (configuration.get("backend").containsKey("srm.endpoint")) {
			getFrontendInfo().setHostname(configuration.get("backend", "srm.endpoint").split(":")[0]);
			getFrontendInfo().setPort(Integer.valueOf(configuration.get("backend", "srm.endpoint").split(":")[1]));
		}
		if (configuration.get("backend").containsKey("compute-checksum"))
			getGridhttpsInfo().setComputeChecksum(configuration.get("backend", "compute-checksum", boolean.class));
		log.info("configuration successfully loaded");
	}
	
	public static void checkConfiguration() throws InitException {
		log.info("checking backend configuration...");
		getBackendInfo().checkConfiguration();
		log.info("checking frontend configuration...");
		getFrontendInfo().checkConfiguration();
		log.info("checking gridhttps configuration...");
		getGridhttpsInfo().checkConfiguration();
	}
	
	public static StormGridhttps getGridhttpsInfo() {
		return gridhttpsInfo;
	}

	private static void setGridhttpsInfo(StormGridhttps gridhttpsInfo) {
		Configuration.gridhttpsInfo = gridhttpsInfo;
	}

	public static StormBackend getBackendInfo() {
		return backendInfo;
	}

	private static void setBackendInfo(StormBackend backendInfo) {
		Configuration.backendInfo = backendInfo;
	}

	public static StormFrontend getFrontendInfo() {
		return frontendInfo;
	}

	private static void setFrontendInfo(StormFrontend frontendInfo) {
		Configuration.frontendInfo = frontendInfo;
	}
	
}