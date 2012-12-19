package it.grid.storm.authorization;

public class AuthorizationStatus {
	
	private boolean authorized;
	private String reason;
	
	public AuthorizationStatus(boolean authorized, String reason) {
		super();
		this.setAuthorized(authorized);
		this.setReason(reason);
	}
	
	public boolean isAuthorized() {
		return authorized;
	}
	private void setAuthorized(boolean authorized) {
		this.authorized = authorized;
	}
	public String getReason() {
		return reason;
	}
	private void setReason(String reason) {
		this.reason = reason;
	}

}