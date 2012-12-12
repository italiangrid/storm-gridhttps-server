package it.grid.storm.authorization;

import it.grid.storm.HttpHelper;

import java.security.cert.X509Certificate;
import java.util.ArrayList;

import org.italiangrid.utils.voms.VOMSSecurityContext;
import org.italiangrid.voms.VOMSAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserCredentials {
	
	private static final Logger log = LoggerFactory.getLogger(UserCredentials.class);
	
	private String userDN;
	private ArrayList<String> userFQANS;
	private boolean isHttp;

	public UserCredentials(HttpHelper httpHelper) {
		initAsAnonymous();
		if (httpHelper.isHttp())
			return;
		/* It's an HTTPS request: */
		VOMSSecurityContext.clearCurrentContext();
		VOMSSecurityContext currentContext = new VOMSSecurityContext();
		VOMSSecurityContext.setCurrentContext(currentContext);
		X509Certificate[] certChain = httpHelper.getX509Certificate();
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