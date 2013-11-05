package it.grid.storm.gridhttps.webapp.common.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class Move implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Move.class);
	
	private Surl source;
	private Surl destination;

	public Move(Surl source, Surl destination) {
		
		if (source == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null source surl");
		if (destination == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null destination surl");
		
		this.setSource(source);
		this.setDestination(destination);
	}
	
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		RequestOutputData output = null;
		log.debug("move '" + this.getSource().asString() + "' to '" + this.getDestination().asString() + "' ...");
		try {
			if (user.isAnonymous()) {
				output = backend.mv(this.getSource().asString(), this.getDestination().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.mv(user.getUserDN(), this.getSource().asString(), this.getDestination().asString());
			} else {
				output = backend.mv(user.getUserDN(), user.getUserFQANS(), this.getSource().asString(), this.getDestination().asString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			TReturnStatus status = new TReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, e.toString());
			throw new SRMOperationException(status, e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
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