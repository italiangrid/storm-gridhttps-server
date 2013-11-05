package it.grid.storm.gridhttps.webapp.common.srmOperations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TReturnStatus;
import it.grid.storm.srm.types.TStatusCode;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

public class Ping implements SRMOperation {

	private static final Logger log = LoggerFactory.getLogger(Ping.class);
	
	private String hostname;
	private int port;
	
	public Ping(String hostname, int port) {
		this.setHostname(hostname);
		this.setPort(port);
	}
	
	@Override
	public PingOutputData executeAs(UserCredentials user, BackendApi backend) throws SRMOperationException {
		log.debug("ping " + this.getHostname() + ":" + this.getPort());
		PingOutputData output = null;
		try {
			if (user.isAnonymous()) { // HTTP
				output = backend.ping();
			} else if (user.getUserFQANS().isEmpty()) {
				output = backend.ping(user.getUserDN());
			} else {
				output = backend.ping(user.getUserDN(), user.getUserFQANS());
			}
		} catch (ApiException e) {
			log.error(e.getMessage());
			TReturnStatus status = new TReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, e.toString());
			throw new SRMOperationException(status, e);
		}
		log.debug(output.getBeOs());
		log.debug(output.getBeVersion());
		log.debug(output.getVersionInfo());
		return output;
	}

	public String getHostname() {
		return hostname;
	}

	private void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	private void setPort(int port) {
		this.port = port;
	}
	
}
