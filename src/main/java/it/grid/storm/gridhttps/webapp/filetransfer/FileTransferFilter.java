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
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants.DavMethod;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.InternalError;
import it.grid.storm.gridhttps.webapp.common.exceptions.InvalidRequestException;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.FileTransferMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.HeadMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.authorization.methods.PutMethodAuthorization;
import it.grid.storm.gridhttps.webapp.filetransfer.factory.FileSystemResourceFactory;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.EnumSet;

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
	
	private final static EnumSet<DavMethod> FTStoRMSupportedMethod = EnumSet.range(DavMethod.HEAD, DavMethod.PUT);
	
	private FilterConfig filterConfig;
	private HttpManager httpManager;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug("{} - Init" , this.getClass().getSimpleName());
		this.filterConfig = filterConfig;
		
		try {
			log.debug("{} - Init HttpManagerBuilder" , this.getClass().getSimpleName());
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
			log.debug("{} - HttpManager created!" , this.getClass().getSimpleName());
			
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			System.exit(1);
		}		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {

		HttpHelper httpHelper = null;
		UserCredentials user = null;
		
		try {
			httpHelper = new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response);
			user = getUser(httpHelper);
			log.debug("User: {}", user);
			httpHelper.setUser(user);
			printInCommand(httpHelper, user);
			checkIfRequestIsValid(httpHelper);
		
			FileTransferMethodAuthorization handler = getAuthorizationMethodHandler(Constants.DavMethod.valueOf(httpHelper.getRequestMethod()));
			AuthorizationStatus	status = handler.isUserAuthorized(httpHelper.getRequest(), httpHelper.getResponse(), user);
			
			if (status.isAuthorized()) {
				log.debug("Processing a FileTransfer request for {}", httpHelper.getRequestURI().getRawPath());
				doMiltonProcessing((HttpServletRequest) request, (HttpServletResponse) response);
			} else {
				log.debug("User is not authorized: {}", status.getReason());
				sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
			}
			
		} catch (InvalidRequestException e) {
			log.debug(e.getMessage());
			sendError((HttpServletResponse) response, e.getErrorcode(), e.getMessage());
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			sendError(httpHelper.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
		} finally {
			printOutCommand(httpHelper, user);
		}
	}
	
	private UserCredentials getUser(HttpHelper httpHelper) throws InternalError {

		if (httpHelper.isHttp()) {
			return new UserCredentials();
		}
		X509Certificate[] certChain = httpHelper.getX509Certificate();
		if (certChain == null) {
			log.warn("Unable to get certificate chain from request header");
			throw new InternalError("Unable to get certificate chain from request header");
		}
		httpHelper.getVOMSSecurityContext().setClientCertChain(certChain);
		String dn = httpHelper.getVOMSSecurityContext().getClientName();
		if (dn == null) {
			log.warn("Unable to get user DN from VOMS security context!");
			throw new InternalError("Unable to get user DN from VOMS security context!");
		}
		ArrayList<String> fqans = new ArrayList<String>();
		for (VOMSAttribute voms : httpHelper.getVOMSSecurityContext()
			.getVOMSAttributes())
			fqans.addAll(voms.getFQANs());
		return new UserCredentials(dn, fqans);
	}
	
	private void checkIfRequestIsValid(HttpHelper httpHelper) throws InvalidRequestException {
		
		/* check method */
		DavMethod method = DavMethod.valueOf(httpHelper.getRequestMethod().toUpperCase());
		if (method == null) {
			throw new InvalidRequestException(
				HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				String.format("Method %s is not supported by this StoRM instance!"));
		}
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
		DavMethod method) throws AuthorizationException {

		if (!FTStoRMSupportedMethod.contains(method)) {
			throw new AuthorizationException("Invalid method!");
		}
		switch (method) {
		case GET:
			return new GetMethodAuthorization();
		case PUT:
			return new PutMethodAuthorization();
		case HEAD:
			return new HeadMethodAuthorization();
		default:
			break;
		}
		return null;
	}
	
	@Override
	public void destroy() {
	}
}
