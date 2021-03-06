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
import it.grid.storm.gridhttps.webapp.common.authorization.AuthorizationStatus;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants.DavMethod;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.CopyMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.DeleteMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.GetMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.HeadMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.MkcolMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.MoveMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.OptionsMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.PropfindMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.PutMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.authorization.methods.WebDAVMethodAuthorization;
import it.grid.storm.gridhttps.webapp.webdav.factory.WebdavResourceFactory;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlRootPage;

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

public class WebDAVFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(WebDAVFilter.class);
	
	private ArrayList<String> rootPaths;
	private HttpManager httpManager;
	private FilterConfig filterConfig;
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug("{} - Init" , this.getClass().getSimpleName());
		this.filterConfig = filterConfig;
		this.rootPaths = new ArrayList<String>();
		this.rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath());
		this.rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath() + File.separator);
		
		try {
			log.debug("{} - Init HttpManagerBuilder" , this.getClass().getSimpleName());
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
			log.debug("{} - HttpManager created!", this.getClass().getSimpleName());
			pfBuilder.setPropertySources(builder.getPropertySources());
		} catch (Exception e) {
			log.error("{} - {}" , this.getClass().getSimpleName() , e.getMessage(),e);
			System.exit(1);
		}		
	}
		
	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {
		
		HttpHelper httpHelper = new HttpHelper((HttpServletRequest) request, 
			(HttpServletResponse) response);
		UserCredentials user = null;
		WebDAVMethodAuthorization handler = null;
		
		try {
			user = getUser(httpHelper);
			log.debug("User: {}", user);
			httpHelper.setUser(user);
			printInCommand(httpHelper, user);
			checkURL(httpHelper);
			
			AuthorizationStatus status = null;
			if (isRootPath(httpHelper.getRequestURI().getRawPath())) {
				status = AuthorizationStatus.AUTHORIZED();
			} else {
				handler = getAuthorizationMethodHandler(httpHelper.getRequestMethod());
				status = handler.isUserAuthorized(httpHelper.getRequest(), httpHelper.getResponse(), user);
			}
			if (status.isAuthorized()) {
				log.debug("User is authorized");
				if (isRootPath(httpHelper.getRequestURI().getRawPath())) {
					log.debug("Processing root page request");
					processRootRequest(httpHelper, user);
				} else {
					log.debug("Processing a WebDAV request for {}", httpHelper.getRequestURI().getRawPath());
					doMiltonProcessing((HttpServletRequest) request, (HttpServletResponse) response);
				}
			} else {
				log.debug("User is not authorized: {}", status.getReason());
				sendError(httpHelper.getResponse(), status.getErrorCode(), status.getReason());
			}
		} catch (WebDAVFilterException e) {
			log.error(e.getMessage());
			sendError(httpHelper.getResponse(), e.getErrorcode(), e.getMessage());
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			sendError(httpHelper.getResponse(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal Server Error: " + e.getMessage());
		} finally {
			printOutCommand(httpHelper, user);
		}
	}

	private UserCredentials getUser(HttpHelper httpHelper) 
		throws WebDAVFilterException {

		if (httpHelper.isHttp()) {
			return new UserCredentials();
		}
		X509Certificate[] certChain = httpHelper.getX509Certificate();
		if (certChain == null) {
			throw new WebDAVFilterException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Unable to get certificate chain from request header");
		}
		httpHelper.getVOMSSecurityContext().setClientCertChain(certChain);
		String dn = httpHelper.getVOMSSecurityContext().getClientName();
		if (dn == null) {
			throw new WebDAVFilterException(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Unable to get user DN from VOMS security context!");
		}
		ArrayList<String> fqans = new ArrayList<String>();
		for (VOMSAttribute voms : httpHelper.getVOMSSecurityContext()
			.getVOMSAttributes())
			fqans.addAll(voms.getFQANs());
		return new UserCredentials(dn, fqans);
	}
	

	private WebDAVMethodAuthorization getAuthorizationMethodHandler(String method) {
		
		DavMethod davMethod = DavMethod.valueOf(method.toUpperCase());
		if (davMethod == null) {
			throw new WebDAVFilterException(
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Invalid method " + method +"!");
		}
		switch (davMethod) {
		case PROPFIND:
			return new PropfindMethodAuthorization();
		case OPTIONS:
			return new OptionsMethodAuthorization();
		case GET:
			return new GetMethodAuthorization();
		case DELETE:
			return new DeleteMethodAuthorization();
		case PUT:
			return new PutMethodAuthorization();
		case MKCOL:
			return new MkcolMethodAuthorization();
		case MOVE:
			return new MoveMethodAuthorization();
		case COPY:
			return new CopyMethodAuthorization();
		case HEAD:
			return new HeadMethodAuthorization();
		default:
			throw new WebDAVFilterException(
				HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"Method " + davMethod +" is not supported!");
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
			sendDavHeader(httpHelper.getResponse());
		} else if (method.toUpperCase().equals("GET")) {
			sendRootPage(httpHelper, user);
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

	private void checkURL(HttpHelper httpHelper) {

		/* check requested path */
		String requestedPath = httpHelper.getRequestURI().getRawPath();
		if (requestedPath.contains("%20")) {
			throw new WebDAVFilterException(HttpServletResponse.SC_BAD_REQUEST, 
				"Request URI '" + requestedPath + "' contains not allowed spaces!");
		}
	}
	
	private String buildCommandMessage(HttpHelper httpHelper, UserCredentials user) {
		String method = httpHelper.getRequestMethod();
		String path = httpHelper.getRequestURI().getPath();
		String destination = httpHelper.hasDestinationHeader() ? " to " + httpHelper.getDestinationURI().getPath() : "";
		String ipSender = httpHelper.getRequest().getRemoteAddr();
		return String.format("%s %s%s from %s ip %s", method, path, destination, user.getFullName(), ipSender);
	}
	
	private void printInCommand(HttpHelper httpHelper, UserCredentials user) {
		log.info("Received {}", buildCommandMessage(httpHelper, user));
	}

	private void printOutCommand(HttpHelper httpHelper, UserCredentials user) {
		int code = httpHelper.getResponse().getStatus();
    String text = (String) httpHelper.getRequest().getAttribute("STATUS_MSG");
    String msg = buildCommandMessage(httpHelper, user) + " exited with " + code + (text != null ? " " + text : "");
    if (code >= 400){
        log.warn(msg);
    }else if (code >= 500) {
        log.error(msg);
    } else {
        log.info(msg);
    }
	}
	
	private boolean isRootPath(String requestedPath) {
		return this.rootPaths.contains(requestedPath);
	}
	
	private void sendError(HttpServletResponse response, int errorCode, String errorMessage) {
		try {
			response.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public void destroy() {
	}
}