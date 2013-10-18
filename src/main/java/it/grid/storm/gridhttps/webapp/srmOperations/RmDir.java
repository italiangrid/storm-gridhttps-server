package it.grid.storm.gridhttps.webapp.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class RmDir implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Rm.class);
	
	private Surl surl;

	public RmDir(Surl surl) {
		
		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getName() + " constructor: null surl");
		
		this.setSurl(surl);
	}
		
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		RequestOutputData output = null;
		log.debug("delete '" + this.getSurl().asString() + "' ...");
		try {
			if (user.isAnonymous()) {
				output = backend.rmdirRecursively(this.getSurl().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.rmdirRecursively(user.getUserDN(), this.getSurl().asString());
			} else {
				output = backend.rmdirRecursively(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString());
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

	public Surl getSurl() {
		return surl;
	}

	private void setSurl(Surl surl) {
		this.surl = surl;
	}
	
}