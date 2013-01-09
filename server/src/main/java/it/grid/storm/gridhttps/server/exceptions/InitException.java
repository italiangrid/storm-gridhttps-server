package it.grid.storm.gridhttps.server.exceptions;

public class InitException extends Exception {

	public InitException(Exception e) {
		super(e);
	}
	
	public InitException(String msg) {
		super(msg);
	}

	private static final long serialVersionUID = -6305462430932044991L;
	
}