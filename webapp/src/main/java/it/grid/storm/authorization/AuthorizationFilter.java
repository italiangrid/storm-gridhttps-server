package it.grid.storm.authorization;

import javax.servlet.ServletException;

public abstract class AuthorizationFilter {
					
	public AuthorizationFilter() {
	}
		
	public abstract boolean isUserAuthorized() throws ServletException;
		
}
