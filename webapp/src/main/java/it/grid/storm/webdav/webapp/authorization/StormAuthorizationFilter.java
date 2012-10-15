package it.grid.storm.webdav.webapp.authorization;

import it.grid.storm.webdav.webapp.authorization.methods.AbstractMethodAuthorization;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private String storageAreaRootDir;
	private String storageAreaName;
	private String storageAreaProtocol;
	private String stormBackendHostname;
	private int stormBackendPort;
	private String subjectDN = "";
	private String[] fqans = {};
	private HttpServletRequest HTTPRequest;
	private HttpServletResponse HTTPResponse;

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			this.storageAreaRootDir = fc.getInitParameter("storageAreaRootDir");
			this.storageAreaName = fc.getInitParameter("storageAreaName");
			this.storageAreaProtocol = fc.getInitParameter("storageAreaProtocol");
			this.stormBackendHostname = fc.getInitParameter("stormBackendHostname");
			this.stormBackendPort = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		}

		log.debug("Init-Parameters' values:");
		log.debug(" - storageAreaRootDir    : " + this.storageAreaRootDir);
		log.debug(" - storageAreaName       : " + this.storageAreaName);
		log.debug(" - storageAreaProtocol   : " + this.storageAreaProtocol);
		log.debug(" - stormBackendHostname  : " + this.stormBackendHostname);
		log.debug(" - stormBackendPort      : " + this.stormBackendPort);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		// Creating HTTPRequest and HTTPResponse
		HTTPResponse = (HttpServletResponse) response;
		HTTPRequest = (HttpServletRequest) request;

		if (!isProtocolAllowed())
			return;

		// Setting subjectDN and FQANS from certificate and VOMS attributes
		initVomsSecurityContext();

		/********************************TEST***********************************/
		if (HTTPRequest.getScheme().toUpperCase().equals("HTTPS")) {
			fqans = new String[2];
			fqans[0] = "/dteam/Role=NULL/Capability=NULL";
			fqans[1] = "/dteam/NGI_IT/Role=NULL/Capability=NULL";
		}
		/********************************TEST***********************************/

		setRequestAttributes();

		if (!isMethodAllowed())
			return;

		log.debug("protocol: " + HTTPRequest.getScheme());
		if (HTTPRequest.getScheme().toUpperCase().equals("HTTP") || isUserAuthorized()) {
			log.info("User is authorized to access the requested resource");
			chain.doFilter(request, response);
		} else {
			log.warn("User is not authorized to access the requested resource");
			sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to access the requested resource");
		}
	}

	private boolean isUserAuthorized() {
		AbstractMethodAuthorization m = StormAuthorizationUtils.METHODS_MAP.get(HTTPRequest.getMethod());
		Map<String, String> operationsMap;
		try {
			operationsMap = m.getOperationsMap(HTTPRequest);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		boolean isAuthorized = true;
		for (Map.Entry<String, String> entry : operationsMap.entrySet()) {
			String op = entry.getKey();
			String path = entry.getValue();
			log.debug("Asking authorization for operation " + op + " on " + path);
			try {
				isAuthorized = isAuthorized
						&& StormAuthorizationUtils.isUserAuthorized(stormBackendHostname, stormBackendPort, subjectDN, fqans, op, path);
			} catch (Exception e) {
				log.error("Unable to verify user authorization. ServletException : " + e.getMessage());
				sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error testing user authorization: " + e.getMessage());
				return false;
			}
		}
		return isAuthorized;
	}

	private boolean isMethodAllowed() {
		String methodName = HTTPRequest.getMethod();
		log.debug("Requested method is : " + methodName);
		boolean isAllowed = StormAuthorizationUtils.methodAllowed(methodName);
		if (!isAllowed) {
			log.warn("Received a request for a not allowed method : " + methodName);
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method " + methodName + " not allowed!");
		}
		return isAllowed;
	}

	private void setRequestAttributes() {
		HTTPRequest.setAttribute("STORAGE_AREA_ROOT", storageAreaRootDir);
		HTTPRequest.setAttribute("STORAGE_AREA_NAME", storageAreaName);
		HTTPRequest.setAttribute("STORM_BACKEND_HOST", stormBackendHostname);
		HTTPRequest.setAttribute("STORM_BACKEND_PORT", stormBackendPort);
		HTTPRequest.setAttribute("SUBJECT_DN", subjectDN);
		HTTPRequest.setAttribute("FQANS", StringUtils.join(fqans, ","));
	}

	private void initVomsSecurityContext() {

		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext sc = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(sc);
		X509Certificate[] certChain;
		try {
			certChain = (X509Certificate[]) HTTPRequest.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.warn("Error fetching certificate from http request: " + e.getMessage());
			return;
		}
		if (certChain == null)
			return;
		sc.setClientCertChain(certChain);
		subjectDN = sc.getClientDN().getX500();
		log.debug("subjectDN = " + subjectDN);
		fqans = sc.getFQANs();
		log.debug("FQANs = " + StringUtils.join(fqans, ","));
	}

	private boolean isProtocolAllowed() {
		String protocol = HTTPRequest.getScheme();
		boolean isAllowed = false;
		try {
			isAllowed = StormAuthorizationUtils.protocolAllowed(this.storageAreaProtocol, protocol);
		} catch (Exception e) {
			log.error(e.getMessage());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return false;
		}
		if (!isAllowed) {
			log.warn("Received a request with a not allowed protocol: " + protocol);
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Protocol " + protocol + " not allowed!");
		}
		return isAllowed;
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			HTTPResponse.sendError(errorCode, errorMessage);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}