package it.grid.storm.gridhttps.webapp.srmOperations;

import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.data.Surl;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareToPut implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToPut.class);

	private Surl surl;
	private ArrayList<String> transferProtocols;
	private boolean overwrite;

	public PrepareToPut(Surl surl, ArrayList<String> transferProtocols) {
		this.setSurl(surl);
		this.setTransferProtocols(transferProtocols);
		this.setOverwrite(false);
	}

	public PrepareToPut(Surl surl, ArrayList<String> transferProtocols, boolean overwrite) {
		this.setSurl(surl);
		this.setTransferProtocols(transferProtocols);
		this.setOverwrite(overwrite);
	}

	@Override
	public FileTransferOutputData executeAs(UserCredentials user, BackendApi backend) throws RuntimeApiException,
			StormRequestFailureException {
		log.debug("prepare to put '" + this.getSurl().asString() + "' with transfer protocols '"
				+ StringUtils.join(this.getTransferProtocols().toArray(), ',') + "' and overwrite is " + this.isOverwrite() + " ...");
		FileTransferOutputData outputPtP = null;
		try {
			if (this.isOverwrite()) {
				if (user.isAnonymous()) { // HTTP
					outputPtP = backend.prepareToPutOverwrite(this.getSurl().asString(), this.getTransferProtocols());
				} else if (user.getUserFQANS().isEmpty()) {
					outputPtP = backend.prepareToPutOverwrite(user.getUserDN(), this.getSurl().asString(), this.getTransferProtocols());
				} else {
					outputPtP = backend.prepareToPutOverwrite(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString(),
							this.getTransferProtocols());
				}
			} else {
				if (user.isAnonymous()) { // HTTP
					outputPtP = backend.prepareToPut(this.getSurl().asString(), this.getTransferProtocols());
				} else if (user.getUserFQANS().isEmpty()) {
					outputPtP = backend.prepareToPut(user.getUserDN(), this.getSurl().asString(), this.getTransferProtocols());
				} else {
					outputPtP = backend.prepareToPut(user.getUserDN(), user.getUserFQANS(), this.getSurl().asString(),
							this.getTransferProtocols());
				}
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new RuntimeApiException(e.getMessage(), e);
		}
		log.debug(outputPtP.getStatus().getStatusCode().getValue());
		log.debug(outputPtP.getStatus().getExplanation());
		if (!outputPtP.getStatus().getStatusCode().getValue().equals("SRM_SPACE_AVAILABLE")) {
			throw new StormRequestFailureException(outputPtP.getStatus());
		}
		return outputPtP;
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

	public boolean isOverwrite() {
		return overwrite;
	}

	private void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

}