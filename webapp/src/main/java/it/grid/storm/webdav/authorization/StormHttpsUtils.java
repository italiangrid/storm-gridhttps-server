package it.grid.storm.webdav.authorization;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;

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

	private static final String[] ALLOWED_METHODS = { "PROPFIND", "OPTIONS",
			"GET", "DELETE", "PUT", "MKCOL", "MOVE", "COPY" };

	private static final HashMap<String, String[]> METHODS_MAP = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 1L;
		{
			// Usage:
			// put(METHOD_NAME, new String[]{OPERATION_ON_SOURCE_PATH , OPERATION_ON_DESTINATION_PATH})
			// OPERATION_ON_DESTINATION_PATH is not mandatory (only COPY and MOVE have it)
			put("OPTIONS", new String[] {});
			put("GET", new String[] { "srmPrepareToGet" });
			put("PUT", new String[] { "srmPrepareToPut" }); // da modificare in srmPrepareToPutOverwrite se il flag e' true
			put("DELETE", new String[] { "srmRemove" }); // lato StoRM poi viene controllata anche la srmRemoveDir
			put("PROPFIND", new String[] { "srmLs" });
			put("MKCOL", new String[] { "srmMakeDir" });
			put("COPY", new String[] { "srmCopyFrom", "srmCopyTo" });
			put("MOVE", new String[] { "srmMoveSource", "srmMoveDest" });
		};
	};

	/* Constants for prepareURI */

	private static final String ENCODING_SCHEME = "UTF-8";
	private static final String RESOURCE = "authorization";
	private static final String VERSION = "1.0";

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
	public static final String SERVLET_CONTEXT_PATH = "WebDAV-fs-server";
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

	public static String convertToStorageAreaPath(String uri_string)
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