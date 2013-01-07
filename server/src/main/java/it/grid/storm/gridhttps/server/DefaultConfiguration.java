package it.grid.storm.gridhttps.server;

public class DefaultConfiguration {

	/* mapper servlet context path */
	public final static String MAPPER_SERVLET_CONTEXT_PATH = "gridhttps_webapp";
	public final static String MAPPER_SERVLET_CONTEXT_SPEC = "resourceMapping";

	/* storm backend ports */
	public final static int STORM_BE_PORT = 8080;
	public final static int STORM_BE_SERVICE_PORT = 9998;

	/* storm frontend port */
	public final static int STORM_FE_PORT = 8444;

	/* storm gridhttps */
	public final static int STORM_GHTTPS_HTTP_PORT = 8085;
	public final static boolean STORM_GHTTPS_USE_HTTP = false;	
	public final static int STORM_GHTTPS_HTTPS_PORT = 8443;
	public final static boolean STORM_GHTTPS_HTTPS_WANT_CLIENT_AUTH = true;
	public final static boolean STORM_GHTTPS_HTTPS_NEED_CLIENT_AUTH = true;
	public final static String STORM_GHTTPS_HTTPS_CERTIFICATE_FILE = "/etc/grid-security/gridhttps/hostcert.pem";
	public final static String STORM_GHTTPS_HTTPS_KEY_FILE = "/etc/grid-security/gridhttps/hostkey.pem";
	public final static String STORM_GHTTPS_HTTPS_TRUST_STORE_DIRECTORY = "/etc/grid-security/certificates";
	public final static long STORM_GHTTPS_DEFAULT_TRUST_STORE_REFRESH_INTERVAL_IN_MSECS = 600000L;
	public final static String STORM_GHTTPS_WEBAPPS_DIRECTORY = "/var/lib/storm";	
	public final static String STORM_GHTTPS_LOG_FILE = "/etc/storm/gridhttps-server/logback.xml";
	public final static String WEBAPP_DIRECTORY_NAME = "gridhttps-server/webapp";
	
	public final static String WEBAPP_CONTEXTPATH = "";
	public final static String FILETRANSFER_CONTEXTPATH = "fileTransfer";
	
	public final static String ROOTDIRECTORY = "/";
	public final static boolean COMPUTE_CHECKSUM = true;
	public final static String CHECKSUM_TYPE = "adler32";
}
