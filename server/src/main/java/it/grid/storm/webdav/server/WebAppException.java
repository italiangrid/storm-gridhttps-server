package it.grid.storm.webdav.server;

public class WebAppException extends Exception {

	private static final long serialVersionUID = 1L;

	public WebAppException(String description) {
		super(description);
	}
}