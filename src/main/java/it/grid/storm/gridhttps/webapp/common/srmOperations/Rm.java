package it.grid.storm.gridhttps.webapp.common.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
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
import it.grid.storm.xmlrpc.outputdata.RequestOutputData;

public class Rm implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Rm.class);
	
	private ArrayList<String> surlList = new ArrayList<String>();

	public Rm(Surl surl) {
		
		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl");
		
		this.surlList.clear();
		this.surlList.add(surl.asString());
	}
	
	public Rm(ArrayList<Surl> surlList) {
		
		if (surlList == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl-list");
		if (surlList.isEmpty())
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: empty surl-list");
		
		this.surlList.clear();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
	}
	
	public ArrayList<String> getSurlList() {
		return surlList;
	}
	
	@Override
	public RequestOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
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
			TReturnStatus status = new TReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, e.toString());
			throw new SRMOperationException(status, TSRMExceptionReason.INTERNALERROR);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		return output;
	}
	
}