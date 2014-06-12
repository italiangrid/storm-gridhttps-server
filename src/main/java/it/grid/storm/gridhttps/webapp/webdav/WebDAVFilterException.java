package it.grid.storm.gridhttps.webapp.webdav;

public class WebDAVFilterException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3622954068514419785L;
	
	private int errorcode;
	private String msg;
	
	public WebDAVFilterException(int error, String msg) {
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