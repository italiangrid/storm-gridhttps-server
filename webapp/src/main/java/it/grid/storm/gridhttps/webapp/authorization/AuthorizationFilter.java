package it.grid.storm.gridhttps.webapp.authorization;

public abstract class AuthorizationFilter {
	
	public abstract AuthorizationStatus isUserAuthorized();
		
}
