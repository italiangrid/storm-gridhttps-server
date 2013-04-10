package it.grid.storm.gridhttps.webapp.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class Move implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Move.class);
	
	private Surl source;
	private Surl destination;

	public Move(Surl source, Surl destination) {
		this.setSource(source);
		this.setDestination(destination);
	}
	
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException {
		RequestOutputData output = null;
		log.debug("move '" + this.getSource().asString() + "' to '" + this.getDestination().asString() + "' ...");
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.mv(this.getSource().asString(), this.getDestination().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.mv(user.getUserDN(), this.getSource().asString(), this.getDestination().asString());
			} else {
				output = backend.mv(user.getUserDN(), user.getUserFQANS(), this.getSource().asString(), this.getDestination().asString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormRequestFailureException(output.getStatus());
		}
		return output;
	}

	public Surl getSource() {
		return source;
	}

	private void setSource(Surl source) {
		this.source = source;
	}

	public Surl getDestination() {
		return destination;
	}

	private void setDestination(Surl destination) {
		this.destination = destination;
	}
	
}