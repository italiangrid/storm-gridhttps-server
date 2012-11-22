package it.grid.storm.webdav;

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
	public final static int STORM_GRIDHTTPS_HTTP_PORT = 8085;
	public final static int STORM_GRIDHTTPS_HTTPS_PORT = 8443;
	public final static boolean STORM_GRIDHTTPS_USE_HTTP = true;
	public final static boolean STORM_GRIDHTTPS_HTTPS_WANT_CLIENT_AUTH = true;
	public final static boolean STORM_GRIDHTTPS_HTTPS_NEED_CLIENT_AUTH = true;
	public final static String WEBAPPS_DIRECTORY_ROOT = "/var/lib/storm";
	public final static String WEBAPPS_DIRECTORY_NAME = "webapps";
	public final static String WEBAPPS_DIRECTORY = WEBAPPS_DIRECTORY_ROOT + "/" + WEBAPPS_DIRECTORY_NAME;
	public final static String LOG_FILE = "/etc/storm/gridhttps-server/logback.xml";
	public final static String HTTPS_CERTIFICATE_FILE = "/etc/grid-security/storm/hostcert.pem";
	public final static String HTTPS_KEY_FILE = "/etc/grid-security/storm/hostkey.pem";
	public final static String HTTPS_TRUST_STORE_DIRECTORY = "/etc/grid-security/certificates";
}