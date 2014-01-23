package it.grid.storm.gridhttps.webapp.common.srmOperations;

import it.grid.storm.gridhttps.webapp.common.Surl;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.FileTransferOutputData;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrepareToPut implements SRMOperation {

	private static final boolean DEFAULT_OVERWRITE = false;
	private static final ArrayList<String> DEFAULT_TRANSFERPROTOCOLS = new ArrayList<String>();
	
	private static final Logger log = LoggerFactory.getLogger(PrepareToPut.class);

	private Surl surl;
	private ArrayList<String> transferProtocols = new ArrayList<String>();
	private boolean overwrite;

	public PrepareToPut(Surl surl) {
		this(surl, DEFAULT_OVERWRITE, DEFAULT_TRANSFERPROTOCOLS);
	}
	
	public PrepareToPut(Surl surl, ArrayList<String> transferProtocols) {
		this(surl, DEFAULT_OVERWRITE, transferProtocols);
	}
	
	public PrepareToPut(Surl surl, boolean overwrite) {
		this(surl, overwrite, DEFAULT_TRANSFERPROTOCOLS);
	}
	
	public PrepareToPut(Surl surl, boolean overwrite, ArrayList<String> transferProtocols) {
		this.setSurl(surl);
		this.setTransferProtocols(transferProtocols);
		this.setOverwrite(overwrite);
	}

	@Override
	public FileTransferOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {

		if (this.getTransferProtocols().isEmpty()) {
			log.debug("srmPtP (overwrite={}) '{}' ...", isOverwrite(), getSurl());
		} else {
			log.debug(
				"srmPtP (overwrite={}) '{}' with transfer protocols {} ...",
				new Object[] { isOverwrite(), getSurl(),
					StringUtils.join(getTransferProtocols().toArray(), ',') });
		}
		
		FileTransferOutputData outputPtP = null;
		try {
			if (this.getTransferProtocols().isEmpty()) {
				if (this.isOverwrite()) {
					if (user.isAnonymous()) {
						outputPtP = backend.prepareToPutOverwrite(getSurl().toString());
					} else if (user.getUserFQANS().isEmpty()) {
						outputPtP = backend.prepareToPutOverwrite(user.getUserDN(),
							getSurl().toString());
					} else {
						outputPtP = backend.prepareToPutOverwrite(user.getUserDN(),
							user.getUserFQANS(), getSurl().toString());
					}
				} else {
					if (user.isAnonymous()) {
						outputPtP = backend.prepareToPut(getSurl().toString());
					} else if (user.getUserFQANS().isEmpty()) {
						outputPtP = backend.prepareToPut(user.getUserDN(), getSurl()
							.toString());
					} else {
						outputPtP = backend.prepareToPut(user.getUserDN(),
							user.getUserFQANS(), getSurl().toString());
					}
				}
			} else {
				if (this.isOverwrite()) {
					if (user.isAnonymous()) {
						outputPtP = backend.prepareToPutOverwrite(getSurl().toString(),
							getTransferProtocols());
					} else if (user.getUserFQANS().isEmpty()) {
						outputPtP = backend.prepareToPutOverwrite(user.getUserDN(),
							getSurl().toString(), getTransferProtocols());
					} else {
						outputPtP = backend
							.prepareToPutOverwrite(user.getUserDN(), user.getUserFQANS(),
								getSurl().toString(), getTransferProtocols());
					}
				} else {
					if (user.isAnonymous()) {
						outputPtP = backend.prepareToPut(getSurl().toString(),
							getTransferProtocols());
					} else if (user.getUserFQANS().isEmpty()) {
						outputPtP = backend.prepareToPut(user.getUserDN(), getSurl()
							.toString(), getTransferProtocols());
					} else {
						outputPtP = backend
							.prepareToPut(user.getUserDN(), user.getUserFQANS(), getSurl()
								.toString(), getTransferProtocols());
					}
				}
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		log.debug(outputPtP.getStatus().getStatusCode().getValue());
		log.debug(outputPtP.getStatus().getExplanation());
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
		this.transferProtocols.clear();
		if (transferProtocols != null && !transferProtocols.isEmpty())
			this.transferProtocols.addAll(transferProtocols);
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	private void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

}