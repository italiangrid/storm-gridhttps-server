package it.grid.storm.ftwebapp.authorization;

import it.grid.storm.ftwebapp.Configuration;
import it.grid.storm.ftwebapp.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.ftwebapp.authorization.methods.GetMethodAuthorization;
import it.grid.storm.ftwebapp.authorization.methods.PutMethodAuthorization;
import it.grid.storm.ftwebapp.factory.HTTPHelper;

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
import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationUtils {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationUtils.class);

	private static HashMap<String, AbstractMethodAuthorization> METHODS_MAP = new HashMap<String, AbstractMethodAuthorization>();

	private static void doInitMethodMap(HttpServletRequest HTTPRequest) {
		METHODS_MAP.clear();
		METHODS_MAP.put("GET", new GetMethodAuthorization(HTTPRequest));
		METHODS_MAP.put("PUT", new PutMethodAuthorization(HTTPRequest));
	}

	private static final HashMap<String, String[]> PROTOCOL_MAP = new HashMap<String, String[]>() {
		private static final long serialVersionUID = 1L;
		{
			put("HTTP_PROTOCOL", new String[] { "HTTP" });
			put("HTTPS_PROTOCOL", new String[] { "HTTPS" });
			put("HTTP_AND_HTTPS_PROTOCOLS", new String[] { "HTTP", "HTTPS" });
		};
	};

	private static final ArrayList<String> METHOD_LIST = new ArrayList<String>() {
		private static final long serialVersionUID = 1L;
		{
			add("GET");
			add("PUT");
		};
	};

	/* Public methods */

	public static AbstractMethodAuthorization getAuthorizationHandler(HttpServletRequest HTTPRequest) {
		doInitMethodMap(HTTPRequest);
		return METHODS_MAP.get(HTTPRequest.getMethod().toUpperCase());
	}

	public static VOMSSecurityContext getVomsSecurityContext(HttpServletRequest HTTPRequest) {
		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext sc = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(sc);
		X509Certificate[] certChain = HTTPHelper.getX509Certificate();
		if (certChain != null)
			sc.setClientCertChain(certChain);
		return sc;
	}

	public static boolean protocolAllowed(String requestProtocol) throws Exception {
		String key = Configuration.storageAreaProtocol.toUpperCase();
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
		if (METHOD_LIST.contains(method.toUpperCase())) {
			log.debug("Method " + method.toUpperCase() + " is allowed");
			response = true;
		}
		return response;
	}

	public static String getUserDN(VOMSSecurityContext vomsSecurityContext) {
		return vomsSecurityContext.getClientDN() != null ? vomsSecurityContext.getClientDN().getX500() : "";
	}

	public static String getUserDN(HttpServletRequest HTTPRequest) {
		return getUserDN(getVomsSecurityContext(HTTPRequest));
	}

	public static String getUserDN() {
		return getUserDN(HTTPHelper.getRequest());
	}

	public static ArrayList<String> getUserFQANs(VOMSSecurityContext vomsSecurityContext) {
		ArrayList<String> fqansStr = new ArrayList<String>();
		if (vomsSecurityContext.isEmpty())
			return fqansStr;
		List<VOMSAttribute> vomsAttrs = vomsSecurityContext.getVOMSAttributes();
		for (VOMSAttribute voms : vomsAttrs)
			for (String s : voms.getFQANs()) {
		    	fqansStr.add(s);
		    	log.debug("fqan: " + s);
		    }
		/******************************** TEST ***********************************/
//		fqansStr.clear();
//		fqansStr.add("/dteam/Role=NULL/Capability=NULL");
//		fqansStr.add("/dteam/NGI_IT/Role=NULL/Capability=NULL");
		/******************************** TEST ***********************************/
		return fqansStr;
	}

	public static ArrayList<String> getUserFQANs(HttpServletRequest HTTPRequest) {
		return getUserFQANs(getVomsSecurityContext(HTTPRequest));
	}

	public static ArrayList<String> getUserFQANs() {
		return getUserFQANs(HTTPHelper.getRequest());
	}

	public static boolean isUserAuthorized(String operation, String path) throws Exception, IllegalArgumentException {
		String userDN = StormAuthorizationUtils.getUserDN();
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs();
		return isUserAuthorized(userDN, userFQANs, operation, path);
	}

	private static boolean isUserAuthorized(String userDN, ArrayList<String> userFQANs, String operation, String path) throws Exception,
			IllegalArgumentException {
		if (path == null || operation == null || userFQANs == null || userDN == null) {
			String errorMsg = "Received null mandatory parameter(s) at isUserAuthorized: ";
			errorMsg += "path=" + path + " operation=" + operation;
			errorMsg += " userDN=" + userDN + " FQANs=" + StringUtils.join(userFQANs, ",");
			log.error(errorMsg);
			throw new IllegalArgumentException("Received null mandatory parameter(s)");
		}
		
		URI uri = StormAuthorizationUtils.prepareURI(path, operation, userDN, userFQANs);

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
			qparams.add(new BasicNameValuePair(Constants.FQANS_KEY, fqansList));
		}
		URI uri;
		try {
			uri = new URI("http", null, Configuration.stormBackendHostname, Configuration.stormBackendServicePort, path, qparams.isEmpty() ? null
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