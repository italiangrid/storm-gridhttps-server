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
package it.grid.storm.gridhttps.webapp.webdav;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.http11.DefaultHttp11ResponseHandler.BUFFERING;
import io.milton.property.PropertySource;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.StormStandardFilter;
import it.grid.storm.gridhttps.webapp.common.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.webdav.authorization.WebDAVAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.webdav.factory.WebdavResourceFactory;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlRootPage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

public class WebDAVFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(WebDAVFilter.class);

	private ArrayList<String> rootPaths;
	private HttpManager httpManager;
	private FilterConfig filterConfig;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug(this.getClass().getSimpleName() + " - Init");
		this.filterConfig = filterConfig;
		this.rootPaths = new ArrayList<String>();
		this.rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath());
		this.rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath() + File.separator);
		
		try {
			log.debug(this.getClass().getSimpleName() + " - Init HttpManagerBuilder");
			HttpManagerBuilder builder = new HttpManagerBuilder();
			builder.setResourceFactory(new WebdavResourceFactory());
			builder.setDefaultStandardFilter(new StormStandardFilter());
			ArrayList<PropertySource> extraPropertySources = new ArrayList<PropertySource>();
			extraPropertySources.add(new StormPropertySource());
			builder.setExtraPropertySources(extraPropertySources);
			builder.setEnabledJson(false);
			builder.setBuffering(BUFFERING.never);
			builder.setEnableBasicAuth(false);
			builder.setEnableCompression(false);
			builder.setEnableExpectContinue(false);
			builder.setEnableFormAuth(false);
			builder.setEnableCookieAuth(false);
			builder.setPropertySources(new ArrayList<PropertySource>());
			StormPropFindPropertyBuilder pfBuilder = new StormPropFindPropertyBuilder();
			builder.setPropFindPropertyBuilder(pfBuilder);
			this.httpManager = builder.buildHttpManager();
			log.debug(this.getClass().getSimpleName() + " - HttpManager created!");
			pfBuilder.setPropertySources(builder.getPropertySources());
		} catch (Exception e) {
			log.error(this.getClass().getSimpleName() + " - " + e.getMessage());
			System.exit(1);
		}		
	}
		
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {
		
		HttpHelper httpHelper = null;
		try {
			httpHelper = new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response);
		} catch (Exception e) {
			log.error(e.getMessage());
			sendError((HttpServletResponse) response, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}
		
		UserCredentials user = httpHelper.getUser();
		printCommand(httpHelper, user);
		String requestedPath = httpHelper.getRequestURI().getRawPath();
		log.debug("Requested-URI: " + requestedPath);

		if (isFavicon(requestedPath)) {
			log.debug("Requested-URI is favicon");
			return;
		}
		
		AuthorizationStatus status = checkAuthorization(httpHelper, user, requestedPath);
		if (status.isAuthorized()) {
			log.debug(getAuthorizedMsg(httpHelper, user));
			if (isRootPath(requestedPath)) {
				log.debug("Requested-URI is root");
				processRootRequest(httpHelper, user);
			} else {
				doMiltonProcessing((HttpServletRequest) request, (HttpServletResponse) response);
			}
		} else {
			log.warn(getUnAuthorizedMsg(httpHelper, user, status.getReason()));
			sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
			return;
		}
	}

	private AuthorizationStatus checkAuthorization(HttpHelper httpHelper, UserCredentials user, String requestedPath) {
		
		if (requestedPath.contains("%20")) {
			log.error("Request URI '" + requestedPath + "' contains not allowed spaces!");
			return AuthorizationStatus.NOTAUTHORIZED(400, "Request URI '" + requestedPath + "' contains not allowed spaces!");
		}
		if (isRootPath(requestedPath)) {
			log.debug(this.getClass().getSimpleName() + ": is root path");
			return AuthorizationStatus.AUTHORIZED();
		} else {
			try {
				return (new WebDAVAuthorizationFilter()).isUserAuthorized(httpHelper.getRequest(), httpHelper.getResponse(), user);
			} catch (AuthorizationException e) {
				log.error(e.getMessage());
				return AuthorizationStatus.NOTAUTHORIZED(400, e.getMessage());
			}
		}
		
	}

	private String getAuthorizedMsg(HttpHelper httpHelper, UserCredentials user) {
		String userStr = user.isAnonymous() ? "anonymous" : user.getRealUserDN();
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		return "User '" + userStr + "' is authorized to " + method + " " + path;
	}

	private String getUnAuthorizedMsg(HttpHelper httpHelper, UserCredentials user, String reason) {
		String userStr = user.isAnonymous() ? "anonymous" : user.getRealUserDN();
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		return "User '" + userStr + "' is NOT authorized to " + method + " " + path + ": " + reason;
	}

	private void printCommand(HttpHelper httpHelper, UserCredentials user) {
		String fqans = user.getUserFQANSAsStr();
		String userStr = user.isAnonymous() ? "anonymous" : user.getRealUserDN();
		userStr += fqans.isEmpty() ? "" : " with fqans '" + fqans + "'";
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		String destination = httpHelper.hasDestinationHeader() ? " to " + httpHelper.getDestinationURI().getPath() : "";
		String ipSender = httpHelper.getRequest().getRemoteAddr();
		log.info("Received " + method + " " + path + destination + " from " + userStr + " ip " + ipSender);
	}

	private boolean isFavicon(String requestedPath) {
		return requestedPath.contains("favicon.ico");
	}

	private boolean isRootPath(String requestedPath) {
		return this.rootPaths.contains(requestedPath);
	}
	
	private void sendError(HttpServletResponse response, int errorCode, String errorMessage) {
		try {
			response.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void doMiltonProcessing(HttpServletRequest req,
		HttpServletResponse resp) throws IOException {

		try {
			MiltonServlet.setThreadlocals(req, resp);
			Request request = new io.milton.servlet.ServletRequest(req,
				this.filterConfig.getServletContext());
			Response response = new io.milton.servlet.ServletResponse(resp);
			httpManager.process(request, response);
		} finally {
			MiltonServlet.clearThreadlocals();
			resp.getOutputStream().flush();
			resp.flushBuffer();
		}
	}
	
	private void sendDavHeader(HttpServletResponse response) throws IOException {
		response.addHeader("DAV", "1");
		response.flushBuffer();
	}
	
	private void processRootRequest(HttpHelper httpHelper, UserCredentials user) throws IOException {
		String method = httpHelper.getRequestMethod();
		if (method.toUpperCase().equals("OPTIONS")) {
			doPing(user);
			sendDavHeader(httpHelper.getResponse());
		} else if (method.toUpperCase().equals("GET")) {
			sendRootPage(httpHelper, user);
		}
	}
	
	private void doPing(UserCredentials user) {
		try {
			StormResourceHelper.getInstance().doPing(user, Configuration.getBackendInfo().getHostname(), Configuration.getBackendInfo().getPort());
		} catch (SRMOperationException e) {
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
		page.addStorageAreaList(StorageAreaManager.getInstance().getUserAuthorizedStorageAreas(user, httpHelper.getRequestProtocol()));
		page.end();
	}
	
	@Override
	public void destroy() {
	}
}