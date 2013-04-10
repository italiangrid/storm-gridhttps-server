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
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;

public class LsDetailed implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(LsDetailed.class);

	private ArrayList<String> surlList;
	private RecursionLevel recursion;
	private boolean hasCount;
	private int count;

	public LsDetailed(Surl surl) {
		this.surlList = new ArrayList<String>();
		this.surlList.add(surl.asString());
		this.setRecursion(new RecursionLevel(Recursion.NONE));
		this.setHasCount(false);
	}

	public LsDetailed(ArrayList<Surl> surlList) {
		this.surlList = new ArrayList<String>();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
		this.setRecursion(new RecursionLevel(Recursion.NONE));
		this.setHasCount(false);
	}

	public LsDetailed(Surl surl, RecursionLevel recursion) {
		this(surl);
		this.setRecursion(recursion);
	}

	public LsDetailed(ArrayList<Surl> surlList, RecursionLevel recursion) {
		this(surlList);
		this.setRecursion(recursion);
	}

	public LsDetailed(Surl surl, RecursionLevel recursion, int count) {
		this(surl, recursion);
		this.setHasCount(true);
		this.setCount(count);
	}

	public LsDetailed(ArrayList<Surl> surlList, RecursionLevel recursion, int count) {
		this(surlList, recursion);
		this.setHasCount(true);
		this.setCount(count);
	}

	@Override
	public LsOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException,
			TooManyResultsException {
		log.debug("ls-detailed '" + StringUtils.join(this.getSurlList().toArray(), ',') + "' ...");
		LsOutputData output = null;
		try {
			if (this.hasCount()) {
				if (user.isAnonymous()) { // HTTP
					output = backend.lsDetailed(this.getSurlList(), this.getRecursion(), this.getCount());
				} else if (user.getUserFQANS().isEmpty()) {
					output = backend.lsDetailed(user.getUserDN(), this.getSurlList(), this.getRecursion(), this.getCount());
				} else {
					output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getRecursion(), this.getCount());
				}
			} else {
				if (user.isAnonymous()) { // HTTP
					output = backend.lsDetailed(this.getSurlList(), this.getRecursion());
				} else if (user.getUserFQANS().isEmpty()) {
					output = backend.lsDetailed(user.getUserDN(), this.getSurlList(), this.getRecursion());
				} else {
					output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getRecursion());
				}
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException("Backend API Exception!", e);
		}
		log.debug(output.getStatus().getStatusCode().getValue());
		log.debug(output.getStatus().getExplanation());
		if (output.getStatus().getStatusCode().equals(TStatusCode.SRM_TOO_MANY_RESULTS))
			throw new TooManyResultsException("ls-detailed output status is " + output.getStatus().getStatusCode().getValue(), output.getStatus());
		if (!output.isSuccess())
			throw new StormRequestFailureException(output.getStatus());
		return output;
	}

	public ArrayList<String> getSurlList() {
		return surlList;
	}

	public RecursionLevel getRecursion() {
		return recursion;
	}

	private void setRecursion(RecursionLevel recursion) {
		this.recursion = recursion;
	}

	public int getCount() {
		return count;
	}

	private void setCount(int count) {
		this.count = count;
	}

	public boolean hasCount() {
		return hasCount;
	}

	private void setHasCount(boolean hasCount) {
		this.hasCount = hasCount;
	}

}