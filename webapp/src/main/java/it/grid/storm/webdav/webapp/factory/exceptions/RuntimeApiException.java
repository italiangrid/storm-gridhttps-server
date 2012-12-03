package it.grid.storm.webdav.webapp.factory.exceptions;

public class RuntimeApiException extends RuntimeException {

	private static final long serialVersionUID = 3065886408319057340L;

	public RuntimeApiException() {
		super();
	}

	public RuntimeApiException(String s) {
		super(s);
	}

	public RuntimeApiException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public RuntimeApiException(Throwable throwable) {
		super(throwable);
	}

}