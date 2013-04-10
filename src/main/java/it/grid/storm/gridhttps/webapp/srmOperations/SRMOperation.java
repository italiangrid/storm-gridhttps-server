package it.grid.storm.gridhttps.webapp.srmOperations;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.xmlrpc.BackendApi;

public interface SRMOperation {
	public Object executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException;
}