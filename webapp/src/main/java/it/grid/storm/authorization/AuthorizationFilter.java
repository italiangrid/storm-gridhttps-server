package it.grid.storm.authorization;

public abstract class AuthorizationFilter {
					
	public AuthorizationFilter() {
	}
		
	public abstract boolean isUserAuthorized() throws UnauthorizedException;
		
}
