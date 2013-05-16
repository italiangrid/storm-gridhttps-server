package it.grid.storm.gridhttps.webapp.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PrepareToPutStatus implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToPutStatus.class);

	private Surl surl;

	public PrepareToPutStatus(Surl surl) {
		this.setSurl(surl);
	}
	
	public Surl getSurl() {
		return surl;
	}

	private void setSurl(Surl surl) {
		this.surl = surl;
	}
	
	@Override
	public SurlArrayRequestOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException {
		SurlArrayRequestOutputData outputSPtP = null;
		log.debug("status of prepare to put on '" + this.getSurl().asString() + "' ...");
		try {
			if (user.isAnonymous()) { // HTTP
				outputSPtP = backend.prepareToPutStatus(this.getSurl().asString());
			} else if (user.getUserFQANS().isEmpty()) {
				outputSPtP = backend.prepareToPutStatus(user.getUserDN(), this.getSurl().asString());
			} else {
				outputSPtP = backend.prepareToPutStatus(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputSPtP.getStatus().getStatusCode().getValue());
		log.debug(outputSPtP.getStatus().getExplanation());
		return outputSPtP;
	}
	
}