package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import javax.servlet.ServletException;

public class MoveMethodAuthorization extends AbstractMethodAuthorization {

	public MoveMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String destinationURL = httpHelper.getDestinationHeader();
		boolean hasDestination = destinationURL != null;
		if (!hasDestination) 
			return false;
		StorageArea reqStorageArea, destStorageArea;
		try {
			reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
			destStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getDestinationURI());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		}
		String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
		String destPath = destStorageArea.getRealPath(getHttpHelper().getDestinationURI().getPath());
		boolean response_cp_from = askAuth(Constants.MOVE_FROM_OPERATION, reqPath);
		boolean response_cp_to = askAuth(httpHelper.isOverwriteRequest() ? Constants.MOVE_TO_OVERWRITE_OPERATION : Constants.MOVE_TO_OPERATION, destPath);
		
		return response_cp_to && response_cp_from;
	}
	
}