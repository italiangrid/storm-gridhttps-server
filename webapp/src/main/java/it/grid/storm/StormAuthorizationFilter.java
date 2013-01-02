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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormAuthorizationFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(StormAuthorizationFilter.class);
	
	private HttpHelper httpHelper;
	private UserCredentials user;

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			Configuration.BACKEND_HOSTNAME = fc.getInitParameter("stormBackendHostname");
			Configuration.BACKEND_PORT = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
			Configuration.BACKEND_SERVICE_PORT = Integer.valueOf(fc.getInitParameter("stormBackendServicePort"));
			Configuration.FRONTEND_HOSTNAME = fc.getInitParameter("stormFrontendHostname");
			Configuration.FRONTEND_PORT = Integer.valueOf(fc.getInitParameter("stormFrontendPort"));
			Configuration.GPFS_ROOT_DIRECTORY = fc.getInitParameter("rootDirectory");
			Configuration.WEBDAV_CONTEXT_PATH = fc.getInitParameter("webdavContextPath");
			Configuration.FILETRANSFER_CONTEXTPATH = fc.getInitParameter("fileTransferContextPath");
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
		printConfiguration();
		/* Load Storage Area List */
		try {
			StorageAreaManager.init(Configuration.BACKEND_HOSTNAME, Configuration.BACKEND_SERVICE_PORT);
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
				sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to identify the right handler to evaluate the requested path " + requestedPath);
			}
		}
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
				return new FileTransferAuthorizationFilter(httpHelper, File.separator + Configuration.FILETRANSFER_CONTEXTPATH);
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
		return requestedURI.startsWith(File.separator + Configuration.FILETRANSFER_CONTEXTPATH);
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			getHttpHelper().getResponse().sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void printConfiguration() {
		log.debug("StoRM-Authorization-filter init-parameters' values:");
		log.debug(" - stormBackendHostname    : " + Configuration.BACKEND_HOSTNAME);
		log.debug(" - stormBackendPort        : " + Configuration.BACKEND_PORT);
		log.debug(" - stormBackendServicePort : " + Configuration.BACKEND_SERVICE_PORT);
		log.debug(" - stormFrontendHostname   : " + Configuration.FRONTEND_HOSTNAME);
		log.debug(" - stormFrontendPort       : " + Configuration.FRONTEND_PORT);
	}

	private void doPing() {
		// doPing
		log.info("ping " + Configuration.BACKEND_HOSTNAME + ":" + Configuration.BACKEND_PORT);
		try {
			PingOutputData output = StormResourceHelper.doPing(Configuration.BACKEND_HOSTNAME, Configuration.BACKEND_PORT, getUser());
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