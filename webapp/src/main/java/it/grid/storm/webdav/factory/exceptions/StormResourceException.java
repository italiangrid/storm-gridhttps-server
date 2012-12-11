package it.grid.storm.webdav.factory.exceptions;

import io.milton.http.exceptions.BadRequestException;

public class StormResourceException extends BadRequestException {

	private static final long serialVersionUID = 1200998154780371147L;

	public StormResourceException(String reason) {
		super(reason);
	}

	public StormResourceException(String s, Throwable throwable) {
		super(s, throwable);
	}

}