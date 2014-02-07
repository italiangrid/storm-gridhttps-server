package it.grid.storm.gridhttps.webapp.common.exceptions;

public class PreconditionFailedException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3622954068514419785L;
	
	private String msg;
	
	public PreconditionFailedException(String msg) {
		this.msg = msg;
	}

	/**
	 * @return the msg
	 */
	public String getMessage() {
	
		return msg;
	}
}