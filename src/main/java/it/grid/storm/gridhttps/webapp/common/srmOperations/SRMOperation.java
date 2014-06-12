package it.grid.storm.gridhttps.webapp.common.srmOperations;

import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.xmlrpc.BackendApi;

public interface SRMOperation {

	public Object executeAs(UserCredentials user, BackendApi backend)
		throws SRMOperationException;
}