/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.webdav;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.configuration.Configuration;

import java.io.File;
import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Loads the spring context from classpath at applicationContext.xml
 *
 * This filter then gets the bean named milton.http.manager and uses that for
 * milton processing.
 *
 * The milton.http.manager bean can either be a HttpManager or it can be a
 * HttpManagerBuilder, in which case a HttpManager is constructed from it
 *
 * Requests with a path which begins with one of the exclude paths will not be
 * processed by milton. Instead, for these requests, the filter chain will be
 * invoked so the request can be serviced by JSP or a servlet, etc
 *
 * This uses an init parameter called milton.exclude.paths, which should be a
 * comma seperated list of paths to ignore. For example:
 * /static,/images,/login.jsp
 *
 * This allows non-milton resources to be accessed, while still mapping all urls
 * to milton
 *
 * @author bradm
 */
public class WebdavSpringMiltonFilter implements javax.servlet.Filter {

	private static final Logger log = LoggerFactory.getLogger(WebdavSpringMiltonFilter.class);
	private ClassPathXmlApplicationContext context;
	private HttpManager httpManager;
	private FilterConfig filterConfig;
	private ServletContext servletContext;
	/**
	 * Resources with this as the first part of their path will not be served
	 * from milton. Instead, this filter will allow filter processing to
	 * continue so they will be served by JSP or a servlet
	 */
	private String[] excludeMiltonPaths;

	public void init(FilterConfig fc) throws ServletException {
		log.info("WebDAV-filter init");
		filterConfig = fc;
		ApplicationContext context;
		try {
			context = new ClassPathXmlApplicationContext(new String[]{filterConfig.getInitParameter("contextConfigLocation")});
		} catch (BeansException e) {
			log.error(e.getMessage());
			e.printStackTrace();
			return;
		}
		Object milton = context.getBean("milton.http.manager");
		if (milton instanceof HttpManager) {
			this.httpManager = (HttpManager) milton;
		} else if (milton instanceof HttpManagerBuilder) {
			HttpManagerBuilder builder = (HttpManagerBuilder) milton;
			this.httpManager = builder.buildHttpManager();
		}
		servletContext = filterConfig.getServletContext();
		log.debug("servletContext: " + servletContext.getClass());
		
		String mappingContextPath = File.separator + Configuration.getGridhttpsInfo().getMapperServlet().getContextPath() + File.separator
				+ Configuration.getGridhttpsInfo().getMapperServlet().getContextSpec();
		String fileTransferContextPath = File.separator + Configuration.getGridhttpsInfo().getFiletransferContextPath();
		String[] davExcluded = { "/index.jsp", fileTransferContextPath, mappingContextPath };
		excludeMiltonPaths = davExcluded;
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain fc) throws IOException, ServletException {
		if (req instanceof HttpServletRequest) {
			HttpServletRequest hsr = (HttpServletRequest) req;
			String url = hsr.getRequestURI();
			// Allow certain paths to be excluded from milton, these might be other servlets, for example
			for (String s : excludeMiltonPaths) {
				if (url.startsWith(s)) {
					fc.doFilter(req, resp);
					return;
				}
			}
			doMiltonProcessing((HttpServletRequest) req, (HttpServletResponse) resp);
		} else {
			fc.doFilter(req, resp);
			return;
		}
	}

	public void destroy() {
		context.close();
		if (httpManager != null) {
			httpManager.shutdown();
		}
	}

	private void doMiltonProcessing(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			MiltonServlet.setThreadlocals(req, resp);
			Request request = new io.milton.servlet.ServletRequest(req, servletContext);
			Response response = new io.milton.servlet.ServletResponse(resp);
			httpManager.process(request, response);
		} finally {
			MiltonServlet.clearThreadlocals();
			resp.getOutputStream().flush();
			resp.flushBuffer();
		}
	}
}
