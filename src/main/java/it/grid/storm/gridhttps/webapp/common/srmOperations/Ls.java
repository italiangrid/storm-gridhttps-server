package it.grid.storm.gridhttps.webapp.common.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;

public class Ls implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Ls.class);

	private ArrayList<String> surlList = new ArrayList<String>();

	public Ls(Surl surl) {

		if (surl == null)
			throw new IllegalArgumentException("surl must not be null!");

		surlList.clear();
		surlList.add(surl.toString());
	}

	public Ls(ArrayList<Surl> surlList) {

		if (surlList == null)
			throw new IllegalArgumentException("surlList must not be null!");
		if (surlList.isEmpty())
			throw new IllegalArgumentException("surlList must not be empty!");

		this.surlList.clear();
		for (Surl surl : surlList)
			this.surlList.add(surl.toString());
	}

	@Override
	public LsOutputData executeAs(UserCredentials user, BackendApi backend)
		throws SRMOperationException {

		log.debug("srmLs '{}' ...", StringUtils.join(getSurlList().toArray(), ','));
		LsOutputData output = null;
		try {
			if (user.isAnonymous()) {
				output = backend.ls(this.getSurlList());
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.ls(user.getUserDN(), this.getSurlList());
			} else {
				output = backend.ls(user.getUserDN(), user.getUserFQANS(),
					this.getSurlList());
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

}