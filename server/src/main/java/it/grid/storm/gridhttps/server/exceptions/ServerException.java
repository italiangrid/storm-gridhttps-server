package it.grid.storm.gridhttps.server.exceptions;

public class ServerException extends Exception {

	public ServerException(Exception e) {
		super(e);
	}
	
	public ServerException(String msg) {
		super(msg);
	}

	private static final long serialVersionUID = -6305462430932044991L;
	
}