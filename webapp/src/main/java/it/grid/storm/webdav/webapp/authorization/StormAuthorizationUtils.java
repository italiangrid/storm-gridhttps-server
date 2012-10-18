package it.grid.storm.webdav.webapp.authorization;

import it.grid.storm.webdav.webapp.authorization.methods.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationUtils {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationUtils.class);

	public static String storageAreaRootDir;
	public static String storageAreaName;
	public static String storageAreaProtocol;
	public static String stormBackendHostname;
	public static int stormBackendPort;
	public static int stormBackendServicePort;
	public static String stormFrontendHostname;
	public static int stormFrontendPort;

	public static HashMap<String, AbstractMethodAuthorization> METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();
	
	public static void doInitMethodMap(HttpServletRequest HTTPRequest) {
		METHODS_MAP.clear();
		METHODS_MAP.put("PROPFIND", new PropfindMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("OPTIONS", new OptionsMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("GET", new GetMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("DELETE", new DeleteMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("MKCOL", new MkcolMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("MOVE", new MoveMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("COPY", new CopyMethodAuthorization(HTTPRequest));
	}
	
	private static final HashMap<String, String[]> PROTOCOL_MAP = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 1L;
		{
			put("HTTP_PROTOCOL", new String[] { "HTTP" });
			put("HTTPS_PROTOCOL", new String[] { "HTTPS" });
			put("HTTP_AND_HTTPS_PROTOCOLS", new String[] { "HTTP", "HTTPS" });
		};
	};

	/* Public methods */

	public static VOMSSecurityContext getVomsSecurityContext(HttpServletRequest HTTPRequest) {
		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext vomsSecurityContext = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(vomsSecurityContext);
		X509Certificate[] certChain;
		try {
			certChain = (X509Certificate[]) HTTPRequest.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.error("Error fetching certificate from http request: " + e.getMessage());
			return vomsSecurityContext;
		}
		if (certChain != null)
			vomsSecurityContext.setClientCertChain(certChain);
		return vomsSecurityContext;
	}

	public static boolean protocolAllowed(String requestProtocol) throws Exception {
		String key = StormAuthorizationUtils.storageAreaProtocol.toUpperCase();
		if (PROTOCOL_MAP.containsKey(key)) {
			if (Arrays.asList(PROTOCOL_MAP.get(key)).contains(requestProtocol.toUpperCase()))
				return true;
			else
				return false;
		} else
			throw new Exception("protocolConfiguration '" + key + "' is not contained in  PROTOCOL_MAP");
	}

	public static boolean methodAllowed(String method) {
		boolean response = false;
		if (METHODS_MAP.containsKey(method.toUpperCase())) {
			log.debug("Method " + method.toUpperCase() + " is allowed");
			response = true;
		}
		return response;
	}

	public static String getUserDN(VOMSSecurityContext vomsSecurityContext) {
		return vomsSecurityContext.getClientDN().getX500();
	}

	public static ArrayList<String> getUserFQANs(VOMSSecurityContext vomsSecurityContext) {
		String[] userFQANs = vomsSecurityContext.getFQANs();
		ArrayList<String> fqans = new ArrayList<String>();
		for (String s : userFQANs)
			fqans.add(s);
		return fqans;
	}

	public static boolean isUserAuthorized(VOMSSecurityContext vomsSecurityContext, String operation, String path) throws Exception,
			IllegalArgumentException {

		if (path == null || operation == null || vomsSecurityContext == null) {
			log.error("Received null mandatory parameter(s) at isUserAuthorized: path=" + path + " operation=" + operation
					+ " vomsSecurityContext=" + vomsSecurityContext);
			throw new IllegalArgumentException("Received null mandatory parameter(s)");
		}

		String userDN = StormAuthorizationUtils.getUserDN(vomsSecurityContext);
		ArrayList<String> fqans = StormAuthorizationUtils.getUserFQANs(vomsSecurityContext);
		
		/********************************TEST***********************************/
		fqans.clear();
		fqans.add("/dteam/Role=NULL/Capability=NULL");
		fqans.add("/dteam/NGI_IT/Role=NULL/Capability=NULL");
		/********************************TEST***********************************/

		URI uri = StormAuthorizationUtils.prepareURI(path, operation, userDN, fqans);
		log.debug("Auth request userDN = " + userDN);
		log.debug("Auth request fqans = " + StringUtils.join(fqans, ","));
		log.debug("Auth request uri = " + uri.toString());
		
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException " + e.getLocalizedMessage());
			throw new Exception("Error contacting authorization service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException " + e.getLocalizedMessage());
			throw new Exception("Error contacting authorization service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new Exception("Unexpected error! response.getStatusLine() returned null! Please contact storm support");
		}
		int httpCode = status.getStatusCode();
		String httpMessage = status.getReasonPhrase();
		HttpEntity entity = httpResponse.getEntity();
		String output = "";
		if (entity != null) {
			InputStream responseIS;
			try {
				responseIS = entity.getContent();
			} catch (IllegalStateException e) {
				log.error("unable to get the input content stream from server answer. IllegalStateException " + e.getLocalizedMessage());
				throw new Exception("Error comunicationg with the authorization service.");
			} catch (IOException e) {
				log.error("unable to get the input content stream from server answer. IOException " + e.getLocalizedMessage());
				throw new Exception("Error comunicationg with the authorization service.");
			}
			int l;
			byte[] tmp = new byte[512];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output = output + (new String(tmp, 0, l));
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException " + e.getMessage());
				throw new Exception("Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new Exception("Unable to get a valid authorization response from the server.");
		}
		log.debug("Authorization response is : '" + output + "'");
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : '" + httpCode + "' "
					+ httpMessage);
			throw new Exception("Unable to get a valid response from server. Received a non HTTP 200 response from the server : '"
					+ httpCode + "' " + httpMessage);
		}
		Boolean response = new Boolean(output);
		log.debug("Authorization response (Boolean value): '" + response + "'");
		return response.booleanValue();
	}

	/* Private methods */

	private static URI prepareURI(String resourcePath, String operation, String userDN, ArrayList<String> fqans) throws Exception,
			IllegalArgumentException {
		if (resourcePath == null || operation == null || fqans == null) {
			log.error("Received null mandatory parameter(s) at prepareURL: resourcePath=" + resourcePath + " operation=" + operation
					+ " fqans=" + fqans.toString());
			throw new IllegalArgumentException("Received null mandatory parameter(s)");
		}
		log.debug("Encoding Authorization request parameters");
		String path;
		boolean hasSubjectDN = (userDN != null && userDN.length() > 0);
		boolean hasVOMSExtension = (fqans.size() > 0);
		try {
			path = buildpath(URLEncoder.encode(resourcePath, Constants.ENCODING_SCHEME), operation, hasSubjectDN, hasVOMSExtension);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception encoding the path \'" + resourcePath + "\' UnsupportedEncodingException: " + e.getMessage());
			throw new Exception("Unable to encode resourcePath paramether, unsupported encoding \'" + Constants.ENCODING_SCHEME + "\'");
		}
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		if (hasSubjectDN) {
			qparams.add(new BasicNameValuePair(Constants.DN_KEY, userDN));
		}
		if (hasVOMSExtension) {
			String fqansList = StringUtils.join(fqans, Constants.FQANS_SEPARATOR);
			log.debug("fqanslist = '" + fqansList + "'");
			qparams.add(new BasicNameValuePair(Constants.FQANS_KEY, fqansList));
		}
		URI uri;
		try {
			uri = new URI("http", null, stormBackendHostname, stormBackendServicePort, path, qparams.isEmpty() ? null
					: URLEncodedUtils.format(qparams, "UTF-8"), null);
		} catch (URISyntaxException e) {
			log.error("Unable to build Authorization Service URI. URISyntaxException " + e.getLocalizedMessage());
			throw new Exception("Unable to build Authorization Service URI");
		}
		log.debug("Prepared URI : " + uri);
		return uri;
	}

	private static String buildpath(String resourcePath, String operation, boolean hasSubjectDN, boolean hasVOMSExtension)
			throws UnsupportedEncodingException {
		String path = "/" + Constants.RESOURCE + "/" + Constants.VERSION + "/" + resourcePath + "/"
				+ URLEncoder.encode(operation, Constants.ENCODING_SCHEME) + "/";
		if (hasSubjectDN) {
			if (hasVOMSExtension) {
				path += Constants.VOMS_EXTENSIONS + "/";
			} else {
				path += Constants.PLAIN + "/";
			}
			path += Constants.USER;
		}

		log.debug("Built path " + path);
		return path;
	}

}