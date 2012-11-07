package it.grid.storm.ftwebapp;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ftMethodFilter implements Filter {

	public static HttpServletRequest HTTPRequest;
	public static HttpServletResponse HTTPResponse;
	private List<String> allowedMethods;
	private List<String> allowedProtocols;
	
	private static final Logger log = LoggerFactory.getLogger(ftMethodFilter.class);

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			allowedMethods = Arrays.asList(StringUtils.split(fc.getInitParameter("allowed.methods").toUpperCase(), ','));
			allowedProtocols = Arrays.asList(StringUtils.split(fc.getInitParameter("allowed.protocols").toUpperCase(), ','));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		}
		log.debug("Allowed methods: " + StringUtils.join(((String[])allowedMethods.toArray())));	
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HTTPRequest = (HttpServletRequest) request;
		HTTPResponse = (HttpServletResponse) response;
		
		if (!isProtocolAllowed(HTTPRequest.getScheme())) {
			log.warn("Received a request with a not allowed protocol: " + HTTPRequest.getScheme());
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Protocol " + HTTPRequest.getScheme() + " not allowed!");
			return;
		}
		log.debug(HTTPRequest.getScheme().toUpperCase() + " protocol is allowed");
				
		if (!isMethodAllowed(HTTPRequest.getMethod())) {
			log.warn("Received a request for a not allowed method : " + HTTPRequest.getMethod());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method " + HTTPRequest.getMethod() + " not allowed!");
			return;
		}
		log.debug(HTTPRequest.getMethod() + " method is allowed");
		
		chain.doFilter(request, response);
	}
	
	private boolean isMethodAllowed(String methodName) {
		return allowedMethods.contains(methodName.toUpperCase());
	}

	private boolean isProtocolAllowed(String protocol) {
		return allowedProtocols.contains(protocol.toUpperCase());
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