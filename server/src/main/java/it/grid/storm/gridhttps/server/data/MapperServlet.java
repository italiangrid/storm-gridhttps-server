package it.grid.storm.gridhttps.server.data;

import it.grid.storm.gridhttps.server.DefaultConfiguration;
import it.grid.storm.gridhttps.server.exceptions.InitException;


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

	public void checkConfiguration() throws InitException {
		if (contextPath.isEmpty())
			throw new InitException("contextPath is empty!");
		if (contextSpec.isEmpty())
			throw new InitException("contextSpec is empty!");
	}
}