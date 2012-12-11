package it.grid.storm.authorization;

import javax.servlet.ServletException;

public abstract class AuthorizationFilter {
				
	private String requestedURI;
	
	public AuthorizationFilter(String requestedURI) {
		this.setRequestedURI(requestedURI);
	}
	
	public abstract String stripContext();
	
	public abstract boolean isUserAuthorized() throws ServletException;
	
	public String getRequestedURI() {
		return requestedURI;
	}

	private void setRequestedURI(String requestedURI) {
		this.requestedURI = requestedURI;
	}

	
}
