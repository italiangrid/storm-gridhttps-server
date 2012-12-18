package it.grid.storm.webdav.authorization.methods;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import javax.servlet.ServletException;

public class DeleteMethodAuthorization extends AbstractMethodAuthorization {
	
	public DeleteMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
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
		String reqPath = reqStorageArea.getRealPath(getHttpHelper().getRequestURI().getPath());
		String operation = Constants.RM_OPERATION;
		return askAuth(operation, reqPath);
	}
	
}