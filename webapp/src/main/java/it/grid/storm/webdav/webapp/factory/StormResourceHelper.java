package it.grid.storm.webdav.webapp.factory;

import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;
import it.grid.storm.xmlrpc.ApiException;
import it.grid.storm.xmlrpc.BackendApi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);

	private HttpServletRequest HTTPRequest;
	private StormResource res;

	public StormResourceHelper(HttpServletRequest HTTPRequest) {
		this.HTTPRequest = HTTPRequest;
	}

	public StormResourceHelper(HttpServletRequest HTTPRequest, StormResource res) {
		this(HTTPRequest);
		this.res = res;
	}

	public BackendApi createBackend() throws IOException {
		BackendApi be;
		try {
			be = new BackendApi(getBEHostname(), (long)getBEPort());
		} catch (ApiException e) {
			throw new IOException(e.getMessage());
		}
		return be;
	}

	public String getBEHostname() {
		return (String) HTTPRequest.getAttribute("STORM_BACKEND_HOST"); 
	}
	
	public int getBEPort() {
		return (Integer) HTTPRequest.getAttribute("STORM_BACKEND_PORT");
	}
	
	public String getContextPath() {
		return (String) HTTPRequest.getAttribute("STORAGE_AREA_NAME"); 
	}
	
	public String getUserDN() {
		String userDN = (String) HTTPRequest.getAttribute("SUBJECT_DN"); 
		log.debug(" # userDN = " + userDN);
		return userDN;
	}

	public List<String> getUserFQANS() {
		List<String> userFQANS = new ArrayList<String>();
		log.debug(" # fqANs = ( ");
		for (String s : (String[]) HTTPRequest.getAttribute("FQANS")) {
			userFQANS.add(s);
			log.debug("  " + s);
		}
		log.debug(" )");
		//log.debug(" # fqANs = ( " + ((String[])userFQANS.toArray()).toString() + ")");
		return userFQANS;
	}

	public List<String> getProtocols() {
		List<String> protocols = new ArrayList<String>();
		protocols.add(HTTPRequest.getProtocol()); // to check
		log.debug(" # protocols = " + protocols.toString());
		return protocols;
	}

	public List<String> getSurls() {
		List<String> surls = new ArrayList<String>();
		surls.add(this.getSurl());
		return surls;
	}

	protected String getSurl() {
		String surl = "srm://" + getBEHostname() + ":" + getBEPort() + "/" + getContextPath() + "/" + this.res.getFile().getPath();
		log.debug(" # surl = " + surl);
		return surl;
	}

	protected boolean isUserAuthorized(String operation) throws IllegalArgumentException, Exception {
		return StormAuthorizationUtils.isUserAuthorized(getBEHostname(), (int)getBEPort(), getUserDN(), (String[]) getUserFQANS().toArray(), operation, this.res
				.getFile().toString());
	}

}
