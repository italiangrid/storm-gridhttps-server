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
package it.grid.storm.gridhttps.webapp;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.webdav.authorization.WebDAVAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlRootPage;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		log.info("StormAuthorizationFilter - Init");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpHelper httpHelper = new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response);
		UserCredentials user = httpHelper.getUser();

		String requestStr = getCommand(httpHelper, user);
		log.info("Received: " + requestStr);
		
		String requestedPath = httpHelper.getRequestURI().getRawPath();
		log.debug("Requested-URI: " + requestedPath);

		if (requestedPath.contains("%20")) {
			String errorMsg =  "Request URI '" + requestedPath + "' contains not allowed spaces! ";
			log.error(errorMsg + "Exiting..");
			sendError(httpHelper.getResponse(), HttpServletResponse.SC_BAD_REQUEST, errorMsg);
		} else if (isRootPath(requestedPath)) {
			log.debug("Requested-URI is root");
			processRootRequest(httpHelper, user);
		} else if (isFavicon(requestedPath)) {
			log.debug("Requested-URI is favicon");
			// implement getFavicon()
		} else {
			AuthorizationFilter filter = getAuthorizationHandler(httpHelper, requestedPath);
			if (filter != null) {
				AuthorizationStatus status = filter.isUserAuthorized(user);
				if (status.isAuthorized()) {
					log.debug(getAuthorizedMsg(httpHelper, user));
					chain.doFilter(request, response);
				} else {
					log.warn(getUnAuthorizedMsg(httpHelper, user, status.getReason()));
					sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
				}
			} else {
				String errorMsg = "Unable to identify the right handler to evaluate the requested path " + requestedPath;
				log.error(errorMsg);
				sendError(httpHelper.getResponse(), HttpServletResponse.SC_BAD_REQUEST, errorMsg);
			}
		}
	}

	private String getAuthorizedMsg(HttpHelper httpHelper, UserCredentials user) {
		String userStr = user.getRealUserDN().isEmpty() ? "anonymous" : user.getRealUserDN();
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		return "User '" + userStr + "' is authorized to " + method + " " + path;
	}
	
	private String getUnAuthorizedMsg(HttpHelper httpHelper, UserCredentials user, String reason) {
		String userStr = user.getRealUserDN().isEmpty() ? "anonymous" : user.getRealUserDN();
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		return "User '" + userStr + "' is NOT authorized to " + method + " " + path + ": " + reason;
	}
	
	private String getCommand(HttpHelper httpHelper, UserCredentials user) {
		String fqans = user.getUserFQANSAsStr();
		String userStr = user.getRealUserDN().isEmpty() ? "anonymous" : user.getRealUserDN();
		userStr += fqans.isEmpty() ? "" : " with fqans '" + fqans + "'";
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		String destination = httpHelper.hasDestinationHeader() ? " to " + httpHelper.getDestinationURI().getPath() : "";
		String ipSender = httpHelper.getRequest().getRemoteAddr();
		return method + " " + path + destination + " from " + userStr + " ip " + ipSender;
	}
	
	private void processRootRequest(HttpHelper httpHelper, UserCredentials user) throws IOException {
		String method = httpHelper.getRequestMethod();
		if (method.equals("OPTIONS")) {
			doPing();
			sendDavHeader(httpHelper.getResponse());
		} else if (method.equals("GET")) {
			sendRootPage(httpHelper, user);
		}
	}

	private AuthorizationFilter getAuthorizationHandler(HttpHelper httpHelper, String path) {
		try {
			if (isFileTransferRequest(path)) {
				log.debug("Received a file-transfer request");
				return new FileTransferAuthorizationFilter(httpHelper, File.separator + Configuration.getGridhttpsInfo().getFiletransferContextPath());
			} else {
				log.debug("Received a webdav request");
				return new WebDAVAuthorizationFilter(httpHelper);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return null;
	}

	private void sendDavHeader(HttpServletResponse response) throws IOException {
		response.addHeader("DAV", "1");
		response.flushBuffer();
	}

	private boolean isRootPath(String requestedPath) {
		return (requestedPath.isEmpty() || requestedPath.equals("/"));
	}

	private boolean isFavicon(String requestedPath) {
		return requestedPath.equals("/favicon.ico");
	}

	private boolean isFileTransferRequest(String requestedURI) {
		return requestedURI.startsWith(File.separator + Configuration.getGridhttpsInfo().getFiletransferContextPath());
	}

	private void sendError(HttpServletResponse response, int errorCode, String errorMessage) {
		try {
			response.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void doPing() {
		String hostnameBE = Configuration.getBackendInfo().getHostname();
		int portBE = Configuration.getBackendInfo().getPort();
		log.debug("ping " + hostnameBE + ":" + portBE);
		try {
			PingOutputData output = StormResourceHelper.doPing(hostnameBE, portBE);
			log.debug(output.getBeOs());
			log.debug(output.getBeVersion());
			log.debug(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		} catch (StormRequestFailureException e) {
			log.error(e.getMessage());
		}
	}

	private void sendRootPage(HttpHelper httpHelper, UserCredentials user) throws IOException {
		httpHelper.getResponse().addHeader("Content-Type", "text/html");
		httpHelper.getResponse().addHeader("DAV", "1");
		StormHtmlRootPage page = new StormHtmlRootPage(httpHelper.getResponse().getOutputStream());
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		page.addNavigator("/");
		page.addStorageAreaList(getUserAuthorizedStorageAreas(user, httpHelper.getRequestProtocol()));
		page.end();
	}

	private List<StorageArea> getUserAuthorizedStorageAreas(UserCredentials user, String reqProtocol) {
		List<StorageArea> in = StorageAreaManager.getInstance().getStorageAreas();
		List<StorageArea> out = new ArrayList<StorageArea>();
		for (StorageArea current : in) {
			if (current.getProtocols().contains(reqProtocol)) {
				if (isUserAuthorized(user, current.getFSRoot())) {
					out.add(current);
				}
			}
		}
		return out;
	}

	private boolean isUserAuthorized(UserCredentials user, String path) {
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, Constants.PREPARE_TO_GET_OPERATION, path);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (!response && !user.isAnonymous()) {
			/* Re-try as anonymous user: */
			user.forceAnonymous();
			try {
				response = StormAuthorizationUtils.isUserAuthorized(user, Constants.PREPARE_TO_GET_OPERATION, path);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			user.unforceAnonymous();
		}
		return response;
	}
}