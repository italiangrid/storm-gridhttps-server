package it.grid.storm.webdav.authorization;

import it.grid.storm.webdav.authorization.methods.*;

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

public class StormAuthorizationUtils {

	private static final Logger log = LoggerFactory
			.getLogger(StormAuthorizationUtils.class);
	
	public static final HashMap<String, AbstractMethodAuthorization> METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>() {
		private static final long serialVersionUID = 1L;
		{
			put("PROPFIND", new PropfindMethodAuthorization());
			put("OPTIONS", new OptionsMethodAuthorization());
			put("GET", new GetMethodAuthorization());
			put("DELETE", new DeleteMethodAuthorization());
			put("PUT", new PutMethodAuthorization());
			put("MKCOL", new MkcolMethodAuthorization());
			put("MOVE", new MoveMethodAuthorization());
			put("COPY", new CopyMethodAuthorization());
		};
	};

	/* Constants only for test */

	public static final String STORM_BE_HOSTNAME = "etics-06-vm03.cnaf.infn.it";
	public static final int STORM_BE_PORT = 9998;
	public static final String SUBJECT_DN = "CN=Matteo Manzali,L=CNAF,OU=Personal Certificate,O=INFN,C=IT";
	
	/* Public methods */

	public static boolean methodAllowed(String method) {
		boolean response = false;
		if(METHODS_MAP.containsKey(method)){
			log.debug("Method " + method + " is allowed");
			response = true;
		}
		return response;
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
			path = buildpath(
					URLEncoder.encode(resourcePath, Constants.ENCODING_SCHEME),
					operation, fqans.length > 0);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception encoding the path \'" + resourcePath
					+ "\' UnsupportedEncodingException: " + e.getMessage());
			throw new ServletException(
					"Unable to encode resourcePath paramether, unsupported encoding \'"
							+ Constants.ENCODING_SCHEME + "\'");
		}
		String fqansList = null;
		if (fqans.length > 0) {
			fqansList = "";
			for (int i = 0; i < fqans.length; i++) {
				if (i > 0) {
					fqansList += Constants.FQANS_SEPARATOR;
				}
				fqansList += fqans[i];
			}
		}
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair(Constants.DN_KEY, subjectDN));
		if (fqansList != null) {
			qparams.add(new BasicNameValuePair(Constants.FQANS_KEY, fqansList));
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
		String path = "/" + Constants.RESOURCE + "/" + Constants.VERSION + "/"
				+ resourcePath + "/"
				+ URLEncoder.encode(operation, Constants.ENCODING_SCHEME) + "/";
		if (hasVOMSExtension) {
			path += Constants.VOMS_EXTENSIONS + "/";
		} else {
			path += Constants.PLAIN + "/";
		}
		log.debug("Built path " + path + Constants.USER);
		return path + Constants.USER;
	}

}