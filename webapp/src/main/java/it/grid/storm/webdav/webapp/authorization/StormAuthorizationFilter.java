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

import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private String storageAreaRootDir;
	private String storageAreaName;
	private String storageAreaProtocol;
	private String stormBackendHostname;
	private int stormBackendPort;

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
		// TODO Auto-generated method stub
	}

	public void init(FilterConfig fc) throws ServletException {

		this.storageAreaRootDir = fc.getInitParameter("storageAreaRootDir");
		this.storageAreaName = fc.getInitParameter("storageAreaRootDir");
		this.storageAreaProtocol = fc.getInitParameter("storageAreaProtocol");
		this.stormBackendHostname = fc.getInitParameter("storageAreaRootDir");
		this.stormBackendPort = Integer.valueOf(fc.getInitParameter("storageAreaRootDir"));

		log.info("storageAreaRootDir: " + this.storageAreaRootDir);
		log.info("storageAreaName: " + this.storageAreaName);
		log.info("storageAreaProtocol: " + this.storageAreaProtocol);
		log.info("stormBackendHostname: " + this.stormBackendHostname);
		log.info("stormBackendPort: " + this.stormBackendPort);
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {

		// Creating HTTPRequest and HTTPResponse

		HttpServletResponse HTTPResponse = null;
		HttpServletRequest HTTPRequest = null;
		if (HttpServletRequest.class.isAssignableFrom(request.getClass())
				&& HttpServletResponse.class.isAssignableFrom(response.getClass())) {
			HTTPRequest = (HttpServletRequest) request;
			HTTPResponse = (HttpServletResponse) response;
		} else {
			log.error("Received non HTTP request. Class is : " + request.getClass());
			throw new ServletException("Protocol not supported. Use HTTP(S)");
		}

		/* *********************************************** */

		// Checking if the protocol is allowed
		String requestProtocol = HTTPRequest.getScheme();
		try {
			if (!StormAuthorizationUtils.protocolAllowed(this.storageAreaProtocol, requestProtocol)) {
				log.warn("Received a request with a not allowed protocol: " + requestProtocol);
				HTTPResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Protocol " + requestProtocol
						+ " not allowed!");
				return;
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			HTTPResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			return;
		}

		/* *********************************************** */

		// Setting request attributes

		HTTPRequest.setAttribute("STORAGE_AREA_ROOT", this.storageAreaRootDir);
		HTTPRequest.setAttribute("STORAGE_AREA_NAME", this.storageAreaName);
		HTTPRequest.setAttribute("STORM_BACKEND_HOST", this.stormBackendHostname);
		HTTPRequest.setAttribute("STORM_BACKEND_PORT", this.stormBackendPort);

		/* *********************************************** */

		// Setting subjectDN and FQANS from certificate and VOMS attributes

		String subjectDN = ""; // in case of HTTP it is an empty String and not null!
		String[] fqans = {};

		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext sc = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(sc);
		X509Certificate[] certChain = getCertChain(HTTPRequest);

		if (certChain != null) {
			sc.setClientCertChain(certChain);
			subjectDN = sc.getClientDN().getX500();
			fqans = sc.getFQANs();
		}

		HTTPRequest.setAttribute("SUBJECT_DN", subjectDN);
		HTTPRequest.setAttribute("FQANS", fqans);

		/* *********************************************** */

		// Check if method is allowed

		String methodName = HTTPRequest.getMethod();
		log.debug("Requested method is : " + methodName);
		if (!StormAuthorizationUtils.methodAllowed(methodName)) {
			log.warn("Received a request for a not allowed method : " + methodName);
			HTTPResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method " + methodName
					+ " not allowed!");
			return;
		}

		/* *********************************************** */

		// Check if the user is authorized

		AbstractMethodAuthorization m = StormAuthorizationUtils.METHODS_MAP.get(methodName);
		Map<String, String> operationsMap = m.getOperationsMap(HTTPRequest);
		boolean isAuthorized = true;

		for (Map.Entry<String, String> entry : operationsMap.entrySet()) {
			String op = entry.getKey();
			String path = entry.getValue();

			log.debug("Asking authorization for operation " + op + " on " + path);
			try {
				isAuthorized = isAuthorized
						&& StormAuthorizationUtils.isUserAuthorized(stormBackendHostname, stormBackendPort, subjectDN,
								fqans, op, path);
			} catch (Exception e) {
				log.error("Unable to verify user authorization. ServletException : " + e.getMessage());
				HTTPResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error testing user authorization: " + e.getMessage());
				return;
			}

		}

		if (isAuthorized) {
			log.info("User is authorized to access the requested resource");
			chain.doFilter(request, response);
		} else {
			log.warn("User is not authorized to access the requested resource");
			HTTPResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
					"You are not authorized to access the requested resource");
			return;
		}

	}

	private X509Certificate[] getCertChain(HttpServletRequest request) {

		X509Certificate[] certChain = null;
		try {
			certChain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.warn("Error fetching certificate from http request: " + e.getMessage());
		}
		// if (certChain == null)
		// throw new Exception("Unauthenticated connection from " +
		// request.getRemoteAddr());
		return certChain;
	}

}