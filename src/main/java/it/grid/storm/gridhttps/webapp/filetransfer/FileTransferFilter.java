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
import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.StormStandardFilter;
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants.DavMethod;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.utils.StormPartialGetHelper;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.FileTransferException;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.InvalidTURLException;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.FileTransferMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.HeadMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.PutMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.factory.FileSystemResourceFactory;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileTransferFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferFilter.class);
	
	private FilterConfig filterConfig;
	private HttpManager httpManager;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		this.filterConfig = filterConfig;
		
		try {
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
			builder.setPartialGetHelper(new StormPartialGetHelper());
			builder.setPropertySources(new ArrayList<PropertySource>());
			this.httpManager = builder.buildHttpManager();
			log.debug("HttpManager succesfully created!");
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			System.exit(1);
		}		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {

		HttpHelper httpHelper = new HttpHelper((HttpServletRequest) request,
			(HttpServletResponse) response);
		UserCredentials user = null;
		FileTransferMethodAuthorization handler = null;
		
		try {
			/* get user */
			user = getUser(httpHelper);
			log.debug("User: {}", user);
			httpHelper.setUser(user);
			
			/* print received command */
			printInCommand(httpHelper, user);
			
			/* check if TURL is malformed */
			String uriPath = httpHelper.getRequestURI().getRawPath();
			checkRequestedTURL(uriPath);
			
			/* check if user is authorized */
			handler = getAuthorizationMethodHandler(httpHelper.getRequestMethod());
			log.debug("AuthorizationMethodHandler is {}", handler.getClass().getName());
			AuthorizationStatus status = handler.isUserAuthorized(
				httpHelper.getRequest(), httpHelper.getResponse(), user);

			if (status.isAuthorized()) {
				log.debug("Processing a FileTransfer request for {}", uriPath);
				doMiltonProcessing((HttpServletRequest) request, (HttpServletResponse) response);
			} else {
				log.debug("User is not authorized: {}", status.getReason());
				sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
			}
			
		} catch (FileTransferException e) {
			log.error(e.getMessage());
			sendError(httpHelper.getResponse(), e.getErrorcode(), e.getMessage());
		
		} catch (InvalidTURLException e) {
			log.error(e.getMessage());
			sendError(httpHelper.getResponse(), e.getErrorcode(), e.getMessage());
		
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			sendError(httpHelper.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		
		} finally {
			printOutCommand(httpHelper, user);
		}
	}
	
	private void checkRequestedTURL(String uriPath) {

		String ftContextPath = File.separator + 
			Configuration.getGridhttpsInfo().getFiletransferContextPath();
		
		if (!uriPath.startsWith(ftContextPath)) {
			throw new InvalidTURLException(HttpServletResponse.SC_CONFLICT, 
				String.format("TURL '%s' doesn't start with %s!", uriPath, ftContextPath));
		}
		log.debug("TURL {} starts with {}", uriPath, ftContextPath);
		
		if (uriPath.contains("..")) {
			throw new InvalidTURLException(HttpServletResponse.SC_CONFLICT, 
				String.format("TURL '%s' contains a dotted segment!", uriPath));
		}
		log.debug("TURL {} doesn't contain dotted segments", uriPath);
}

	private UserCredentials getUser(HttpHelper httpHelper) throws FileTransferException {

		if (httpHelper.isHttp()) {
			return new UserCredentials();
		}
		X509Certificate[] certChain = httpHelper.getX509Certificate();
		if (certChain == null) {
			throw new FileTransferException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Unable to get certificate chain from request header");
		}
		httpHelper.getVOMSSecurityContext().setClientCertChain(certChain);
		String dn = httpHelper.getVOMSSecurityContext().getClientName();
		if (dn == null) {
			throw new FileTransferException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Unable to get user DN from VOMS security context!");
		}
		ArrayList<String> fqans = new ArrayList<String>();
		for (VOMSAttribute voms : httpHelper.getVOMSSecurityContext()
			.getVOMSAttributes())
			fqans.addAll(voms.getFQANs());
		return new UserCredentials(dn, fqans);
	}
	
	private String buildLogMessage(HttpHelper httpHelper, UserCredentials user) {
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		String destination = httpHelper.hasDestinationHeader() ? " to " + httpHelper.getDestinationURI().getPath() : "";
		String ipSender = httpHelper.getRequest().getRemoteAddr();
		return String.format("%s %s%s from %s ip %s", method, path, destination, user.getFullName(), ipSender);
	}
	
	private void printInCommand(HttpHelper httpHelper, UserCredentials user) {
		log.info("Received {}", buildLogMessage(httpHelper, user));
	}

	private void printOutCommand(HttpHelper httpHelper, UserCredentials user) {
		int code = httpHelper.getResponse().getStatus();
    String text = (String) httpHelper.getRequest().getAttribute("STATUS_MSG");
    String msg = String.format("%s exited with %s%s", buildLogMessage(httpHelper, user), code, (text != null ? " " + text : ""));
    if (code >= 400){
        log.warn(msg);
    }else if (code >= 500) {
        log.error(msg);
    } else {
        log.info(msg);
    }
	}

	private void sendError(HttpServletResponse response, int errorCode, String errorMessage) {
		try {
			response.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
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

	private FileTransferMethodAuthorization getAuthorizationMethodHandler(
		String method) throws FileTransferException {

		DavMethod davMethod = DavMethod.valueOf(method.toUpperCase());
		if (davMethod == null) {
			throw new FileTransferException(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Invalid method " + method +"!");
		}
		switch (davMethod) {
		case GET:
			return new GetMethodAuthorization();
		case PUT:
			return new PutMethodAuthorization();
		case HEAD:
			return new HeadMethodAuthorization();
		default:
			throw new FileTransferException(
				HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"Method " + davMethod +" is not supported!");
		}
	}
	
	@Override
	public void destroy() {
	}
}
