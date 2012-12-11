package it.grid.storm.webdav.server.data;

import it.grid.storm.webdav.DefaultConfiguration;

public class MapperServlet {
	private String contextPath;
	private String contextSpec;

	public MapperServlet(String contextPath, String contextSpec) {
		this();
		this.setContextPath(contextPath);
		this.setContextSpec(contextSpec);
	}

	public MapperServlet() {
		this.setContextPath(DefaultConfiguration.MAPPER_SERVLET_CONTEXT_PATH);
		this.setContextSpec(DefaultConfiguration.MAPPER_SERVLET_CONTEXT_SPEC);
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getContextSpec() {
		return contextSpec;
	}

	public void setContextSpec(String contextSpec) {
		this.contextSpec = contextSpec;
	}

	public String toString() {
		return "{'" + contextPath + "', " + contextSpec + "}";
	}

	public void checkConfiguration() throws Exception {
		if (contextPath.isEmpty())
			throw new Exception("contextPath is empty!");
		if (contextSpec.isEmpty())
			throw new Exception("contextSpec is empty!");
	}
}