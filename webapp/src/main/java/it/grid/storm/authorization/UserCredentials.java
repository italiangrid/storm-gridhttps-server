package it.grid.storm.authorization;

import it.grid.storm.StormAuthorizationFilter;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentials {
	
	private static final Logger log = LoggerFactory.getLogger(UserCredentials.class);
	
	private String userDN;
	private ArrayList<String> userFQANS;
	private boolean isHttp;

	public UserCredentials(HttpServletRequest HTTPRequest) {
		initAsAnonymous();
		isHttp = StormAuthorizationFilter.HTTPRequest.getScheme().toUpperCase().equals("HTTP");
		if (isHttp) return;
		/* It's an HTTPS request: */
		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext currentContext = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(currentContext);
		X509Certificate[] certChain = getX509Certificate(HTTPRequest);
		if (certChain == null) {
			log.warn("Failed to init VOMS Security Context! User initialized with empty DN and FQANs");
			return;
		}
		currentContext.setClientCertChain(certChain);
		userDN = currentContext.getClientDN() != null ? currentContext.getClientDN().getX500() : "";
		log.debug("DN = " + userDN);
		userFQANS.clear();
		for (VOMSAttribute voms : currentContext.getVOMSAttributes())
			for (String s : voms.getFQANs()) {
				userFQANS.add(s);
				log.debug("fqan = " + s);
			}
	}

	private void initAsAnonymous() {
		userDN = "";
		userFQANS = new ArrayList<String>();
	}
	
	private X509Certificate[] getX509Certificate(HttpServletRequest HTTPRequest) {
		X509Certificate[] certChain;
		try {
			certChain = (X509Certificate[]) HTTPRequest.getAttribute("javax.servlet.request.X509Certificate");
		} catch (Exception e) {
			log.error("Error fetching certificate from http request: " + e.getMessage());
			return null;
		}
		return certChain;
	}

	public String getUserDN() {
		return userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return userFQANS;
	}

	public boolean isAnonymous() {
		return (isHttp && userDN.equals("") && userFQANS.isEmpty());
	}
	
}