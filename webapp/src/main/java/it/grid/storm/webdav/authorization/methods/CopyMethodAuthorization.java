package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class CopyMethodAuthorization extends AbstractMethodAuthorization {

	public CopyMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String destinationURL = getDestinationHeader();
		boolean hasDestination = destinationURL != null;
		if (!hasDestination) 
			return false;
		String operation = Constants.CP_FROM_OPERATION;
		String path = getResourcePath();
		boolean response_cp_from = askAuth(operation, path);
		operation = isOverwriteRequest() ? Constants.CP_TO_OVERWRITE_OPERATION : Constants.CP_TO_OPERATION;
		boolean response_cp_to = false;
		if (isLocalDestination()) {
			path = convertToStorageAreaPath(fromStringToURI(destinationURL).getPath());
			response_cp_to = askAuth(operation, path);
		} else {
			response_cp_to = true;
		}
		return response_cp_to && response_cp_from;
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