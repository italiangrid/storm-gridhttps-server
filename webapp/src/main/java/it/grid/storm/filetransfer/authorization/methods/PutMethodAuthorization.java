package it.grid.storm.filetransfer.authorization.methods;

import java.io.UnsupportedEncodingException;

import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

import javax.servlet.ServletException;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	public PutMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	@Override
	public boolean isUserAuthorized() throws ServletException {
		StorageArea reqStorageArea;
		try {
			reqStorageArea = StorageAreaManager.getMatchingSAbyURI(getHttpHelper().getRequestStringURI());
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
		String operation = getHttpHelper().isOverwriteRequest() ? Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION : Constants.PREPARE_TO_PUT_OPERATION;
		return askAuth(operation, reqPath);
	}
}