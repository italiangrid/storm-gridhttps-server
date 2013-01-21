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

import it.grid.storm.gridhttps.webapp.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.webdav.authorization.WebDAVAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.webdav.factory.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlRootPage;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
	}
	
	public void init(FilterConfig fc) throws ServletException {
		Configuration.loadDefaultConfiguration();
		Configuration.initFromJSON(parse(fc.getInitParameter("params")));
		Configuration.print();
		if (!Configuration.isValid()) {
			log.error("Not a valid configuration!");
			throw new ServletException("Not a valid Configuration!");
		}
		/* Load Storage Area List */
		try {
			StorageAreaManager.init(Configuration.getBackendHostname(), Configuration.getBackendServicePort());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> parse(String jsonText) throws ServletException {
		Object decoded = JSON.parse(jsonText);
		if (decoded != null) {
			return (Map<String, String>) decoded;
		}
		throw new ServletException("Error on retrieving init parameters!");
	}
	
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpHelper.init((HttpServletRequest) request, (HttpServletResponse) response);
		UserCredentials.init(HttpHelper.getHelper());		

		HttpHelper httpHelper = HttpHelper.getHelper();
		initSession();
		
		String requestedPath = httpHelper.getRequestURI().getRawPath();
		log.debug("Requested-URI: " + requestedPath);

		if (requestedPath.contains("%20")) {
			log.error("Request URI '" + requestedPath + "' contains not allowed spaces! Exiting..");
			sendError(HttpServletResponse.SC_BAD_REQUEST, "Request URI '" + requestedPath + "' contains not allowed spaces");
		} else if (isRootPath(requestedPath)) {
			log.debug("Requested-URI is root");
			processRootRequest(httpHelper.getRequestMethod());
		} else if (isFavicon(requestedPath)) {
			log.debug("Requested-URI is favicon");
			// implement getFavicon()
		} else {
			AuthorizationFilter filter = getAuthorizationHandler(requestedPath);
			if (filter != null) {
				AuthorizationStatus status = filter.isUserAuthorized(UserCredentials.getUser());
				if (status.isAuthorized()) {
					log.debug("User is authorized to access the requested resource");
					chain.doFilter(request, response);
				} else {
					log.warn("User is not authorized to access the requested resource");
					log.warn("Reason: " + status.getReason());
					sendError(status.getErrorCode(), status.getReason());
				}
			} else {
				sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to identify the right handler to evaluate the requested path "
						+ requestedPath);
			}
		}
	}
	
	private void initSession() {
		HttpHelper.getHelper().initSession();
		HttpHelper.getHelper().getSession().setAttribute("forced", false);
		HttpHelper.getHelper().getSession().setAttribute("user", UserCredentials.getUser());
	}

	private void processRootRequest(String method) throws IOException {
		if (method.equals("OPTIONS")) {
			doPing();
			sendDavHeader();
		} else if (method.equals("GET")) {
			sendRootPage();
		}
	}

	private AuthorizationFilter getAuthorizationHandler(String path) {
		try {
			if (isFileTransferRequest(path)) {
				log.debug("Received a file-transfer request");
				return new FileTransferAuthorizationFilter(HttpHelper.getHelper(), File.separator + Configuration.getFileTransferContextPath());
			} else {
				log.debug("Received a webdav request");
				return new WebDAVAuthorizationFilter(HttpHelper.getHelper());
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		return null;
	}

	private void sendDavHeader() throws IOException {
		HttpHelper.getHelper().getResponse().addHeader("DAV", "1");
		HttpHelper.getHelper().getResponse().flushBuffer();
	}

	private boolean isRootPath(String requestedPath) {
		return (requestedPath.isEmpty() || requestedPath.equals("/"));
	}

	private boolean isFavicon(String requestedPath) {
		return requestedPath.equals("/favicon.ico");
	}

	private boolean isFileTransferRequest(String requestedURI) {
		return requestedURI.startsWith(File.separator + Configuration.getFileTransferContextPath());
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			HttpHelper.getHelper().getResponse().sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void doPing() {
		// doPing
		log.debug("ping " + Configuration.getBackendHostname() + ":" + Configuration.getBackendPort());
		try {
			PingOutputData output = StormResourceHelper.doPing(Configuration.getBackendHostname(), Configuration.getBackendPort());
			log.debug(output.getBeOs());
			log.debug(output.getBeVersion());
			log.debug(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
	}

	private void sendRootPage() throws IOException {
		HttpServletResponse response = HttpHelper.getHelper().getResponse();
		response.addHeader("Content-Type", "text/html");
		response.addHeader("DAV", "1");
		StormHtmlRootPage page = new StormHtmlRootPage(response.getOutputStream());
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		page.addNavigator("/");
		page.addStorageAreaList(getUserAuthorizedStorageAreas(UserCredentials.getUser()));
		page.end();
	}

	private List<StorageArea> getUserAuthorizedStorageAreas(UserCredentials user) {
		List<StorageArea> in = StorageAreaManager.getInstance().getStorageAreas();
		List<StorageArea> out = new ArrayList<StorageArea>();
		String reqProtocol = HttpHelper.getHelper().getRequestProtocol();
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
		if (!response &&  !user.isAnonymous()) {
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