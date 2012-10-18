package it.grid.storm.webdav.webapp.authorization;

import it.grid.storm.webdav.webapp.authorization.methods.AbstractMethodAuthorization;

import java.io.IOException;
import java.util.ArrayList;
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

	private HttpServletRequest HTTPRequest;
	private HttpServletResponse HTTPResponse;
	
	private VOMSSecurityContext vomsSecurityContext;

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			StormAuthorizationUtils.storageAreaRootDir = fc.getInitParameter("storageAreaRootDir");
			StormAuthorizationUtils.storageAreaName = fc.getInitParameter("storageAreaName");
			StormAuthorizationUtils.storageAreaProtocol = fc.getInitParameter("storageAreaProtocol");
			StormAuthorizationUtils.stormBackendHostname = fc.getInitParameter("stormBackendHostname");
			StormAuthorizationUtils.stormBackendPort = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
			StormAuthorizationUtils.stormBackendServicePort = Integer.valueOf(fc.getInitParameter("stormBackendServicePort"));
			StormAuthorizationUtils.stormFrontendHostname = fc.getInitParameter("stormFrontendHostname");
			StormAuthorizationUtils.stormFrontendPort = Integer.valueOf(fc.getInitParameter("stormFrontendPort"));
		} catch (Exception e) {
			e.printStackTrace();
			throw new ServletException(e.getMessage());
		}

		log.debug("Init-Parameters' values:");
		log.debug(" - storageAreaRootDir      : " + StormAuthorizationUtils.storageAreaRootDir);
		log.debug(" - storageAreaName         : " + StormAuthorizationUtils.storageAreaName);
		log.debug(" - storageAreaProtocol     : " + StormAuthorizationUtils.storageAreaProtocol);
		log.debug(" - stormBackendHostname    : " + StormAuthorizationUtils.stormBackendHostname);
		log.debug(" - stormBackendPort        : " + StormAuthorizationUtils.stormBackendPort);
		log.debug(" - stormBackendServicePort : " + StormAuthorizationUtils.stormBackendServicePort);
		log.debug(" - stormFrontendHostname   : " + StormAuthorizationUtils.stormFrontendHostname);
		log.debug(" - stormFrontendPort       : " + StormAuthorizationUtils.stormFrontendPort);
		
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		// Creating HTTPRequest and HTTPResponse
		HTTPResponse = (HttpServletResponse) response;
		HTTPRequest = (HttpServletRequest) request;
		
		StormAuthorizationUtils.doInitMethodMap(HTTPRequest);
		
		if (!isProtocolAllowed(HTTPRequest.getScheme().toUpperCase())) {
			log.warn("Received a request with a not allowed protocol: " + HTTPRequest.getScheme().toUpperCase());
			sendError(HttpServletResponse.SC_UNAUTHORIZED, "Protocol " + HTTPRequest.getScheme().toUpperCase() + " not allowed!");
			return;
		}
		
		vomsSecurityContext = StormAuthorizationUtils.getVomsSecurityContext(HTTPRequest);
		
		setRequestAttributes();

		if (!isMethodAllowed(HTTPRequest.getMethod())) {
			log.warn("Received a request for a not allowed method : " + HTTPRequest.getMethod());
			sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Method " + HTTPRequest.getMethod() + " not allowed!");
			return;
		}
		
		if (!isUserAuthorized()) {
			log.warn("User is not authorized to access the requested resource");
			sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to access the requested resource");
			return;
		}
		
		log.info("User is authorized to access the requested resource");
		chain.doFilter(request, response);
	}

	private boolean isUserAuthorized() {
		AbstractMethodAuthorization authObj = StormAuthorizationUtils.METHODS_MAP.get(HTTPRequest.getMethod());
		Map<String, String> operationsMap;
		try {
			operationsMap = authObj.getOperationsMap();
		} catch (Exception e) {
			log.error(e.getMessage());
			e.printStackTrace();
			return false;
		}
		boolean isAuthorized = true;
		for (Map.Entry<String, String> entry : operationsMap.entrySet()) {
			String operation = entry.getKey();
			String path = entry.getValue();
			log.debug("Asking authorization for operation " + operation + " on " + path);
			boolean response;
			try {
				response = StormAuthorizationUtils.isUserAuthorized(vomsSecurityContext, operation, path);
			} catch (Exception e) {
				log.error(e.getMessage());
				e.printStackTrace();
				return false;
			}
			log.debug("Response: " + response);
			isAuthorized &= response;
		}
		return isAuthorized;
	
	}

	private boolean isMethodAllowed(String methodName) {
		log.debug("Requested method is : " + methodName);
		return StormAuthorizationUtils.methodAllowed(methodName);
	}

	private void setRequestAttributes() {
		
		HTTPRequest.setAttribute("STORAGE_AREA_ROOT", StormAuthorizationUtils.storageAreaRootDir);
		HTTPRequest.setAttribute("STORAGE_AREA_NAME", StormAuthorizationUtils.storageAreaName);
		HTTPRequest.setAttribute("STORM_BACKEND_HOST", StormAuthorizationUtils.stormBackendHostname);
		HTTPRequest.setAttribute("STORM_BACKEND_PORT", StormAuthorizationUtils.stormBackendPort);
		HTTPRequest.setAttribute("STORM_BACKEND_SERVICE_PORT", StormAuthorizationUtils.stormBackendServicePort);
		HTTPRequest.setAttribute("STORM_FRONTEND_HOST", StormAuthorizationUtils.stormFrontendHostname);
		HTTPRequest.setAttribute("STORM_FRONTEND_PORT", StormAuthorizationUtils.stormFrontendPort);
		HTTPRequest.setAttribute("SUBJECT_DN", StormAuthorizationUtils.getUserDN(vomsSecurityContext));
		//HTTPRequest.setAttribute("FQANS", StringUtils.join(StormAuthorizationUtils.getUserFQANs(vomsSecurityContext), ","));
	
		/********************************TEST***********************************/
		ArrayList<String> fqans = new ArrayList<String>();
		if (HTTPRequest.getScheme().toUpperCase().equals("HTTPS")) {
			fqans.clear();
			fqans.add("/dteam/Role=NULL/Capability=NULL");
			fqans.add("/dteam/NGI_IT/Role=NULL/Capability=NULL");
		}
		HTTPRequest.setAttribute("FQANS", StringUtils.join(fqans, ","));
		/********************************TEST***********************************/
	
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
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

}