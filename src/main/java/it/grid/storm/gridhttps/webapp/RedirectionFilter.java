package it.grid.storm.gridhttps.webapp;

import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import java.io.File;
import java.io.IOException;

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


public class RedirectionFilter implements Filter {

	private static final Logger log = LoggerFactory.getLogger(RedirectionFilter.class);
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		log.debug(this.getClass().getName() + " init");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
		FilterChain chain) throws IOException, ServletException {

		log.debug("RedirectionFilter - doFilter");
		
		HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

  	StorageArea SA = StorageAreaManager.getMatchingSA(req.getRequestURI());
  	if (SA != null || isRootPath(req.getRequestURI())) {
  		log.info("Try redirecting to '" + "/webdav" + req.getRequestURI() + "'");
  		res.sendRedirect("/webdav" + req.getRequestURI());
  	} else {
  		chain.doFilter(request, response);
    }
		
	}

	@Override
	public void destroy() {
	}

	private boolean isRootPath(String requestedPath) {
		return requestedPath.isEmpty() || requestedPath.equals(File.separator);
	}
	
}