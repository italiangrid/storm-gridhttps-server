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
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;

public class PrepareToGet implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToGet.class);

	private Surl surl;
	private ArrayList<String> transferProtocols;

	public PrepareToGet(Surl surl, ArrayList<String> transferProtocols) {
		this.setSurl(surl);
		this.setTransferProtocols(transferProtocols);
	}

	@Override
	public PtGOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException, StormRequestFailureException {
		log.debug("prepare to get '" + this.getSurl().asString() + "' with transfer protocols '"
				+ StringUtils.join(this.getTransferProtocols().toArray(), ',') + "' ...");
		PtGOutputData outputPtG = null;
		try {
			if (user.isAnonymous()) { // HTTP
				outputPtG = backend.prepareToGet(this.getSurl().asString(), this.getTransferProtocols());
			} else if (user.getUserFQANS().isEmpty()) {
				outputPtG = backend.prepareToGet(user.getUserDN(), this.getSurl().asString(), this.getTransferProtocols());
			} else {
				outputPtG = backend.prepareToGet(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString(),
						this.getTransferProtocols());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.debug(outputPtG.getStatus().getExplanation());
		if (!outputPtG.getStatus().getStatusCode().getValue().equals("SRM_FILE_PINNED")) {
			throw new StormRequestFailureException(outputPtG.getStatus());
		}
		return outputPtG;
	}

	public Surl getSurl() {
		return surl;
	}

	private void setSurl(Surl surl) {
		this.surl = surl;
	}

	public ArrayList<String> getTransferProtocols() {
		return transferProtocols;
	}

	private void setTransferProtocols(ArrayList<String> transferProtocols) {
		this.transferProtocols = transferProtocols;
	}

}