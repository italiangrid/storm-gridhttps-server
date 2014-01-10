package it.grid.storm.gridhttps.webapp.common.srmOperations;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.Recursion;
import it.grid.storm.srm.types.RecursionLevel;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;

public class LsDetailed implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(LsDetailed.class);

	public static RecursionLevel DEFAULT_RECURSION_LEVEL = new RecursionLevel(Recursion.NONE);
	public static int DEFAULT_COUNT = -1;
	
	private ArrayList<String> surlList = new ArrayList<String>();
	private RecursionLevel recursion;
	private int count;
	
	public LsDetailed(Surl surl) {
		this(surl, DEFAULT_RECURSION_LEVEL, DEFAULT_COUNT);
	}

	public LsDetailed(ArrayList<Surl> surlList) {
		this(surlList, DEFAULT_RECURSION_LEVEL, DEFAULT_COUNT);
	}

	public LsDetailed(Surl surl, RecursionLevel recursion) {
		this(surl, recursion, DEFAULT_COUNT);
	}

	public LsDetailed(ArrayList<Surl> surlList, RecursionLevel recursion) {
		this(surlList);
		this.setRecursion(recursion);
	}

	public LsDetailed(Surl surl, RecursionLevel recursion, int count) {
		
		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl");
		if (recursion == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null recursion");
		
		this.surlList.clear();
		this.surlList.add(surl.asString());
		this.setRecursion(recursion);
		this.setCount(count);
	}

	public LsDetailed(ArrayList<Surl> surlList, RecursionLevel recursion, int count) {
		
		if (surlList == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null surl-list");
		if (surlList.isEmpty())
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: empty surl-list");
		if (recursion == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName() + " constructor: null recursion");
		
		this.surlList.clear();
		for (Surl surl : surlList)
			this.surlList.add(surl.asString());
		this.setRecursion(recursion);
		this.setCount(count);
	}

	@Override
	public LsOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		
		log.debug(String.format("srmLsDetailed (count=%d,recursion=%s) on '%s' ...",
			this.getCount(), this.getRecursion(),
			StringUtils.join(this.getSurlList().toArray(), ',')));
		LsOutputData output = null;
		try {
			if (this.hasCount()) {
				if (user.isAnonymous()) {
					output = backend.lsDetailed(this.getSurlList(), this.getRecursion(), this.getCount());
				} else if (user.getUserFQANS().isEmpty()) {
					output = backend.lsDetailed(user.getUserDN(), this.getSurlList(), this.getRecursion(), this.getCount());
				} else {
					output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getRecursion(), this.getCount());
				}
			} else {
				if (user.isAnonymous()) {
					output = backend.lsDetailed(this.getSurlList(), this.getRecursion());
				} else if (user.getUserFQANS().isEmpty()) {
					output = backend.lsDetailed(user.getUserDN(), this.getSurlList(), this.getRecursion());
				} else {
					output = backend.lsDetailed(user.getUserDN(), user.getUserFQANS(), this.getSurlList(), this.getRecursion());
				}
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
		return this.getCount() != DEFAULT_COUNT;
	}

}