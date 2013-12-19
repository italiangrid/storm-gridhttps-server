package it.grid.storm.gridhttps.webapp.common.exceptions;

public class InternalErrorException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3622954068514419785L;
	
	private String msg;
	
	public InternalErrorException(String msg) {
		this.msg = msg;
	}

	public InternalErrorException(Exception e) {
		this(e.getLocalizedMessage());
	}

	/**
	 * @return the msg
	 */
	public String getMessage() {
	
		return msg;
	}
}