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

public class MkDir implements SRMOperation{

	private static final Logger log = LoggerFactory.getLogger(MkDir.class);
	
	private Surl surl;
	
	public MkDir(Surl surl) {
		this.setSurl(surl);
	}
	
	public Surl getSurl() {
		return surl;
	}

	public void setSurl(Surl surl) {
		this.surl = surl;
	}
	
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		RequestOutputData output = null;
		log.debug("srmMkdir '{}' ...", this.getSurl());
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.mkdir(this.getSurl().toString());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.mkdir(user.getUserDN(), this.getSurl().toString());
			} else {
				output = backend.mkdir(user.getUserDN(), user.getUserFQANS(), this.getSurl().toString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		return output;
	}	
}