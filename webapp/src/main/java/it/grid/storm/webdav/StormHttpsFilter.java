package it.grid.storm.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Arrays;

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

public class StormHttpsFilter implements Filter {

	private static final Logger log = LoggerFactory
			.getLogger(StormHttpsFilter.class);

	private String subjectDN = null;
	private String[] fqans = {};
	private String methodName = null;
	private String resourcePath = null;
	private String destinationPath = null;

	public void destroy() {
		log.debug("StormHttpsUtilsFilter destroied.");

	}

	public void init(FilterConfig filterConfig) throws ServletException {
		// nope
	}

	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {

		/* *********************************************** */

		// Setting HTTPRequest and HTTPResponse

		HttpServletResponse HTTPResponse = null;
		HttpServletRequest HTTPRequest = null;
		if (HttpServletRequest.class.isAssignableFrom(request.getClass())
				&& HttpServletResponse.class.isAssignableFrom(response
						.getClass())) {
			HTTPRequest = (HttpServletRequest) request;
			HTTPResponse = (HttpServletResponse) response;
		} else {
			log.error("Received non HTTP request. Class is : "
					+ request.getClass());
			throw new ServletException("Protocol not supported. Use HTTP(S)");
		}

		/* *********************************************** */

		// Checking schema

		// Is needed???

		String schema = request.getScheme();
		log.debug("Requested schema is : " + schema);
		if (!StormHttpsUtils.checkSchema(schema)) {
			log.warn("Received a request with au unknown schema. Schema is : "
					+ schema);
			throw new ServletException("Schema not supported. Use HTTP(S)");
		}

		/* *********************************************** */

		// Setting security context (only with certificates)

		// VOMSSecurityContext.clearCurrentContext();
		// VOMSSecurityContext sc = new VOMSSecurityContext();
		// VOMSSecurityContext.setCurrentContext(sc);
		//
		// X509Certificate[] certChain = null;
		//
		// try {
		//
		// certChain = (X509Certificate[])
		// request.getAttribute("javax.servlet.request.X509Certificate");
		//
		// } catch (Exception e) {
		// log.warn("Error fetching certificate from http request: {}",
		// e.getMessage(), e);
		// // We swallow the exception and continue processing.
		// }
		//
		// if (certChain == null){
		// log.info("Unauthenticated connection from {}",
		// request.getRemoteAddr());
		// return;
		// }
		//
		// sc.setClientCertChain(certChain);
		//
		// this.subjectDN = sc.getClientDN().getRFCDNv2();
		// this.fqans = sc.getFQANs();

		/* *********************************************** */

		// Instead of certificates...

		this.subjectDN = StormHttpsUtils.SUBJECT_DN;
		// fqans are already null

		/* *********************************************** */

		// Setting methodName

		this.methodName = HTTPRequest.getMethod();
		log.debug("Requested method is : " + this.methodName);
		if (!StormHttpsUtils.methodAllowed(this.methodName)) {
			log.warn("Received a request for a not allowed method : "
					+ this.methodName);
			HTTPResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
					"Method " + this.methodName + " not allowed!");
			return;
		}

		/* *********************************************** */

		// Setting resourcePath

		this.resourcePath = StormHttpsUtils.prepareResourcePath(HTTPRequest
				.getRequestURI());

		/* *********************************************** */

		// Setting destinationPath (if it exists)

		String destinationURI = HTTPRequest.getHeader("Destination");
		if (destinationURI != null)
			this.destinationPath = StormHttpsUtils
					.prepareResourcePath(destinationURI);

		/* *********************************************** */

		// log.info("methodName is : " + this.methodName);
		// log.info("fqans are : " + Arrays.toString(this.fqans));
		// log.info("subjectDN is : " + this.subjectDN);
		// log.info("resourcePath is : " + this.resourcePath);
		// if (this.destinationPath != null) log.info("destinationPath is : " +
		// this.destinationPath);

		/* *********************************************** */

		// Retriving sourceOperation and destinationOperation

		String sourceOperation = StormHttpsUtils
				.sourceOperation(this.methodName);
		String destinationOperation = StormHttpsUtils
				.destinationOperation(this.methodName);

		if ((destinationOperation != null) && (this.destinationPath == null)) {
			throw new ServletException(
					"No Destination header found in the request for the method "
							+ this.methodName);
		}

		String summary = "The method " + this.methodName
				+ " requires authorization for these operations:\n";
		summary = summary + " -> '" + sourceOperation + "' on "
				+ this.resourcePath;
		if (destinationOperation != null)
			summary = summary + "\n" + " -> '" + destinationOperation + "' on "
					+ this.resourcePath;
		log.debug(summary);

		/* *********************************************** */

		// Checking if the user is authorized

		boolean isAuthorized = false;
		log.debug("Asking for " + sourceOperation + " on " + this.resourcePath);
		try {
			isAuthorized = isUserAuthorizedFake(this.resourcePath,
					sourceOperation);
		} catch (ServletException e) {
			log.error("Unable to verify user authorization. ServletException : "
					+ e.getMessage());
			HTTPResponse.sendError(
					HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Error testing user authorization: " + e.getMessage());
			return;
		}

		if (destinationOperation != null) {
			log.debug("Asking for " + destinationOperation + " on "
					+ this.destinationPath);
			try {
				isAuthorized = isAuthorized
						&& isUserAuthorizedFake(this.resourcePath,
								sourceOperation);
			} catch (ServletException e) {
				log.error("Unable to verify user authorization. ServletException : "
						+ e.getMessage());
				HTTPResponse.sendError(
						HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error testing user authorization: " + e.getMessage());
				return;
			}
		}

		if (isAuthorized) {
			log.info("User is authorized to access the requested resource");
			chain.doFilter(request, response);
			return;
		} else {
			log.warn("User is not authorized to access the requested resource");
			HTTPResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
					"You are not authorized to access the requested resource");
			return;
		}

	}

	private boolean isUserAuthorizedFake(String path, String operation)
			throws ServletException, IllegalArgumentException {
		return true;
	}

	private boolean isUserAuthorized(String path, String operation)
			throws ServletException, IllegalArgumentException {
		if (path == null || operation == null) {
			log.error("Received null parameter(s) at isUserAuthorized: path="
					+ path + " operation=" + operation);
			throw new IllegalArgumentException("Received null parameter(s)");
		}
		URI uri = StormHttpsUtils.prepareURI(path, operation, this.subjectDN,
				this.fqans);
		log.debug("Authorization request uri = " + uri.toString());
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException "
					+ e.getLocalizedMessage());
			throw new ServletException(
					"Error contacting authorization service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException "
					+ e.getLocalizedMessage());
			throw new ServletException(
					"Error contacting authorization service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new ServletException(
					"Unexpected error! response.getStatusLine() returned null! Please contact storm support");
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
				log.error("unable to get the input content stream from server answer. IllegalStateException "
						+ e.getLocalizedMessage());
				throw new ServletException(
						"Error comunicationg with the authorization service.");
			} catch (IOException e) {
				log.error("unable to get the input content stream from server answer. IOException "
						+ e.getLocalizedMessage());
				throw new ServletException(
						"Error comunicationg with the authorization service.");
			}
			int l;
			byte[] tmp = new byte[512];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output = output + (new String(tmp, 0, l));
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException "
						+ e.getMessage());
				throw new ServletException(
						"Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new ServletException(
					"Unable to get a valid authorization response from the server.");
		}
		log.debug("Authorization response is : \'" + output + "\'");
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : \'"
					+ httpCode + "\' " + httpMessage);
			throw new ServletException(
					"Unable to get a valid response from server. Received a non HTTP 200 response from the server : \'"
							+ httpCode + "\' " + httpMessage);
		}
		Boolean response = new Boolean(output);
		log.debug("Authorization response (Boolean value): \'" + response
				+ "\'");
		return response.booleanValue();
	}

}