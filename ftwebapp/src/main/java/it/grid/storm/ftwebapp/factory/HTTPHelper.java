package it.grid.storm.ftwebapp.factory;

import it.grid.storm.ftwebapp.authorization.StormAuthorizationFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HTTPHelper {
	
	private static final Logger log = LoggerFactory.getLogger(HTTPHelper.class);

	public static HttpServletRequest getRequest() {
		return StormAuthorizationFilter.HTTPRequest;
	}
	
	public static HttpServletResponse getResponse() {
		return StormAuthorizationFilter.HTTPResponse;
	}
	
	public static String getRequestMethod() {
		return getRequest().getMethod().toUpperCase();
	}

	public static void sendError(int code, String description) {
		try {
			getResponse().sendError(code, description);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}
	
	public static X509Certificate[] getX509Certificate() { 
		X509Certificate[] certChain;
		try {
			certChain = (X509Certificate[]) getRequest().getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.error("Error fetching certificate from http request: " + e.getMessage());
			return null;
		}
		return certChain;
	}
	
	public static String getOverwriteHeader() {
		return getRequest().getHeader("Overwrite");
	}
	
	public static boolean isOverwriteRequest() {
		String methodName = getRequestMethod();
		String overwrite = getOverwriteHeader();
		if (methodName.toUpperCase().equals("PUT")) 
			return ((overwrite == null) || (overwrite.equals("T")));
		return false;
	}
	
}