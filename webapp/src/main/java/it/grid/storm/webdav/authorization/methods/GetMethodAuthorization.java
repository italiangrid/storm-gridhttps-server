package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import javax.servlet.ServletException;

public class GetMethodAuthorization extends AbstractMethodAuthorization {

	public GetMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		String path = getHttpHelper().getRequestURI().getPath();
		StorageArea reqStorageArea;
		try {
			reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		}
		String realPath = reqStorageArea.getRealPath(path);
		String operation = Constants.PREPARE_TO_GET_OPERATION;
		return askAuth(operation, realPath);
	}

}
