package it.grid.storm.webdav;

import it.grid.storm.gridhttps.remotecall.UserAuthzServiceConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationUtils {

	private static final Logger log = LoggerFactory
			.getLogger(StormAuthorizationUtils.class);

	private static final String READ_METHOD = "GET";
	private static final String WRITE_METHOD = "PUT";
	private static String ENCODED_READ_OPERATION;
	private static String ENCODED_WRITE_OPERATION;

	private String stormBackendHostname;
	private int stormBackendRestPort;

	public StormAuthorizationUtils(String stormBackendHostname,
			int stormBackendRestPort) throws Exception {
		initOperationNames();
		this.stormBackendHostname = stormBackendHostname;
		this.stormBackendRestPort = stormBackendRestPort;
	}

	private void initOperationNames() throws Exception {
		if (ENCODED_READ_OPERATION == null)
			try {
				ENCODED_READ_OPERATION = URLEncoder.encode(
						UserAuthzServiceConstants.READ_OPERATION,
						UserAuthzServiceConstants.ENCODING_SCHEME);
				ENCODED_WRITE_OPERATION = URLEncoder.encode(
						UserAuthzServiceConstants.WRITE_OPERATION,
						UserAuthzServiceConstants.ENCODING_SCHEME);
			} catch (UnsupportedEncodingException e) {
				log.error("ERROR: unable to encode operations! UnsupportedEncodingException "
						+ e.getMessage());
				throw new Exception(
						"Internal error! Unable to encode authorization server operation!");
			}
	}

	public boolean isUserAuthorized(String resourcePath, String method,
			String subjectDN, String[] fqans) throws Exception {
		if (resourcePath == null || method == null || subjectDN == null
				|| fqans == null) {
			log.error("Received null parameter(s) at isUserAuthorized: resourcePath="
					+ resourcePath
					+ " method="
					+ method
					+ " subjectDN="
					+ subjectDN + " fqans=" + fqans);
			throw new IllegalArgumentException("Received null parameter(s)");
		}
		URI uri = prepareURI(resourcePath, method, subjectDN, fqans);
		log.debug("Authorization request uri = " + uri.toString());
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException "
					+ e.getLocalizedMessage());
			throw new Exception("Error contacting authorization service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException "
					+ e.getLocalizedMessage());
			throw new Exception("Error contacting authorization service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new Exception(
					"Unexpected error! response.getStatusLine() returned null! Please contact storm support");
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
				log.error("unable to get the input content stream from server answer. IllegalStateException "
						+ e.getLocalizedMessage());
				throw new Exception(
						"Error comunicationg with the authorization service.");
			} catch (IOException e) {
				log.error("unable to get the input content stream from server answer. IOException "
						+ e.getLocalizedMessage());
				throw new Exception(
						"Error comunicationg with the authorization service.");
			}
			int l;
			byte[] tmp = new byte[512];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output += new String(tmp, 0, l);
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException "
						+ e.getMessage());
				throw new Exception(
						"Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new Exception(
					"Unable to get a valid authorization response from the server.");
		}
		log.debug("Authorization response response is : \'" + output + "\'");
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : \'"
					+ httpCode + "\' " + httpMessage);
			throw new Exception("Unable to get a valid response from server. "
					+ httpMessage);
		}
		Boolean response = new Boolean(output);
		log.debug("Authorization response (Boolean value): \'" + response
				+ "\'");
		return response.booleanValue();
	}

	private URI prepareURI(String resourcePath, String method,
			String subjectDN, String[] fqans) throws Exception {
		if (resourcePath == null || method == null || subjectDN == null
				|| fqans == null) {
			log.error("Received null parameter(s) at prepareURL: resourcePath="
					+ resourcePath + " method=" + method + " subjectDN="
					+ subjectDN + " fqans=" + fqans);
			throw new IllegalArgumentException("Received null parameter(s)");
		}
		log.debug("Encoding Authorization request parameters");
		String path;
		try {
			path = buildpath(URLEncoder.encode(resourcePath,
					UserAuthzServiceConstants.ENCODING_SCHEME), method,
					fqans.length > 0);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception encoding the path \'" + resourcePath
					+ "\' UnsupportedEncodingException: " + e.getMessage());
			throw new Exception(
					"Unable to encode resourcePath paramether, unsupported encoding \'"
							+ UserAuthzServiceConstants.ENCODING_SCHEME + "\'");
		}
		String fqansList = null;
		if (fqans.length > 0) {
			fqansList = "";
			for (int i = 0; i < fqans.length; i++) {
				if (i > 0) {
					fqansList += UserAuthzServiceConstants.FQANS_SEPARATOR;
				}
				fqansList += fqans[i];
			}
		}
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		qparams.add(new BasicNameValuePair(UserAuthzServiceConstants.DN_KEY,
				subjectDN));
		if (fqansList != null) {
			qparams.add(new BasicNameValuePair(
					UserAuthzServiceConstants.FQANS_KEY, fqansList));
		}
		URI uri;
		try {

			uri = new URI("http", null, stormBackendHostname,
					stormBackendRestPort, path, URLEncodedUtils.format(qparams,
							"UTF-8"), null);

		} catch (URISyntaxException e) {
			log.error("Unable to build Authorization Service RUI. URISyntaxException "
					+ e.getLocalizedMessage());
			throw new Exception("Unable to build Authorization Service RUI");
		}
		log.debug("Prepared URI : " + uri);
		return uri;
	}

	private String buildpath(String resourcePath, String method,
			boolean hasVOMSExtension) {
		String operation;
		if (method.equals(READ_METHOD)) {
			operation = ENCODED_READ_OPERATION;
		} else {
			operation = ENCODED_WRITE_OPERATION;
		}
		String path = "/" + UserAuthzServiceConstants.RESOURCE + "/"
				+ UserAuthzServiceConstants.VERSION + "/" + resourcePath + "/"
				+ operation + "/";
		if (hasVOMSExtension) {
			path += UserAuthzServiceConstants.VOMS_EXTENSIONS + "/";
		} else {
			path += UserAuthzServiceConstants.PLAIN + "/";
		}
		log.debug("Built path " + path + UserAuthzServiceConstants.USER);
		return path + UserAuthzServiceConstants.USER;
	}
	
}