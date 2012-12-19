package it.grid.storm;

import io.milton.http.XmlWriter;
import it.grid.storm.Configuration;
import it.grid.storm.authorization.AuthorizationFilter;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.StormAuthorizationUtils;
import it.grid.storm.authorization.UnauthorizedException;
import it.grid.storm.authorization.UserCredentials;
import it.grid.storm.filetransfer.authorization.FileTransferAuthorizationFilter;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;
import it.grid.storm.HttpHelper;
import it.grid.storm.webdav.authorization.WebDAVAuthorizationFilter;
import it.grid.storm.webdav.factory.StormResourceHelper;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.IOException;
import java.io.OutputStream;

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

	public void destroy() {
	}

	public void init(FilterConfig fc) throws ServletException {
		try {
			Configuration.stormBackendHostname = fc.getInitParameter("stormBackendHostname");
			Configuration.stormBackendPort = Integer.valueOf(fc.getInitParameter("stormBackendPort"));
			Configuration.stormBackendServicePort = Integer.valueOf(fc.getInitParameter("stormBackendServicePort"));
			Configuration.stormFrontendHostname = fc.getInitParameter("stormFrontendHostname");
			Configuration.stormFrontendPort = Integer.valueOf(fc.getInitParameter("stormFrontendPort"));
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
		printConfiguration();
		/* Load Storage Area List */
		try {
			StorageAreaManager.init(Configuration.stormBackendHostname, Configuration.stormBackendServicePort);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException(e.getMessage());
		}
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		setHttpHelper(new HttpHelper((HttpServletRequest) request, (HttpServletResponse) response));
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
				boolean isAuthorized = false;
				String unauthMsg = "You are not authorized to access the requested resource";
				try {
					isAuthorized = filter.isUserAuthorized();
				} catch (UnauthorizedException e) {
					isAuthorized = false;
					unauthMsg = e.getMessage();
				}
				if (isAuthorized) {
					log.info("User is authorized to access the requested resource");
					chain.doFilter(request, response);
				} else {
					log.warn("User is not authorized to access the requested resource");
					sendError(HttpServletResponse.SC_UNAUTHORIZED, unauthMsg);
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
		String method = httpHelper.getRequestMethod();
		if (method.equals("OPTIONS")) {
			doPing();
			sendDavHeader();
			return;
		}
		if (method.equals("GET")) {
			sendRootPage();
			return;
		}
	}
	
	private AuthorizationFilter getAuthorizationHandler(String path) {
		try {
			if (isFileTransferRequest(path)) {
				log.info("Received a file-transfer request");
				return new FileTransferAuthorizationFilter(httpHelper, Configuration.FILETRANSFER_CONTEXTPATH);
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
		return requestedURI.startsWith(Configuration.FILETRANSFER_CONTEXTPATH);
	}

	private void sendError(int errorCode, String errorMessage) {
		try {
			httpHelper.getResponse().sendError(errorCode, errorMessage);
		} catch (IOException e) {
			log.error(e.getMessage());
			e.printStackTrace();
		}
	}

	private void printConfiguration() {
		log.debug("StoRM-Authorization-filter init-parameters' values:");
		log.debug(" - stormBackendHostname    : " + Configuration.stormBackendHostname);
		log.debug(" - stormBackendPort        : " + Configuration.stormBackendPort);
		log.debug(" - stormBackendServicePort : " + Configuration.stormBackendServicePort);
		log.debug(" - stormFrontendHostname   : " + Configuration.stormFrontendHostname);
		log.debug(" - stormFrontendPort       : " + Configuration.stormFrontendPort);
	}

	private void doPing() {
		// doPing
		log.info("ping " + Configuration.stormBackendHostname + ":" + Configuration.stormBackendPort);
		try {
			UserCredentials user = new UserCredentials(httpHelper);
			PingOutputData output = StormResourceHelper.doPing(Configuration.stormBackendHostname, Configuration.stormBackendPort, user);
			log.info(output.getBeOs());
			log.info(output.getBeVersion());
			log.info(output.getVersionInfo());
		} catch (RuntimeApiException e) {
			log.error(e.getMessage());
		}
	}

	private void sendRootPage() throws IOException {
		OutputStream out = httpHelper.getResponse().getOutputStream();
		// Content-Type: text/html; charset=iso-8859-1
		httpHelper.getResponse().addHeader("Content-Type", "text/html");
		httpHelper.getResponse().addHeader("DAV", "1");
		XmlWriter w = new XmlWriter(out);
		w.writeText("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">");
		w.begin("html").writeAtt("lang", "en").writeAtt("xmlns", "http://www.w3.org/1999/xhtml").open();
		w.open("head");
		w.begin("style").writeAtt("type", "text/css").open().writeText(getTableStyle()).close();
		w.close("head");
		w.open("body");
		w.begin("h1").open().writeText("StoRM Gridhttps-server WebDAV - /").close();
		w.open("table");
		w.open("tr");
		w.begin("td").open().begin("b").open().writeText("storage-area name").close().close();
		w.close("tr");
		UserCredentials user = new UserCredentials(httpHelper);
		for (StorageArea sa : StorageAreaManager.getInstance().getStorageAreas()) {
			if (!sa.getProtocols().contains(httpHelper.getRequestProtocol())) {
				continue;
			}
			if (!isUserAuthorized(user, sa)) {
				continue;
			}
			w.open("tr");
			w.open("td");
			String name = sa.getStfnRoot().substring(1);
			// entry name-link
			String path = buildHref(sa.getStfnRoot(), "");
			w.begin("img").writeAtt("alt", "").writeAtt("src", getFolderIco()).open().close();
			w.begin("a").writeAtt("href", path).open().writeText(name).close();
			w.close("td");
			w.close("tr");
		}
		w.close("table");
		w.close("body");
		w.close("html");
		w.flush();
	}

	private boolean isUserAuthorized(UserCredentials user, StorageArea sa) {
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, Constants.PREPARE_TO_GET_OPERATION, sa.getFSRoot());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return response;
	}

	private String getTableStyle() {
		String out = "table {width: 100%; font-family: Arial,\"Bitstream Vera Sans\",Helvetica,Verdana,sans-serif; color: #333;}";
		out += "table td, table th {color: #555;}";
		out += "table th {text-shadow: rgba(255, 255, 255, 0.796875) 0px 1px 0px; font-family: Georgia,\"Times New Roman\",\"Bitstream Charter\",Times,serif; font-weight: normal; padding: 7px 7px 8px; text-align: left; line-height: 1.3em; font-size: 14px;}";
		out += "table td {font-size: 12px; padding: 4px 7px 2px; vertical-align: top; }";
		out += "img {margin-right: 5px; margin-top: 0; vertical-align: bottom; width: 12px; }";
		return out;
	}

	private String getFolderIco() {
		String out = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAAGXcA1uAAAABGdBTUEAALGOfPtRkwAAACBjSFJNAAB6JQAAgIMAAPn/AACA6AAAdTAAAOpgAAA6lwAAF2+XqZnUAAACFklEQVR4nGL4//8/AwwDBBBDSkpKKRDvB2GAAAJxfIH4PwgDBBADsjKAAALJsMKUAQQQA0wJCAMEEIhTiCQwHYjngrQABBBIIgtZJQwDBBDYQCCjAIgbkLAmQACh2IiMAQII2cKtQDwbiDtAEgABBJNYB3MaFEcABBDMDgzLAQIIJKiPRaIOIIBgOpBd1AASAwggmEQ4SBU0tCJwuRSEAQIIpLgam91Y8CaQBoAAggVGCJGa/gMEEEhDCi4fYsGXAAII5odiIjVIAwQQTs/hwgABRLIGgAACOUcY6vEqIC4DYlF8GgACCJdnpwLxTGiimAPEFjANAAFEbOj8h2kACCCYhjwgTkdLUeh4A0gDQADBNGgRaVMpQACBNBwB4jYiNagDBBBIQwYQ3yLWHwABBNIgQIrHAQIIZ07Bgv+A1AIEEEyDUgpaZkHDebBgBQggkpMGQACRrIFUDBBAIOfLAvEbAv49B8Ss5FgAEEAgC+qIjQUi8F8gtke2ACCAQBZUICn4lwIpeX2AWAeINaC0FpQNwmpArIKEQRGoAMVyQCyJbAFAACGXFheAmAmIV1LRR+4AAQSyIBeIfwExHxDHUNFwEA4ACCCQBaBcPAWaXjdQ0fAfQMwCEEAgQxOB2AaIOYD4DxUtWAFyNEAAwSMDKBBJ5eAJAZkLEEDIFmgD8TcqGAxKqmUwcwECiOY5GSCAaG4BQIABAFbNMXYg1UnRAAAAAElFTkSuQmCC";
		return out;
	}

	private String buildHref(String uri, String name) {
		String abUrl = uri;
		if (!abUrl.endsWith("/")) {
			abUrl += "/";
		}
		return abUrl + name;
	}
	
	public HttpHelper getHttpHelper() {
		return httpHelper;
	}

	private void setHttpHelper(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
	}
}