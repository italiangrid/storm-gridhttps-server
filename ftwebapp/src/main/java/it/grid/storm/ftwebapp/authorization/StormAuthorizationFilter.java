package it.grid.storm.ftwebapp.authorization;

import it.grid.storm.ftwebapp.Configuration;

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

	public static HttpServletRequest HTTPRequest;
	public static HttpServletResponse HTTPResponse;
	
	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			Configuration.storageAreaRootDir = fc.getInitParameter("storageAreaRootDir");
			Configuration.storageAreaName = fc.getInitParameter("storageAreaName");
			Configuration.storageAreaProtocol = fc.getInitParameter("storageAreaProtocol");
			Configuration.stormBackendHostname = fc.getInitParameter("stormBackendHostname");
			Configuration.stormBackendPort = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
			Configuration.stormBackendServicePort = Integer.valueOf(fc.getInitParameter("stormBackendServicePort"));
			Configuration.stormFrontendHostname = fc.getInitParameter("stormFrontendHostname");
			Configuration.stormFrontendPort = Integer.valueOf(fc.getInitParameter("stormFrontendPort"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		}
		log.debug("Init-Parameters' values:");
		log.debug(" - storageAreaRootDir      : " + Configuration.storageAreaRootDir);
		log.debug(" - storageAreaName         : " + Configuration.storageAreaName);
		log.debug(" - storageAreaProtocol     : " + Configuration.storageAreaProtocol);
		log.debug(" - stormBackendHostname    : " + Configuration.stormBackendHostname);
		log.debug(" - stormBackendPort        : " + Configuration.stormBackendPort);
		log.debug(" - stormBackendServicePort : " + Configuration.stormBackendServicePort);
		log.debug(" - stormFrontendHostname   : " + Configuration.stormFrontendHostname);
		log.debug(" - stormFrontendPort       : " + Configuration.stormFrontendPort);	
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HTTPRequest = (HttpServletRequest) request;
		HTTPResponse = (HttpServletResponse) response;
		
		if (!isProtocolAllowed(HTTPRequest.getScheme().toUpperCase())) {
			log.warn("Received a request with a not allowed protocol: " + HTTPRequest.getScheme().toUpperCase());
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Protocol " + HTTPRequest.getScheme().toUpperCase() + " not allowed!");
			return;
		}
		log.debug(HTTPRequest.getScheme().toUpperCase() + " protocol is allowed");
				
		if (!isMethodAllowed(HTTPRequest.getMethod())) {
			log.warn("Received a request for a not allowed method : " + HTTPRequest.getMethod());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method " + HTTPRequest.getMethod() + " not allowed!");
			return;
		}
		log.debug(HTTPRequest.getMethod() + " method is allowed");
		
		if (!isUserAuthorized(HTTPRequest)) {
			log.warn("User is not authorized to access the requested resource");
			sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to access the requested resource");
			return;
		}		
		log.info("User is authorized to access the requested resource");
		
		chain.doFilter(request, response);
	}

	private boolean isUserAuthorized(HttpServletRequest HTTPRequest) throws ServletException {
		return StormAuthorizationUtils.getAuthorizationHandler(HTTPRequest).isUserAuthorized();
	}

	private boolean isMethodAllowed(String methodName) {
		return StormAuthorizationUtils.methodAllowed(methodName);
	}

	private boolean isProtocolAllowed(String protocol) {
		boolean isAllowed = false;
		try {
			isAllowed = StormAuthorizationUtils.protocolAllowed(protocol);
		} catch (Exception e) {
			log.error(e.getMessage());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return false;
		}
		return isAllowed;
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			HTTPResponse.sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

}