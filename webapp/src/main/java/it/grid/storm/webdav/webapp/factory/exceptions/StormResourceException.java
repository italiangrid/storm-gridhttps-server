package it.grid.storm.webdav.webapp.factory.exceptions;

public class StormResourceException extends RuntimeException {

	private static final long serialVersionUID = 1200998154780371147L;

	public StormResourceException() {
		super();
	}

	public StormResourceException(String s) {
		super(s);
	}

	public StormResourceException(String s, Throwable throwable) {
		super(s, throwable);
	}

	public StormResourceException(Throwable throwable) {
		super(throwable);
	}

}