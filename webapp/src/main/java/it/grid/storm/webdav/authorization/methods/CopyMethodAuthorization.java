package it.grid.storm.webdav.authorization.methods;

import java.io.UnsupportedEncodingException;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import javax.servlet.ServletException;

public class CopyMethodAuthorization extends AbstractMethodAuthorization {

	public CopyMethodAuthorization(HttpHelper httpHelper) {
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
			reqStorageArea = StorageAreaManager.getMatchingSAbyURI(getHttpHelper().getRequestStringURI());
			destStorageArea = StorageAreaManager.getMatchingSAbyURI(getHttpHelper().getDestinationHeader());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
		String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
		String destPath = destStorageArea.getRealPath(getHttpHelper().getDestinationURI().getPath());
		boolean response_cp_from = askAuth(Constants.CP_FROM_OPERATION, reqPath);
		boolean response_cp_to = askAuth(httpHelper.isOverwriteRequest() ? Constants.CP_TO_OVERWRITE_OPERATION : Constants.CP_TO_OPERATION, destPath);
		
		return response_cp_to && response_cp_from;
	}

}