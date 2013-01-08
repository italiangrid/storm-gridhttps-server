package it.grid.storm.gridhttps.webapp;

import io.milton.servlet.MiltonServlet;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpHelper {

	public static final int DEPTH_NULL = -1;
	public static final int DEPTH_0 = 0;
	public static final int DEPTH_1 = 1;
	public static final int DEPTH_INFINITY = 2;

	private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

	private HttpServletRequest HTTPRequest = null;
	private HttpServletResponse HTTPResponse = null;

	public static HttpHelper getInstance() {
		return new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
	}

	public HttpHelper(HttpServletRequest HTTPRequest, HttpServletResponse HTTPResponse) {
		this.HTTPRequest = HTTPRequest;
		this.HTTPResponse = HTTPResponse;
	}

	private HttpServletRequest getRequest() {
		return this.HTTPRequest;
	}
	
	public HttpSession getSession() {
		return this.HTTPRequest.getSession();
	}
	
	public void initSession() {
		this.HTTPRequest.getSession(true);
	}

	public HttpServletResponse getResponse() {
		return this.HTTPResponse;
	}
	
	public String getRequestStringURI() {
//		if (Configuration.getRemoveSpaces())
//			return getRequest().getRequestURI().replaceAll(" ", Configuration.getRemoveSpacesWith());
		return getRequest().getRequestURI();
	}

	public URI getRequestURI() {
		URI uri = null;
		try {
			uri = new URI(getRequestStringURI());
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Unable to create URI object from the string: " + getRequestStringURI());
		}
		return uri;
	}

	public String getRequestMethod() {
		return getRequest().getMethod().toUpperCase();
	}

	public String getRequestProtocol() {
		return getRequest().getScheme().toUpperCase();
	}

	public String getDestinationHeader() {
//		if (Configuration.getRemoveSpaces())
//			return getRequest().getHeader("Destination").replaceAll(" ", Configuration.getRemoveSpacesWith());
		return getRequest().getHeader("Destination");
	}

	public URI getDestinationURI() {
		URI uri = null;
		try {
			uri = new URI(getDestinationHeader());
		} catch (URISyntaxException e) {
			log.error(e.getMessage());
			throw new RuntimeException("Unable to create URI object from the string: " + getDestinationHeader());
		}
		return uri;
	}

	public String getDestinationProtocol() {
		URI destination = getDestinationURI();
		return destination.getScheme() != null ? destination.getScheme().toUpperCase() : getRequestProtocol();
	}

	public String getOverwriteHeader() {
		if (Configuration.getRemoveSpaces())
			return getRequest().getHeader("Overwrite").replaceAll(" ", "");
		return getRequest().getHeader("Overwrite");
	}

	public X509Certificate[] getX509Certificate() {
		X509Certificate[] certChain;
		try {
			certChain = (X509Certificate[]) getRequest().getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.error("Error fetching certificate from http request: " + e.getMessage());
			return null;
		}
		return certChain;
	}

	public boolean isOverwriteRequest() {
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

	public int getDepthHeader() {
		String depth = getRequest().getHeader("Depth");
		if ((depth == null) || (depth.equals("infinity")))
			return DEPTH_INFINITY;
		if (depth.equals("0"))
			return DEPTH_0;
		if (depth.equals("1"))
			return DEPTH_1;
		return DEPTH_NULL;
	}

	public boolean isDepthInfinity() {
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

	public boolean isHttp() {
		return getRequestProtocol().equals("HTTP");
	}

	public boolean isHttps() {
		return getRequestProtocol().equals("HTTPS");
	}

	public void sendError(int code, String description) {
		try {
			getResponse().sendError(code, description);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

}