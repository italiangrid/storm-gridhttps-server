package it.grid.storm.gridhttps.webapp.filetransfer.authorization;

public class InvalidTURLException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3622954068514419785L;
	
	private int errorcode;
	private String msg;
	
	public InvalidTURLException(int error, String msg) {
		this.errorcode = error;
		this.msg = msg;
	}

	
	/**
	 * @return the errorcode
	 */
	public int getErrorcode() {
	
		return errorcode;
	}

	/**
	 * @return the msg
	 */
	public String getMessage() {
	
		return msg;
	}
}