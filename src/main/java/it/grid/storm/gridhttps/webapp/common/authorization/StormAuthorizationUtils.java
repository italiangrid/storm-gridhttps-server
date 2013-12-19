/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.common.authorization;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.StormHTTPClient;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationUtils {

  private static final Logger log = LoggerFactory
    .getLogger(StormAuthorizationUtils.class);

  private static final boolean AUTHZ_CALL_ENABLED = Configuration
    .getGridhttpsInfo().isAuthzCallEnabled();

  public static boolean isUserAuthorized(UserCredentials user,
    String operation, String path) {

    if (path == null)
      throw new IllegalArgumentException(
        "Received null path at isUserAuthorized!");

    if (operation == null)
      throw new IllegalArgumentException(
        "Received null operation at isUserAuthorized!");

    if (user == null)
      throw new IllegalArgumentException(
        "Received null user at isUserAuthorized!");

    if (AUTHZ_CALL_ENABLED) {
      log.debug("Asking authorization for operation " + operation + " on "
        + path);
      return getAuthorizationResponse(prepareURI(path, operation, user));
    }

    return true;
  }

  private static boolean getAuthorizationResponse(URI uri) {

    final HttpGet authzCall = new HttpGet(uri);
    final HttpClient httpclient = StormHTTPClient.INSTANCE.getHTTPClient();

    try {

      final HttpResponse httpResponse = httpclient.execute(authzCall);
      final StatusLine status = httpResponse.getStatusLine();

      final int statusCode = status.getStatusCode();
      final String reasonPhrase = status.getReasonPhrase();
      final HttpEntity entity = httpResponse.getEntity();

      if (statusCode != HttpStatus.SC_OK) {
        String errorMsg = String.format(
          "Error contacting authz endpoint: %d %s", statusCode, reasonPhrase);
        log.error(errorMsg);
        throw new AuthorizationException(errorMsg);
      }

      if (entity == null) {
        log.error("Autorization endpoint returned an empty response:{}",
          httpResponse);
        throw new AuthorizationException(
          "Authorization endpoint returned an empty response.");
      }

      String responseContent = EntityUtils.toString(entity);
      log.debug("Authorization response: {}", responseContent);
      return Boolean.parseBoolean(responseContent);

    } catch (Throwable t) {
      authzCall.abort();
      log.error("Error executing authz callout: {}", t.getMessage(), t);
      throw new AuthorizationException(t);
    }
  }

  private static URI prepareURI(String resourcePath, String operation,
    UserCredentials user) {

    log.debug("Encoding Authorization request parameters");
    String path;
    boolean hasSubjectDN = (!user.isAnonymous());
    boolean hasVOMSExtension = (!user.isAnonymous())
      && (!user.getUserFQANS().isEmpty());
    try {
      path = buildpath(
        URLEncoder.encode(resourcePath, Constants.ENCODING_SCHEME), operation,
        hasSubjectDN, hasVOMSExtension);
    } catch (UnsupportedEncodingException e) {
      log.error("Exception encoding the path \'" + resourcePath
        + "\' UnsupportedEncodingException: " + e.getMessage());
      throw new AuthorizationException(
        "Unable to encode resourcePath paramether, unsupported encoding \'"
          + Constants.ENCODING_SCHEME + "\'");
    }
    List<NameValuePair> qparams = new ArrayList<NameValuePair>();
    if (hasSubjectDN) {
      qparams.add(new BasicNameValuePair(Constants.DN_KEY, user.getUserDN()));
    }
    if (hasVOMSExtension) {
      String fqansList = StringUtils.join(user.getUserFQANS(),
        Constants.FQANS_SEPARATOR);
      qparams.add(new BasicNameValuePair(Constants.FQANS_KEY, fqansList));
    }
    URI uri;
    try {
      uri = new URI("http", null, Configuration.getBackendInfo().getHostname(),
        Configuration.getBackendInfo().getServicePort(), path,
        qparams.isEmpty() ? null : URLEncodedUtils.format(qparams, "UTF-8"),
        null);
    } catch (URISyntaxException e) {
      log
        .error("Unable to build Authorization Service URI. URISyntaxException "
          + e.getLocalizedMessage());
      throw new AuthorizationException("Unable to build Authorization Service URI");
    }
    log.debug("Prepared URI : " + uri);
    return uri;
  }

  private static String buildpath(String resourcePath, String operation,
    boolean hasSubjectDN, boolean hasVOMSExtension)
    throws UnsupportedEncodingException {

    String path = "/" + Constants.RESOURCE + "/" + Constants.VERSION + "/"
      + resourcePath + "/"
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
