package it.grid.storm;

import it.grid.storm.Configuration;
import it.grid.storm.authorization.AuthorizationFilter;
import it.grid.storm.authorization.AuthorizationStatus;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.StormAuthorizationUtils;
import it.grid.storm.authorization.UserCredentials;
import it.grid.storm.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.HttpHelper;
import it.grid.storm.webdav.authorization.WebDAVAuthorizationFilter;
import it.grid.storm.webdav.factory.StormResourceHelper;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.html.StormHtmlRootPage;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);

	private HttpHelper httpHelper;
	private UserCredentials user;

	public void destroy() {
	}
	
	public void init(FilterConfig fc) throws ServletException {
		Configuration.loadDefaultConfiguration();
		Configuration.initFromJSON(parse(fc.getInitParameter("params")));
		Configuration.print();
		if (!Configuration.isValid()) {
			log.error("Not a valid configuration!");
			throw new ServletException("Not a valid Configuration!");
		}
		/* Load Storage Area List */
		try {
			StorageAreaManager.init(Configuration.getBackendHostname(), Configuration.getBackendPort());
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		setHttpHelper(new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response));
		setUser(new UserCredentials(getHttpHelper()));
		initSession();
		String requestedPath = getHttpHelper().getRequestURI().getPath();
		log.debug("Requested-URI: " + requestedPath);

		if (isRootPath(requestedPath)) {
			log.debug("Requested-URI is root");
			processRootRequest();
		} else if (isFavicon(requestedPath)) {
			log.debug("Requested-URI is favicon");
			// implement getFavicon()
		} else {
			AuthorizationFilter filter = getAuthorizationHandler(requestedPath);
			if (filter != null) {
				AuthorizationStatus status = filter.isUserAuthorized();
				if (status.isAuthorized()) {
					log.info("User is authorized to access the requested resource");
					chain.doFilter(request, response);
				} else {
					log.warn("User is not authorized to access the requested resource");
					log.warn("Reason: " + status.getReason());
					sendError(HttpServletResponse.SC_UNAUTHORIZED, status.getReason());
				}
			} else {
				sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to identify the right handler to evaluate the requested path "
						+ requestedPath);
			}
		}
	}

	private JSONObject parse(String jsonText) throws ServletException {
		JSONObject params = null;		
		params = (JSONObject) JSONValue.parse(jsonText);
		if (params == null)
			throw new ServletException("Error on retrieving init parameters!");
		return params;
	}
	
	private void initSession() {
		getHttpHelper().getRequest().getSession(true);
		getHttpHelper().getRequest().getSession().setAttribute("forced", false);
	}

	private void processRootRequest() throws IOException {
		String method = getHttpHelper().getRequestMethod();
		if (method.equals("OPTIONS")) {
			doPing();
			sendDavHeader();
		} else if (method.equals("GET")) {
			sendRootPage();
		}
	}

	private AuthorizationFilter getAuthorizationHandler(String path) {
		try {
			if (isFileTransferRequest(path)) {
				log.info("Received a file-transfer request");
				return new FileTransferAuthorizationFilter(httpHelper, File.separator + Configuration.getFileTransferContextPath());
			} else {
				log.info("Received a webdav request");
				return new WebDAVAuthorizationFilter(httpHelper);
			}
		} catch (ServletException e) {
			log.error(e.getMessage());
		}
		return null;
	}

	private void sendDavHeader() throws IOException {
		httpHelper.getResponse().addHeader("DAV", "1");
		httpHelper.getResponse().flushBuffer();
	}

	private boolean isRootPath(String requestedPath) {
		return (requestedPath.isEmpty() || requestedPath.equals("/"));
	}

	private boolean isFavicon(String requestedPath) {
		return requestedPath.equals("/favicon.ico");
	}

	private boolean isFileTransferRequest(String requestedURI) {
		return requestedURI.startsWith(File.separator + Configuration.getFileTransferContextPath());
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			getHttpHelper().getResponse().sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void doPing() {
		// doPing
		log.info("ping " + Configuration.getBackendHostname() + ":" + Configuration.getBackendPort());
		try {
			PingOutputData output = StormResourceHelper.doPing(Configuration.getBackendHostname(), Configuration.getBackendPort(), getUser());
			log.info(output.getBeOs());
			log.info(output.getBeVersion());
			log.info(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
	}

	private void sendRootPage() throws IOException {
		OutputStream out = getHttpHelper().getResponse().getOutputStream();
		getHttpHelper().getResponse().addHeader("Content-Type", "text/html");
		getHttpHelper().getResponse().addHeader("DAV", "1");
		StormHtmlRootPage page = new StormHtmlRootPage(out);
		page.start();
		page.addTitle("StoRM Gridhttps-server WebDAV");
		page.addNavigator("/");
		List<StorageArea> sas = StorageAreaManager.getInstance().getStorageAreas();
		ListIterator<StorageArea> li = sas.listIterator();
		while (li.hasNext()) {
			StorageArea current = li.next();
			if (!hasStorageAreaAccess(current))
				li.remove();
		}
		page.addStorageAreaList(sas);
		page.end();
	}

	private boolean hasStorageAreaAccess(StorageArea sa) {
		return (sa.getProtocols().contains(getHttpHelper().getRequestProtocol()) && isUserAuthorized(sa));
	}

	private boolean isUserAuthorized(StorageArea sa) {
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(getUser(), Constants.PREPARE_TO_GET_OPERATION, sa.getFSRoot());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	public HttpHelper getHttpHelper() {
		return httpHelper;
	}

	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}

	public UserCredentials getUser() {
		return user;
	}

	private void setUser(UserCredentials user) {
		this.user = user;
	}
}