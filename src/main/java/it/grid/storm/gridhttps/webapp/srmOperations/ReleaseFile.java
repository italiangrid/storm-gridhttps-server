package it.grid.storm.gridhttps.webapp.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class ReleaseFile implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(ReleaseFile.class);
	
	private ArrayList<String> surlList;
	private TRequestToken token;
	
	public ReleaseFile(Surl surl, TRequestToken token) {
		this.surlList = new ArrayList<String>();
		this.surlList.add(surl.asString());
		this.setToken(token);
	}
	
	public ReleaseFile(ArrayList<Surl> surlList, TRequestToken token) {
		this.surlList = new ArrayList<String>();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
		this.setToken(token);
	}
	
	@Override
	public SurlArrayRequestOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException {
	
		log.debug("release '" + StringUtils.join(this.getSurlList().toArray(), ',') + "' ...");
		SurlArrayRequestOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.releaseFiles(this.getSurlList(), this.getToken());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.releaseFiles(user.getUserDN(), this.getSurlList(), this.getToken());
			} else {
				output = backend.releaseFiles(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getToken());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		} catch (RuntimeException e) {
			log.error(e.getMessage());
			throw e;
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		if (!output.isSuccess()) {
			throw new StormRequestFailureException(output.getStatus());
		}
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