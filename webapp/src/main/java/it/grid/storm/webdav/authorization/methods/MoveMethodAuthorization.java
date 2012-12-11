package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class MoveMethodAuthorization extends AbstractMethodAuthorization {

	public MoveMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String destinationURL = getDestinationHeader();
		boolean hasDestination = destinationURL != null;
		if (!hasDestination) 
			return false;
		String operation = Constants.MOVE_FROM_OPERATION;
		String path = getResourcePath();
		boolean response_mv_from = askAuth(operation, path);
		operation = isOverwriteRequest() ? Constants.MOVE_TO_OVERWRITE_OPERATION : Constants.MOVE_TO_OPERATION;
		boolean response_mv_to = false;
		if (isLocalDestination()) {
			path = convertToStorageAreaPath(fromStringToURI(destinationURL).getPath());
			response_mv_to = askAuth(operation, path);
		} else {
			response_mv_to = true;
		}
		return response_mv_to && response_mv_from;
	}

	public String getDestinationHeader() {
		return this.HTTPRequest.getHeader("Destination");
	}
	
	public String getOverwriteHeader() {
		return this.HTTPRequest.getHeader("Overwrite");
	}
	
	public boolean isOverwriteRequest() {
		String overwrite = getOverwriteHeader();
		return ((overwrite != null) && (overwrite.equals("T")));
	}
	
	private boolean isLocalDestination() throws ServletException {
		String url = getDestinationHeader();
		String destHostname = fromStringToURI(url).getHost();
		int destPort = fromStringToURI(url).getPort();
		boolean sameHost = this.HTTPRequest.getLocalName().equals(destHostname);
		boolean samePort = this.HTTPRequest.getLocalPort() == destPort;
		return sameHost && samePort;
	}
	
}