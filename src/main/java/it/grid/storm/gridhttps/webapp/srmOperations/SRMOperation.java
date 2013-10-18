package it.grid.storm.gridhttps.webapp.srmOperations;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.exceptions.SRMOperationException;
import it.grid.storm.xmlrpc.BackendApi;

public interface SRMOperation {
	public Object executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException;
}