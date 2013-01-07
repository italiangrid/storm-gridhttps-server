package it.grid.storm.gridhttps.server;

import it.grid.storm.gridhttps.server.utils.WebNamespaceContext;
import it.grid.storm.gridhttps.server.utils.XML;

import java.io.File;
import java.util.Map;

import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.webapp.WebAppContext;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class WebApp {

	private File resourceBase;
	private File descriptorFile;
	
	public WebApp(File resourceBase) {
		this.setResourceBase(resourceBase);
		this.setDescriptorFile(new File(resourceBase, "/WEB-INF/web.xml"));
	}
	
	public WebAppContext getContext()  {
		WebAppContext context = new WebAppContext();
		context.setDescriptor(getDescriptorFile().toString());
		context.setResourceBase(getResourceBase().getAbsolutePath());
		context.setParentLoaderPriority(true);
		return context;
	}

	public void configureDescriptor(Map<String,String> params) throws Exception {
		XML doc = new XML(descriptorFile);
		String query = "/j2ee:web-app/j2ee:filter[@id='stormAuthorizationFilter']/j2ee:init-param/j2ee:param-value";
		NodeList initParams = doc.getNodes(query, new WebNamespaceContext(null));
		((Element) initParams.item(0)).setTextContent(JSON.toString(params));
		doc.save();
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