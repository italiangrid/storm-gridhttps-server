package it.grid.storm.webdav;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.servlet.SpringMiltonFilter;
import it.grid.storm.webdav.authorization.StormHttpsAuthorization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

public class StormWebdavFilter implements Filter {

	private static final Logger log = LoggerFactory
			.getLogger(StormWebdavFilter.class);

	private SpringMiltonFilter miltonFilter;
	private String rootPath;
	private String contextPath;

	public void destroy() {
		miltonFilter.destroy();

	}

	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain fc) throws IOException, ServletException {
		if(StormHttpsAuthorization.isUserAuthorized(req, resp)) miltonFilter.doFilter(req, resp, fc);
	}

	public void init(FilterConfig fc) throws ServletException {
		miltonFilter = new SpringMiltonFilter();		
		miltonFilter.init(fc);
		setDinamicPaths();
	}

	private void setDinamicPaths() {
		StaticApplicationContext parent = new StaticApplicationContext();
		parent.refresh();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				new String[] { "applicationContext.xml" }, parent);
		Object beanFactory;
		
		if (context.containsBean("milton.fs.resource.factory")) {
			beanFactory = context.getBean("milton.fs.resource.factory");
			if (beanFactory instanceof FileSystemResourceFactory) {
				this.rootPath = ((FileSystemResourceFactory) beanFactory)
						.getRoot().getAbsolutePath();
				this.contextPath = ((FileSystemResourceFactory) beanFactory)
						.getContextPath();
				log.info("rootPath: " + this.rootPath);
				log.info("contextPath: " + this.contextPath);
			}
		}
	}

}