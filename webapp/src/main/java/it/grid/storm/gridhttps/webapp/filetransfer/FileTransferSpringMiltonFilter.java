package it.grid.storm.gridhttps.webapp.filetransfer;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.servlet.FilterConfigWrapper;
import io.milton.servlet.MiltonServlet;

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
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

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
public class FileTransferSpringMiltonFilter implements javax.servlet.Filter {

	private static final Logger log = LoggerFactory.getLogger(FileTransferSpringMiltonFilter.class);
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
		log.info("FileTransfer-SpringMiltonFilter init");
		this.filterConfig = fc;
		StaticApplicationContext parent = new StaticApplicationContext();
		FilterConfigWrapper configWrapper = new FilterConfigWrapper(this.filterConfig);
		parent.getBeanFactory().registerSingleton("config", configWrapper);
		parent.getBeanFactory().registerSingleton("servletContext", fc.getServletContext());
		File webRoot = new File(fc.getServletContext().getRealPath("/"));
		parent.getBeanFactory().registerSingleton("webRoot", webRoot);
		log.info("Registered root webapp path in: webroot=" + webRoot.getAbsolutePath());
		parent.refresh();
		context = new ClassPathXmlApplicationContext(new String[]{fc.getInitParameter("context.filename")}, parent);
		Object milton = context.getBean("milton.http.ftmanager");
		if (milton instanceof HttpManager) {
			this.httpManager = (HttpManager) milton;
		} else if (milton instanceof HttpManagerBuilder) {
			HttpManagerBuilder builder = (HttpManagerBuilder) milton;
			this.httpManager = builder.buildHttpManager();
		}
		servletContext = fc.getServletContext();
		System.out.println("servletContext: " + servletContext.getClass());
		String sExcludePaths = fc.getInitParameter("milton.exclude.paths");
		log.info("init: exclude paths: " + sExcludePaths);
		excludeMiltonPaths = sExcludePaths.split(",");
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
