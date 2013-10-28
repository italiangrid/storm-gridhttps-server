/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.common.authorization;

import it.grid.storm.gridhttps.configuration.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationUtils {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationUtils.class);

	/* Public methods */

	public static boolean isUserAuthorized(UserCredentials user, String operation, String path) throws Exception, IllegalArgumentException {
		if (path == null)
			throw new IllegalArgumentException("Received null path at isUserAuthorized!");
		if (operation == null)
			throw new IllegalArgumentException("Received null operation at isUserAuthorized!");
		if (user == null)
			throw new IllegalArgumentException("Received null user at isUserAuthorized!");
		
		log.debug("Asking authorization for operation " + operation + " on " + path);
		return getAuthorizationResponse(prepareURI(path, operation, user));
	}

	/* Private methods */

	private static boolean getAuthorizationResponse(URI uri) throws Exception {
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

	private static URI prepareURI(String resourcePath, String operation, UserCredentials user) throws Exception {
		log.debug("Encoding Authorization request parameters");
		String path;
		boolean hasSubjectDN = (!user.isAnonymous());
		boolean hasVOMSExtension = (!user.isAnonymous()) && (!user.getUserFQANS().isEmpty());
		try {
			path = buildpath(URLEncoder.encode(resourcePath, Constants.ENCODING_SCHEME), operation, hasSubjectDN, hasVOMSExtension);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception encoding the path \'" + resourcePath + "\' UnsupportedEncodingException: " + e.getMessage());
			throw new Exception("Unable to encode resourcePath paramether, unsupported encoding \'" + Constants.ENCODING_SCHEME + "\'");
		}
		List<NameValuePair> qparams = new ArrayList<NameValuePair>();
		if (hasSubjectDN) {
			qparams.add(new BasicNameValuePair(Constants.DN_KEY, user.getUserDN()));
		}
		if (hasVOMSExtension) {
			String fqansList = StringUtils.join(user.getUserFQANS(), Constants.FQANS_SEPARATOR);
			qparams.add(new BasicNameValuePair(Constants.FQANS_KEY, fqansList));
		}
		URI uri;
		try {
			uri = new URI("http", null, Configuration.getBackendInfo().getHostname(), Configuration.getBackendInfo().getServicePort(), path,
					qparams.isEmpty() ? null : URLEncodedUtils.format(qparams, "UTF-8"), null);
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