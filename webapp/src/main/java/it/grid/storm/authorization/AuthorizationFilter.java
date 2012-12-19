package it.grid.storm.authorization;

public abstract class AuthorizationFilter {
	
	public abstract AuthorizationStatus isUserAuthorized();
		
}
