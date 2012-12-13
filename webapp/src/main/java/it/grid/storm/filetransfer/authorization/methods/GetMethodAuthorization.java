package it.grid.storm.filetransfer.authorization.methods;

import java.io.UnsupportedEncodingException;

import it.grid.storm.Configuration;
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

	private String stripContext(String url) {
		return url.replaceFirst(Configuration.FILETRANSFER_CONTEXTPATH, "");
	}
	
	@Override
	public boolean isUserAuthorized() throws ServletException {
		StorageArea reqStorageArea;
		String path = stripContext(getHttpHelper().getRequestStringURI());
		try {
			reqStorageArea = StorageAreaManager.getMatchingSAbyURI(path);
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
		String reqPath = reqStorageArea.getRealPath(path);
		String operation = Constants.PREPARE_TO_GET_OPERATION;
		return askAuth(operation, reqPath);
	}

}