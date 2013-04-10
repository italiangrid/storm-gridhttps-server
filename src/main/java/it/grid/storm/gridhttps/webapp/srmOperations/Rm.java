package it.grid.storm.gridhttps.webapp.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class Rm implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Rm.class);
	
	private ArrayList<String> surlList;

	public Rm(Surl surl) {
		this.surlList = new ArrayList<String>();
		this.surlList.add(surl.asString());
	}
	
	public Rm(ArrayList<Surl> surlList) {
		this.surlList = new ArrayList<String>();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
	}
	
	public ArrayList<String> getSurlList() {
		return surlList;
	}
	
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException {
		RequestOutputData output = null;
		log.debug("delete '" + StringUtils.join(this.getSurlList().toArray(), ',') + "' ...");
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.rm(this.getSurlList());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.rm(user.getUserDN(), this.getSurlList());
			} else {
				output = backend.rm(user.getUserDN(), user.getUserFQANS(), this.getSurlList());
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
	
}