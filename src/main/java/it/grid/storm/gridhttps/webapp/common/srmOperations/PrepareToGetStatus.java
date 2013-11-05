package it.grid.storm.gridhttps.webapp.common.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException.TSRMExceptionReason;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PrepareToGetStatus implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToGetStatus.class);

	private Surl surl;

	public PrepareToGetStatus(Surl surl) {
		
		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl");
		
		this.setSurl(surl);
	}
	
	public Surl getSurl() {
		return surl;
	}

	private void setSurl(Surl surl) {
		this.surl = surl;
	}
	
	@Override
	public SurlArrayRequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		SurlArrayRequestOutputData outputSPtG = null;
		log.debug("status of prepare to get on '" + this.getSurl().asString() + "' ...");
		try {
			if (user.isAnonymous()) {
				outputSPtG = backend.prepareToGetStatus(this.getSurl().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				outputSPtG = backend.prepareToGetStatus(user.getUserDN(), this.getSurl().asString());
			} else {
				outputSPtG = backend.prepareToGetStatus(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			TReturnStatus status = new TReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, e.toString());
			throw new SRMOperationException(status, TSRMExceptionReason.INTERNALERROR);
		}
		log.debug(outputSPtG.getStatus().getStatusCode().getValue());
		log.debug(outputSPtG.getStatus().getExplanation());
		return outputSPtG;
	}
	
}