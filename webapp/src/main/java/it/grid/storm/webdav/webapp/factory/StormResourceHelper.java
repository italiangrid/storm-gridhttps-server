
package it.grid.storm.webdav.webapp.factory;


import io.milton.servlet.MiltonServlet;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StormResourceHelper {

	private static final Logger log = LoggerFactory
			.getLogger(StormResourceHelper.class);
	
	private String BEHostname;
	private long BEPort;
	private String contextPath;
	private StormResource res;

	public StormResourceHelper(){
		init();
	}
	
	public StormResourceHelper(StormResource res) {
		this.res = res;
		init();
	}
	
	private void init(){
		this.BEHostname = (String) MiltonServlet.request().getAttribute(
				"STORM_BACKEND_HOST");
		this.BEPort = Integer.valueOf((String) MiltonServlet.request()
				.getAttribute("STORM_BACKEND_PORT"));
		this.contextPath = (String) MiltonServlet.request().getAttribute(
				"STORAGE_AREA_NAME");
	}

	public BackendApi createBackend() throws IOException {
		BackendApi be;
		try {
			be = new BackendApi(BEHostname, BEPort);
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}
		return be;
	}

	public String getUserDN() {
		String userDN = (String) MiltonServlet.request().getAttribute(
				"SUBJECT_DN");
		log.debug(" # userDN = " + userDN);
		return userDN;
	}

	public List<String> getUserFQANS() {
		List<String> userFQANS = new ArrayList<String>();
		for (String s : (String[]) MiltonServlet.request()
				.getAttribute("FQANS"))
			userFQANS.add(s);
		log.debug(" # fqANs = ( " + userFQANS.toArray().toString() + ")");
		return userFQANS;
	}

	public List<String> getProtocols() {
		List<String> protocols = new ArrayList<String>();
		protocols.add(MiltonServlet.request().getProtocol()); // to check
		log.debug(" # protocols = " + protocols.toString());
		return protocols;
	}

	public List<String> getSurls() {
		List<String> surls = new ArrayList<String>();
		surls.add(this.getSurl());
		return surls;
	}

	protected String getSurl() {
		String surl = "srm://" + BEHostname + ":" + BEPort + "/" + contextPath
				+ "/" + this.res.getFile().getPath();
		log.debug(" # surl = " + surl);
		return surl;
	}

	protected boolean isUserAuthorized(String operation)
			throws IllegalArgumentException, Exception {
		return StormAuthorizationUtils.isUserAuthorized(this.getUserDN(),
				(String[]) this.getUserFQANS().toArray(), operation, this.res
						.getFile().toString());
	}

}
