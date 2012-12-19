package it.grid.storm.filetransfer.authorization.methods;

import it.grid.storm.Configuration;
import it.grid.storm.HttpHelper;
import it.grid.storm.authorization.Constants;
import it.grid.storm.authorization.UnauthorizedException;
import it.grid.storm.authorization.methods.AbstractMethodAuthorization;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

public class PutMethodAuthorization extends AbstractMethodAuthorization {

	public PutMethodAuthorization(HttpHelper httpHelper) {
		super(httpHelper);
	}

	private String stripContext(String url) {
		return url.replaceFirst(Configuration.FILETRANSFER_CONTEXTPATH, "");
	}
	
	@Override
	public boolean isUserAuthorized() throws UnauthorizedException {
		StorageArea reqStorageArea;
		String path = stripContext(getHttpHelper().getRequestStringURI());
		try {
			reqStorageArea = StorageAreaManager.getMatchingSA(getHttpHelper().getRequestURI());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			return false;
		} catch (IllegalStateException e) {
			e.printStackTrace();
			return false;
		}
		String reqPath = reqStorageArea.getRealPath(path);
//		String operation = getHttpHelper().isOverwriteRequest() ? Constants.PREPARE_TO_PUT_OVERWRITE_OPERATION : Constants.PREPARE_TO_PUT_OPERATION;
		return askAuth(Constants.WRITE_OPERATION, reqPath);
	}
}
