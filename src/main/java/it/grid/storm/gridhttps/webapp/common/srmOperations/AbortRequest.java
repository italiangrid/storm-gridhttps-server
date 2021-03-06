package it.grid.storm.gridhttps.webapp.common.srmOperations;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class AbortRequest implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(AbortRequest.class);

	private ArrayList<String> surlList;
	private TRequestToken token;

	public AbortRequest(Surl surl, TRequestToken token) {

		surlList = new ArrayList<String>();
		surlList.add(surl.toString());
		setToken(token);
	}

	public AbortRequest(ArrayList<Surl> surlList, TRequestToken token) {

		this.surlList = new ArrayList<String>();
		for (Surl surl : surlList)
			this.surlList.add(surl.toString());
		setToken(token);
	}

	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend)
		throws SRMOperationException {

		log.debug("srmAbortRequest token: '{}'", getToken().getValue());
		RequestOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.abortRequest(getToken());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.abortRequest(user.getUserDN(), getToken());
			} else {
				output = backend.abortRequest(user.getUserDN(), user.getUserFQANS(),
					getToken());
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

	public ArrayList<String> getSurlList() {

		return surlList;
	}

	public TRequestToken getToken() {

		return token;
	}

	private void setToken(TRequestToken token) {

		this.token = token;
	}

}