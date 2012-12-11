package it.grid.storm.webdav.server;

import java.io.File;

import org.eclipse.jetty.webapp.WebAppContext;

public class WebApp {

	private File resourceBase;
	private File descriptorFile;
	private String name;
	
	public WebApp(String name, File resourceBase) {
		this.setName(name);
		this.setResourceBase(resourceBase);
		this.setDescriptorFile(new File(resourceBase, "/WEB-INF/web.xml"));
	}
	
	public WebAppContext getContext()  {
		WebAppContext context = new WebAppContext();
		context.setDescriptor(getDescriptorFile().toString());
		context.setResourceBase(getResourceBase().getAbsolutePath());
//		context.setContextPath("");
		context.setParentLoaderPriority(true);
		return context;
	}
	
	public File getDescriptorFile() {
		return descriptorFile;
	}

	private void setDescriptorFile(File descriptorFile) {
		this.descriptorFile = descriptorFile;
	}

	public String getName() {
		return name;
	}
	
	private void setName(String name) {
		this.name = name;
	}
	

	public File getResourceBase() {
		return resourceBase;
	}

	private void setResourceBase(File resourceBase) {
		this.resourceBase = resourceBase;
	}
	
}