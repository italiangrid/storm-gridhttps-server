package it.grid.storm.webdav.server;

public class ServerException extends Exception {

	private static final long serialVersionUID = 1L;

	public ServerException(String description) {
		super(description);
	}
}