package it.grid.storm.gridhttps.webapp.common.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TRequestToken;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.SurlArrayRequestOutputData;

public class PutDone implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(ReleaseFile.class);
	
	private ArrayList<String> surlList = new ArrayList<String>();
	private TRequestToken token;
	
	public PutDone(Surl surl, TRequestToken token) {
		
		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl");
		if (token == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null token");
		
		this.surlList.clear();
		this.surlList.add(surl.toString());
		this.setToken(token);
	}
	
	public PutDone(ArrayList<Surl> surlList, TRequestToken token) {
		
		if (surlList == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl-list");
		if (surlList.isEmpty())
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: empty surl-list");
		
		this.surlList.clear();
		for (Surl surl : surlList)
			this.surlList.add(surl.toString());
		this.setToken(token);
	}
	
	@Override
	public SurlArrayRequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
	
		log.debug("srmPd '{}'", StringUtils.join(getSurlList().toArray(), ','));
		SurlArrayRequestOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.putDone(this.getSurlList(), this.getToken());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.putDone(user.getUserDN(), this.getSurlList(), this.getToken());
			} else {
				output = backend.putDone(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getToken());
			}
		} catch (Exception e) {
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