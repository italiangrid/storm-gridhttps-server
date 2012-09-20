package it.grid.storm.webdav;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormHttpsUtils {

	private static final Logger log = LoggerFactory
			.getLogger(StormHttpsUtils.class);

	private static final String HTTP_SCHEMA = "http";
	private static final String HTTPS_SCHEMA = "https";

	// private static final String[] READ_METHODS = { "PROPFIND", "OPTIONS",
	// "GET", "HEAD" };
	// private static final String[] WRITE_METHODS = { "DELETE", "PUT", "MKCOL",
	// "MOVE", "COPY" };
	//
	// private static final String[] ALLOWED_METHODS = (String[]) ArrayUtils
	// .addAll(READ_METHODS, WRITE_METHODS);

	private static final String[] ALLOWED_METHODS = { "PROPFIND", "OPTIONS",
			"GET", "DELETE", "PUT", "MKCOL", "MOVE", "COPY" };

	private static final HashMap<String, String[]> METHODS_MAP = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 1L;
		{
			put("OPTIONS", new String[] { "ping" });
			put("GET", new String[] { "get" });
			put("PUT", new String[] { "put" });
			put("DELETE", new String[] { "delete" });
			put("PROPFIND", new String[] { "list" });
			put("MKCOL", new String[] { "mkdir" });
			put("COPY", new String[] { "get", "put" });
			put("MOVE", new String[] { "delete", "put" });
			// N.B.: for the MOVE method, if the user can delete a resource, he
			// also can read it !!
		};

	};

	/* Constants for prepareURI */

	private static final String ENCODING_SCHEME = "UTF-8";
	private static final String RESOURCE = "authorization";
	private static final String VERSION = "1.0";
	// private static final String READ_OPERATION = "read";
	// private static final String WRITE_OPERATION = "write";

	/*
	 * 
	 * Per i seguenti metodi sulla sinistra controllare che l'utente sia
	 * autorizzato a fare i comandi sulla destra:
	 * 
	 * OPTIONS -> ping GET -> get (prepare to get , reserve space) PUT -> put
	 * (prepare to put , put done) DELETE -> delete (rm , rmdir) PROPFIND -> lis
	 * MKCOL -> mkdir COPY -> cp -> si puo' pensare di fare una get sulla source
	 * e una put sulla destination MOVE -> cp , rm -> si puo' pensare di fare
	 * una get sulla source, una put sulla destination e una delete sulla source
	 * 
	 * 
	 * 
	 * 
	 * WRITE_FILE('W', "WRITE_FILE", "Write data"),    READ_FILE('R',
	 * "READ_FILE", "Read data"),    RENAME('F', "RENAME",
	 * "Rename a file or a directory"),    DELETE('D', "DELETE",
	 * "Delete a file or a directory"),    // TRAVERSE_DIRECTORY('T',
	 * "TRAVERSE_DIRECTORY", "Traverse a directory"),    LIST_DIRECTORY('L',
	 * "LIST_DIRECTORY", "Listing a directory"),    MAKE_DIRECTORY('M',
	 * "CREATE_DIRECTORY", "Create a directory"),    CREATE_FILE('N',
	 * "CREATE_FILE", "Create a new file"),
	 */

	private static final String VOMS_EXTENSIONS = "voms";
	private static final String PLAIN = "plain";
	private static final String USER = "user";
	private static final String DN_KEY = "DN";
	private static final String FQANS_KEY = "FQANS";
	private static final String FQANS_SEPARATOR = ",";

	/* Constants only for test */

	public static final String STORM_BE_HOSTNAME = "etics-06-vm03.cnaf.infn.it";
	public static final int STORM_BE_PORT = 9998;

	public static final String SUBJECT_DN = "CN=Matteo Manzali,L=CNAF,OU=Personal Certificate,O=INFN,C=IT";
	public static final String STORM_SA_ROOTDIR = "/storage/dteam";
	public static final String SERVLET_CONTEXT_PATH = "dteam";
	/* Public methods */

	public static boolean methodAllowed(String method) {
		boolean response = false;
		for (String allowedMethod : ALLOWED_METHODS) {
			if (allowedMethod.equals(method)) {
				response = true;
				log.debug("Method " + method + " is allowed");
				break;
			}
		}
		return response;
	}

	public static String sourceOperation(String method) {
		String operations[] = METHODS_MAP.get(method);
		String ret;
		try {
			ret = operations[0];
		} catch (IndexOutOfBoundsException e) {
			ret = null;
		}
		return ret;
	}

	public static String destinationOperation(String method) {
		String operations[] = METHODS_MAP.get(method);
		String ret;
		try {
			ret = operations[1];
		} catch (IndexOutOfBoundsException e) {
			ret = null;
		}
		return ret;
	}

	public static boolean checkSchema(String schema) {
		boolean response = false;
		if (schema != null
				&& (schema.equals(HTTP_SCHEMA) || schema.equals(HTTPS_SCHEMA))) {
			response = true;
			log.debug("Schema " + schema + " is valid");
		}
		return response;
	}

	public static String prepareResourcePath(String uri_string)
			throws ServletException {
		URI uri;
		try {
			uri = new URI(uri_string);
		} catch (URISyntaxException e) {
			throw new ServletException(
					"Unable to create URI object from the string: "
							+ uri_string);
		}
		String path = uri.getPath().replaceFirst(SERVLET_CONTEXT_PATH, "").replace("//", "/");
		return STORM_SA_ROOTDIR + path;
	}

	public static URI prepareURI(String resourcePath, String operation,
			String subjectDN, String[] fqans) throws ServletException,
			IllegalArgumentException {
		if (resourcePath == null || operation == null || subjectDN == null
				|| fqans == null) {
			log.error("Received null parameter(s) at prepareURL: resourcePath="
					+ resourcePath + " operation=" + operation + " subjectDN="
					+ subjectDN + " fqans=" + Arrays.toString(fqans));
			throw new IllegalArgumentException("Received null parameter(s)");
		}
		log.debug("Encoding Authorization request parameters");
		String path;
		try {
			path = buildpath(URLEncoder.encode(resourcePath, ENCODING_SCHEME),
					operation, fqans.length > 0);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception encoding the path \'" + resourcePath
					+ "\' UnsupportedEncodingException: " + e.getMessage());
			throw new ServletException(
					"Unable to encode resourcePath paramether, unsupported encoding \'"
							+ ENCODING_SCHEME + "\'");
		}
		String fqansList = null;
		if (fqans.length > 0) {
			fqansList = "";
			for (int i = 0; i < fqans.length; i++) {
				if (i > 0) {
					fqansList += FQANS_SEPARATOR;
				}
				fqansList += fqans[i];
			}
		}
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair(DN_KEY, subjectDN));
		if (fqansList != null) {
			qparams.add(new BasicNameValuePair(FQANS_KEY, fqansList));
		}

		URI uri;
		try {
			uri = new URI("http", null, STORM_BE_HOSTNAME, STORM_BE_PORT, path,
					URLEncodedUtils.format(qparams, "UTF-8"), null);
		} catch (URISyntaxException e) {
			log.error("Unable to build Authorization Service URI. URISyntaxException "
					+ e.getLocalizedMessage());
			throw new ServletException(
					"Unable to build Authorization Service URI");
		}
		log.debug("Prepared URI : " + uri);
		return uri;
	}

	/* Private methods */

	private static String buildpath(String resourcePath, String operation,
			boolean hasVOMSExtension) throws UnsupportedEncodingException,
			ServletException {
		String path = "/" + RESOURCE + "/" + VERSION + "/" + resourcePath + "/"
				+ URLEncoder.encode(operation, ENCODING_SCHEME) + "/";
		if (hasVOMSExtension) {
			path += VOMS_EXTENSIONS + "/";
		} else {
			path += PLAIN + "/";
		}
		log.debug("Built path " + path + USER);
		return path + USER;
	}

}