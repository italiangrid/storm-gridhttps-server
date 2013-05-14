package it.grid.storm.gridhttps.webapp.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PrepareToGetStatus implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToGetStatus.class);

	private Surl surl;

	public PrepareToGetStatus(Surl surl) {
		this.setSurl(surl);
	}
	
	public Surl getSurl() {
		return surl;
	}

	private void setSurl(Surl surl) {
		this.surl = surl;
	}
	
	@Override
	public SurlArrayRequestOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException {
		SurlArrayRequestOutputData outputSPtG = null;
		log.debug("status of prepare to get on '" + this.getSurl().asString() + "' ...");
		try {
			if (user.isAnonymous()) { // HTTP
				outputSPtG = backend.prepareToGetStatus(this.getSurl().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				outputSPtG = backend.prepareToGetStatus(user.getUserDN(), this.getSurl().asString());
			} else {
				outputSPtG = backend.prepareToGetStatus(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputSPtG.getStatus().getStatusCode().getValue());
		log.debug(outputSPtG.getStatus().getExplanation());
		if (!outputSPtG.getStatus().getStatusCode().getValue().equals("SRM_SUCCESS")) {
			throw new StormRequestFailureException(outputSPtG.getStatus());
		}
		return outputSPtG;
	}
	
}