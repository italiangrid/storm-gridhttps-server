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
import it.grid.storm.xmlrpc.outputdata.PtGOutputData;

public class PrepareToGet implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(PrepareToGet.class);

	private Surl surl;
	private ArrayList<String> transferProtocols = new ArrayList<String>();

	public PrepareToGet(Surl surl, ArrayList<String> transferProtocols) {

		if (surl == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName()
				+ " constructor: null surl");
		if (transferProtocols == null)
			throw new IllegalArgumentException(this.getClass().getSimpleName()
				+ " constructor: null transferProtocols");

		this.setSurl(surl);
		this.setTransferProtocols(transferProtocols);
	}

	public PrepareToGet(Surl surl) {

		this(surl, new ArrayList<String>());
	}

	@Override
	public PtGOutputData executeAs(UserCredentials user, BackendApi backend)
		throws SRMOperationException {

		if (this.getTransferProtocols().isEmpty()) {
			log.debug("srmPtG '{}' ...", getSurl());
		} else {
			log.debug("srmPtG '{}' with transfer protocols '{}' ...", getSurl(),
				StringUtils.join(getTransferProtocols().toArray(), ','));
		}

		PtGOutputData outputPtG = null;
		try {
			if (this.getTransferProtocols().isEmpty()) {
				if (user.isAnonymous()) {
					outputPtG = backend.prepareToGet(getSurl().toString());
				} else if (user.getUserFQANS().isEmpty()) {
					outputPtG = backend.prepareToGet(user.getUserDN(), getSurl()
						.toString());
				} else {
					outputPtG = backend.prepareToGet(user.getUserDN(),
						user.getUserFQANS(), getSurl().toString());
				}
			} else {
				if (user.isAnonymous()) {
					outputPtG = backend.prepareToGet(getSurl().toString(),
						this.getTransferProtocols());
				} else if (user.getUserFQANS().isEmpty()) {
					outputPtG = backend.prepareToGet(user.getUserDN(), getSurl()
						.toString(), getTransferProtocols());
				} else {
					outputPtG = backend.prepareToGet(user.getUserDN(),
						user.getUserFQANS(), getSurl().toString(), getTransferProtocols());
				}
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			throw new SRMOperationException(new TReturnStatus(
				TStatusCode.SRM_INTERNAL_ERROR, e.toString()));
		}
		log.debug(outputPtG.getStatus().getStatusCode().getValue());
		log.debug(outputPtG.getStatus().getExplanation());
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

		this.transferProtocols.clear();
		this.transferProtocols.addAll(transferProtocols);
	}

}