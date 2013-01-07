package it.grid.storm.gridhttps.webapp.webdav.factory.exceptions;

import io.milton.http.exceptions.BadRequestException;

public class RuntimeApiException extends BadRequestException {

	private static final long serialVersionUID = 3065886408319057340L;

	public RuntimeApiException(String reason) {
		super(reason);
	}

	public RuntimeApiException(String s, Throwable throwable) {
		super(s, throwable);
	}

}