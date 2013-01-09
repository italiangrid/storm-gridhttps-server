package it.grid.storm.gridhttps.server;

import java.io.File;

public class WebApp {

	private File resourceBase;
	private File descriptorFile;
	
	public WebApp(File resourceBase) {
		this.setResourceBase(resourceBase);
		this.setDescriptorFile(new File(resourceBase, "/WEB-INF/web.xml"));
	}
	
	public File getDescriptorFile() {
		return descriptorFile;
	}

	private void setDescriptorFile(File descriptorFile) {
		this.descriptorFile = descriptorFile;
	}

	public File getResourceBase() {
		return resourceBase;
	}

	private void setResourceBase(File resourceBase) {
		this.resourceBase = resourceBase;
	}
	
}