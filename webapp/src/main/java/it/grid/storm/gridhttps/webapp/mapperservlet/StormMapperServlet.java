package it.grid.storm.gridhttps.webapp.mapperservlet;

import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

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

public class StormMapperServlet implements Filter {

	private static final Logger log = LoggerFactory.getLogger(StormMapperServlet.class);
	private static final String PATH_PARAMETER_KEY = "path";
	private static final String MAPPER_SERVLET_ENCODING_SCHEME = "UTF-8";
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		log.debug("Serving a mapping request");
		String pathDecoded = getDecodedPath((HttpServletRequest) request);
		log.debug("Decoded filePath = " + pathDecoded + " . Retrieving matching StorageArea");
		StorageArea SA = getMatchingSA(pathDecoded);
		log.debug("Storage-Area: " + SA);
		String relativeUrl = File.separator + Configuration.getFileTransferContextPath() + SA.getStfn(pathDecoded);
		log.info("MAPPING: '" + pathDecoded + "' to '" + relativeUrl + "'");
		sendResponse((HttpServletResponse) response, relativeUrl);
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		
	}
	
	private String getDecodedPath(HttpServletRequest request) throws ServletException {
		String path = request.getParameter(PATH_PARAMETER_KEY);
		String pathDecoded;
		try {
			pathDecoded = URLDecoder.decode(path, MAPPER_SERVLET_ENCODING_SCHEME);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to decode " + PATH_PARAMETER_KEY + " parameter. UnsupportedEncodingException : " + e.getMessage());
			throw new ServletException("Unable to decode " + PATH_PARAMETER_KEY + " parameter", e);
		}
		return pathDecoded;
	}
	
	private StorageArea getMatchingSA(String pathDecoded) throws ServletException {
		StorageArea SA = null;
		try {
			SA = StorageAreaManager.getMatchingSA(new File(pathDecoded));
		} catch (IllegalArgumentException e) {
			log.error("Unable to get matching SA for path " + pathDecoded + ". IllegalArgumentException : " + e.getMessage());
			throw new ServletException("Unable to get matching SA for path " + pathDecoded, e);
		} catch (IllegalStateException e) {
			log.error("Unable to get matching SA for path " + pathDecoded + ". IllegalStateException : " + e.getMessage());
			throw new ServletException("Unable to get matching SA for path " + pathDecoded, e);
		}
		if (SA == null) {
			log.error("No matching StorageArea found for path \'" + pathDecoded + "\' Unable to build http(s) relative path");
			throw new ServletException("No matching StorageArea found for the provided path");
		}
		return SA;
	}
	
	private void sendResponse(HttpServletResponse response, String message) throws IOException {
		response.setContentType("text/html");
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (IOException e) {
			log.error("Unable to obtain the PrintWriter for the response. IOException: " + e.getMessage());
			throw e;
		}
		out.print(message);
	}

	
}