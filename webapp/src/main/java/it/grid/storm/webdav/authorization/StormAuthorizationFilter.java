package it.grid.storm.webdav.authorization;

import it.grid.storm.webdav.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.webdav.factory.StormResourceFactory;

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
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class StormAuthorizationFilter implements Filter {

	private String storageAreaRootDir;
	private String storageAreaName;

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	public void destroy() {
		// TODO Auto-generated method stub
	}

	public void init(FilterConfig arg0) throws ServletException {

		// Setting paths from applicationContext.xml
		StaticApplicationContext parent = new StaticApplicationContext();
		parent.refresh();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "applicationContext.xml" }, parent);

		if (context.containsBean("milton.fs.resource.factory")) {
			Object beanFactory = context.getBean("milton.fs.resource.factory");
			if (beanFactory instanceof StormResourceFactory) {
				this.storageAreaRootDir = ((StormResourceFactory) beanFactory).getRoot().getAbsolutePath();
				this.storageAreaName = ((StormResourceFactory) beanFactory).getContextPath();
				log.info("storageAreaRootDir: " + this.storageAreaRootDir);
				log.info("storageAreaName: " + this.storageAreaName);
			}
		}

	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		// Setting HTTPRequest and HTTPResponse

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

		HTTPRequest.setAttribute("STORAGE_AREA_ROOT", this.storageAreaRootDir);
		HTTPRequest.setAttribute("STORAGE_AREA_NAME", this.storageAreaName);

		HTTPRequest.setAttribute("STORM_BACKEND_HOST", StormAuthorizationUtils.STORM_BE_HOSTNAME);
		HTTPRequest.setAttribute("STORM_BACKEND_PORT", StormAuthorizationUtils.STORM_BE_PORT);
		
		/* *********************************************** */

		// Setting subjectDN and FQANS from certificate and VOMS attributes

		String subjectDN;
		String[] fqans;
		try {
			VOMSSecurityContext.clearCurrentContext();
			VOMSSecurityContext sc = new VOMSSecurityContext();
			VOMSSecurityContext.setCurrentContext(sc);
			X509Certificate[] certChain = getCertChain(HTTPRequest);
			sc.setClientCertChain(certChain);
			subjectDN = sc.getClientDN().getRFCDNv2();
			fqans = sc.getFQANs();
		} catch (Exception e) {
			log.warn(e.getMessage());
			return;
		}

		HTTPRequest.setAttribute("SUBJECT_DN", subjectDN);
		HTTPRequest.setAttribute("FQANS", fqans);

		/* *********************************************** */

		// Check if method is allowed

		String methodName = HTTPRequest.getMethod();
		log.debug("Requested method is : " + methodName);
		if (!StormAuthorizationUtils.methodAllowed(methodName)) {
			log.warn("Received a request for a not allowed method : " + methodName);
			HTTPResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method " + methodName + " not allowed!");
			return;
		}

		/* *********************************************** */

		// Check if the user is authorized

		AbstractMethodAuthorization m = StormAuthorizationUtils.METHODS_MAP.get(methodName);
		Map<String, String> operationsMap = m.getOperationsMap(HTTPRequest);
		boolean isAuthorized = true;

		if (!this.storageAreaName.equals("WebDAV-fs-server")) { //TO REMOVE - ONLY FOR TEST
		
		for (Map.Entry<String, String> entry : operationsMap.entrySet()) {
			String op = entry.getKey();
			String path = entry.getValue();

			log.debug("Asking authorization for operation " + op + " on " + path);
			try {
				isAuthorized = isAuthorized && StormAuthorizationUtils.isUserAuthorized(subjectDN, fqans, op, path);
			} catch (Exception e) {
				log.error("Unable to verify user authorization. ServletException : " + e.getMessage());
				HTTPResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error testing user authorization: " + e.getMessage());
				return;
			}

		}

		} //TO REMOVE - ONLY FOR TEST
		
		if (isAuthorized) {
			log.info("User is authorized to access the requested resource");
			chain.doFilter(request, response);
		} else {
			log.warn("User is not authorized to access the requested resource");
			HTTPResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to access the requested resource");
			return;
		}

	}

	private X509Certificate[] getCertChain(HttpServletRequest request) throws Exception {

		X509Certificate[] certChain = null;
		try {
			certChain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.warn("Error fetching certificate from http request: " + e.getMessage());
		}
		if (certChain == null)
			throw new Exception("Unauthenticated connection from " + request.getRemoteAddr());
		return certChain;
	}

}