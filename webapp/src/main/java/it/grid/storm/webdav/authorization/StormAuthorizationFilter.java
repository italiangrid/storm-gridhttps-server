package it.grid.storm.webdav.authorization;

import it.grid.storm.webdav.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.webdav.factory.StormResourceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.security.cert.X509Certificate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

		/* *********************************************** */

		// Initializing parameters

		String subjectDN = null;
		String[] fqans = {};
		String methodName = null;

		/* *********************************************** */

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

		/* *********************************************** */

		// Setting subjectDN and fqans from certificate and VOMS attributes

		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext sc = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(sc);

		X509Certificate[] certChain = null;

		try {
			certChain = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.warn("Error fetching certificate from http request: " + e.getMessage());
		}

		if (certChain == null) {
			log.warn("Unauthenticated connection from " + request.getRemoteAddr());
			return;
		}

		sc.setClientCertChain(certChain);

		subjectDN = sc.getClientDN().getRFCDNv2();
		fqans = sc.getFQANs();

		/* *********************************************** */

		// Instead of getting info from the certificate...

		//subjectDN = StormAuthorizationUtils.SUBJECT_DN;
		// fqans are already null

		/* *********************************************** */

		// Setting methodName

		methodName = HTTPRequest.getMethod();
		log.debug("Requested method is : " + methodName);
		if (!StormAuthorizationUtils.methodAllowed(methodName)) {
			log.warn("Received a request for a not allowed method : " + methodName);
			HTTPResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method " + methodName + " not allowed!");
			return;
		}

		/* *********************************************** */

		// Checking if the user is authorized

		AbstractMethodAuthorization m = StormAuthorizationUtils.METHODS_MAP.get(methodName);
		m.init(storageAreaRootDir, storageAreaName);
		Map<String, String> operationsMap = m.getOperationsMap(HTTPRequest);

		boolean isAuthorized = true;

		for (Map.Entry<String, String> entry : operationsMap.entrySet()) {
			String op = entry.getKey();
			String path = entry.getValue();

			log.debug("Asking authorization for operation " + op + " on " + path);
			try {
				isAuthorized = isAuthorized && isUserAuthorized(path, op, subjectDN, fqans);
			} catch (ServletException e) {
				log.error("Unable to verify user authorization. ServletException : " + e.getMessage());
				HTTPResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error testing user authorization: " + e.getMessage());
				return;
			}

		}

		if (isAuthorized) {
			log.info("User is authorized to access the requested resource");
			chain.doFilter(request, response);
		} else {
			log.warn("User is not authorized to access the requested resource");
			HTTPResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "You are not authorized to access the requested resource");
			return;
		}

	}

//	private boolean isUserAuthorizedFake(String path, String operation, String subjectDN, String[] fqans) throws ServletException,
//			IllegalArgumentException {
//		return true;
//	}

	private boolean isUserAuthorized(String path, String operation, String subjectDN, String[] fqans) throws ServletException,
			IllegalArgumentException {
		if (path == null || operation == null || subjectDN == null || fqans == null) {
			log.error("Received null parameter(s) at isUserAuthorized: path=" + path + " operation=" + operation + " subjectDN="
					+ subjectDN + " fqans=" + fqans);
			throw new IllegalArgumentException("Received null parameter(s)");
		}
		URI uri = StormAuthorizationUtils.prepareURI(path, operation, subjectDN, fqans);
		log.debug("Authorization request uri = " + uri.toString());
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException " + e.getLocalizedMessage());
			throw new ServletException("Error contacting authorization service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException " + e.getLocalizedMessage());
			throw new ServletException("Error contacting authorization service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new ServletException("Unexpected error! response.getStatusLine() returned null! Please contact storm support");
		}
		int httpCode = status.getStatusCode();
		String httpMessage = status.getReasonPhrase();
		HttpEntity entity = httpResponse.getEntity();
		String output = "";
		if (entity != null) {
			InputStream responseIS;
			try {
				responseIS = entity.getContent();
			} catch (IllegalStateException e) {
				log.error("unable to get the input content stream from server answer. IllegalStateException " + e.getLocalizedMessage());
				throw new ServletException("Error comunicationg with the authorization service.");
			} catch (IOException e) {
				log.error("unable to get the input content stream from server answer. IOException " + e.getLocalizedMessage());
				throw new ServletException("Error comunicationg with the authorization service.");
			}
			int l;
			byte[] tmp = new byte[512];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output = output + (new String(tmp, 0, l));
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException " + e.getMessage());
				throw new ServletException("Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new ServletException("Unable to get a valid authorization response from the server.");
		}
		log.debug("Authorization response is : '" + output + "'");
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : '" + httpCode + "' "
					+ httpMessage);
			throw new ServletException("Unable to get a valid response from server. Received a non HTTP 200 response from the server : '"
					+ httpCode + "' " + httpMessage);
		}
		Boolean response = new Boolean(output);
		log.debug("Authorization response (Boolean value): '" + response + "'");
		return response.booleanValue();
	}

}