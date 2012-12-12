package it.grid.storm;

import it.grid.storm.Configuration;
import it.grid.storm.authorization.AuthorizationFilter;
import it.grid.storm.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.HttpHelper;
import it.grid.storm.webdav.authorization.WebDAVAuthorizationFilter;

import java.io.IOException;

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

	private HttpHelper httpHelper;

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			Configuration.stormBackendHostname = fc.getInitParameter("stormBackendHostname");
			Configuration.stormBackendPort = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
			Configuration.stormBackendServicePort = Integer.valueOf(fc.getInitParameter("stormBackendServicePort"));
			Configuration.stormFrontendHostname = fc.getInitParameter("stormFrontendHostname");
			Configuration.stormFrontendPort = Integer.valueOf(fc.getInitParameter("stormFrontendPort"));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
		printConfiguration();
		/* Load Storage Area List */
		try {
			StorageAreaManager.init(Configuration.stormBackendHostname, Configuration.stormBackendServicePort);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		httpHelper = new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response);

		log.debug("Requested-URI: " + httpHelper.getRequest().getRequestURI());

		AuthorizationFilter filter;
		try {
			if (isFileTransferRequest(httpHelper.getRequest().getRequestURI())) {
				log.info("Received a file-transfer request");
				filter = new FileTransferAuthorizationFilter(httpHelper, "/fileTransfer");
			} else {
				log.info("Received a webdav request");
				filter = new WebDAVAuthorizationFilter(httpHelper);
			}
		} catch (ServletException e) {
			log.error(e.getMessage());
			sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
			return;
		}

		boolean isAuthorized = false;
		try {
			isAuthorized = filter.isUserAuthorized();
		} catch (ServletException e) {
			log.error(e.getMessage());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
		if (!isAuthorized) {
			log.warn("User is not authorized to access the requested resource");
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "You are not authorized to access the requested resource");
			return;
		}
		log.info("User is authorized to access the requested resource");
		chain.doFilter(request, response);
	}

	private boolean isFileTransferRequest(String requestedURI) {
		return requestedURI.startsWith("/fileTransfer");
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			httpHelper.getResponse().sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void printConfiguration() {
		log.debug("StoRM-Authorization-filter init-parameters' values:");
		log.debug(" - stormBackendHostname    : " + Configuration.stormBackendHostname);
		log.debug(" - stormBackendPort        : " + Configuration.stormBackendPort);
		log.debug(" - stormBackendServicePort : " + Configuration.stormBackendServicePort);
		log.debug(" - stormFrontendHostname   : " + Configuration.stormFrontendHostname);
		log.debug(" - stormFrontendPort       : " + Configuration.stormFrontendPort);
	}
}