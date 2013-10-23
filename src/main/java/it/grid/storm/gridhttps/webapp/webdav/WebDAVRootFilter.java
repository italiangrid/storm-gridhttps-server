package it.grid.storm.gridhttps.webapp.webdav;

import it.grid.storm.gridhttps.configuration.Configuration;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.authorization.Constants;
import it.grid.storm.gridhttps.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.StormResourceHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.webdav.factory.html.StormHtmlRootPage;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

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

public class WebDAVRootFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(WebDAVRootFilter.class);
	
	private ArrayList<String> rootPaths = new ArrayList<String>();
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.info("WebDAVRootFilter - Init");
		rootPaths.clear();
		rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath());
		rootPaths.add(File.separator + Configuration.getGridhttpsInfo().getWebdavContextPath() + File.separator);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {

		HttpHelper httpHelper = new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response);
		UserCredentials user = httpHelper.getUser();

		String requestedPath = httpHelper.getRequestURI().getRawPath();
		log.debug(this.getClass().getName() + " - Requested-URI: " + requestedPath);

		if (isRootPath(requestedPath)) {
			log.debug("Requested-URI is root");
			processRootRequest(httpHelper, user);
		} else {
			chain.doFilter(request, response);
		}
		
	}

	@Override
	public void destroy() {
		
	}
	
	private boolean isRootPath(String requestedPath) {
		return this.rootPaths.contains(requestedPath);
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