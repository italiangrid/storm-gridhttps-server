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
package it.grid.storm.gridhttps.webapp.filetransfer;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.http11.DefaultHttp11ResponseHandler.BUFFERING;
import io.milton.property.PropertySource;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.StormStandardFilter;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationException;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationFilter;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.gridhttps.webapp.filetransfer.factory.FileSystemResourceFactory;

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

public class FileTransferFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferFilter.class);
	
	private FilterConfig filterConfig;
	private HttpManager httpManager;
	private AuthorizationFilter authFilter = new FileTransferAuthorizationFilter();
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug(this.getClass().getSimpleName() + " - Init");
		this.filterConfig = filterConfig;
		
		try {
			log.debug(this.getClass().getSimpleName() + " - Init HttpManagerBuilder");
			HttpManagerBuilder builder = new HttpManagerBuilder();
			builder.setResourceFactory(new FileSystemResourceFactory());
			builder.setDefaultStandardFilter(new StormStandardFilter());
			builder.setEnabledJson(false);
			builder.setBuffering(BUFFERING.never);
			builder.setEnableBasicAuth(false);
			builder.setEnableCompression(false);
			builder.setEnableExpectContinue(false);
			builder.setEnableFormAuth(false);
			builder.setEnableCookieAuth(false);
			builder.setPropertySources(new ArrayList<PropertySource>());
			this.httpManager = builder.buildHttpManager();
			log.debug(this.getClass().getSimpleName() + " - HttpManager created!");
			
		} catch (Exception e) {
			log.error(e.getMessage());
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
				
		AuthorizationStatus status = null;
		try {
			status = this.authFilter.isUserAuthorized((HttpServletRequest) request, (HttpServletResponse) response, user);
		} catch (AuthorizationException e) {
			log.error(e.getMessage());
			status = AuthorizationStatus.NOTAUTHORIZED(400, e.getMessage());
		}
		if (status.isAuthorized()) {
			log.debug(getAuthorizedMsg(httpHelper, user));
			doMiltonProcessing((HttpServletRequest) request, (HttpServletResponse) response);
		} else {
			log.warn(getUnAuthorizedMsg(httpHelper, user, status.getReason()));
			sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
			return;
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

	private void printCommand(HttpHelper httpHelper, UserCredentials user) {
		String fqans = user.getUserFQANSAsStr();
		String userStr = user.getRealUserDN().isEmpty() ? "anonymous" : user.getRealUserDN();
		userStr += fqans.isEmpty() ? "" : " with fqans '" + fqans + "'";
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		String destination = httpHelper.hasDestinationHeader() ? " to " + httpHelper.getDestinationURI().getPath() : "";
		String ipSender = httpHelper.getRequest().getRemoteAddr();
		log.info(method + " " + path + destination + " from " + userStr + " ip " + ipSender);
	}

	private void sendError(HttpServletResponse response, int errorCode, String errorMessage) {
		try {
			response.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	private void doMiltonProcessing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			MiltonServlet.setThreadlocals(req, resp);
			Request request = new io.milton.servlet.ServletRequest(req, this.filterConfig.getServletContext());
			Response response = new io.milton.servlet.ServletResponse(resp);
			httpManager.process(request, response);
		} finally {
			MiltonServlet.clearThreadlocals();
			resp.getOutputStream().flush();
			resp.flushBuffer();
		}
	}

	@Override
	public void destroy() {
	}
	
	
	
}
