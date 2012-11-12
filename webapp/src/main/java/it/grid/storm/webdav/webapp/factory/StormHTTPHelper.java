package it.grid.storm.webdav.webapp.factory;

import it.grid.storm.webdav.webapp.authorization.StormAuthorizationFilter;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormHTTPHelper {
	
	public static final int DEPTH_NULL = -1;
	public static final int DEPTH_0 = 0;
	public static final int DEPTH_1 = 1;
	public static final int DEPTH_INFINITY = 2;

	private static final Logger log = LoggerFactory.getLogger(StormHTTPHelper.class);

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
		if (methodName.toUpperCase().equals("COPY"))
			return ((overwrite != null) && (overwrite.equals("T")));
		if (methodName.toUpperCase().equals("MOVE")) 
			return ((overwrite != null) && (overwrite.equals("T")));
		return false;
	}

	public static int getDepthHeader() {
		String depth = getRequest().getHeader("Depth");
		if ((depth == null) || (depth.equals("infinity")))
			return DEPTH_INFINITY;
		if (depth.equals("0"))
			return DEPTH_0;
		if (depth.equals("1"))
			return DEPTH_1;
		return DEPTH_NULL;
	}

	public static boolean isDepthInfinity() {
		String methodName = getRequestMethod();
		int depth = getDepthHeader();
		if (methodName.toUpperCase().equals("COPY")) 
			return (depth == DEPTH_NULL || depth == DEPTH_INFINITY);
		if (methodName.toUpperCase().equals("PROPFIND")) 
			return (depth == DEPTH_NULL || depth == DEPTH_INFINITY);
		if (methodName.toUpperCase().equals("DELETE")) 
			return true;
		return false;
	}
	
	public static boolean isHttp() {
		return StormAuthorizationFilter.HTTPRequest.getScheme().toUpperCase().equals("HTTP");
	}
	
	public static boolean isHttps() {
		return StormAuthorizationFilter.HTTPRequest.getScheme().toUpperCase().equals("HTTPS");
	}
	
}