package it.grid.storm.webdav.server;

import java.io.File;

import org.eclipse.jetty.webapp.WebAppContext;

public class WebDAVWebApp extends WebApp {
	
	public WebDAVWebApp(File baseDir) throws Exception {
		super("WebDAV-webapp", baseDir);
	}

	public WebAppContext getContext()  {
		WebAppContext context = new WebAppContext();
		context.setDescriptor(getDescriptorFile().toString());
		context.setResourceBase(getResourceBase().getAbsolutePath());
//		context.setContextPath("");
		context.setParentLoaderPriority(true);
		return context;
	}

}