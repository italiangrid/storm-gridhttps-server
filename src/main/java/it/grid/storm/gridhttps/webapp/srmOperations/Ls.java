package it.grid.storm.gridhttps.webapp.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.TooManyResultsException;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;

public class Ls implements SRMOperation {
	
	private static final Logger log = LoggerFactory.getLogger(Ls.class);
	
	private ArrayList<String> surlList;
	
	public Ls(Surl surl) {
		this.surlList = new ArrayList<String>();
		this.surlList.add(surl.asString());
	}
	
	public Ls(ArrayList<Surl> surlList) {
		this.surlList = new ArrayList<String>();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
	}
	
	@Override
	public LsOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException, TooManyResultsException {
		log.debug("ls '" + StringUtils.join(this.getSurlList().toArray(), ',') + "' ...");
		LsOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.ls(this.getSurlList());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.ls(user.getUserDN(), this.getSurlList());
			} else {
				output = backend.ls(user.getUserDN(), user.getUserFQANS(), this.getSurlList());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException("Backend API Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS))
			throw new TooManyResultsException("ls output status is " + output.getStatus().getStatusCode().getValue(), output.getStatus());
		if (!output.isSuccess())
			throw new StormRequestFailureException(output.getStatus());
		return output;
	}

	public ArrayList<String> getSurlList() {
		return surlList;
	}
	
}