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
	
	private final String EMPTY_USERDN = "";
	
	private String userDN;
	private ArrayList<String> userFQANS;
	private HttpHelper httpHelper;
	
	public UserCredentials(HttpHelper httpHelper) {
		this.httpHelper = httpHelper;
		initAsAnonymous();
		if (this.httpHelper.isHttp())
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
		userDN = currentContext.getClientDN() != null ? currentContext.getClientDN().getX500() : getEmptyUserDN();
		log.debug("DN = " + userDN);
		userFQANS.clear();
		for (VOMSAttribute voms : currentContext.getVOMSAttributes())
			for (String s : voms.getFQANs()) {
				userFQANS.add(s);
				log.debug("fqan = " + s);
			}
	}
	
	/* public methods */
	
	public String getUserDN() {
		return isForcedAnonymous() ? getEmptyUserDN() : userDN;
	}

	public ArrayList<String> getUserFQANS() {
		return isForcedAnonymous() ? getEmptyUserFQANS() : userFQANS;
	}

	public boolean isAnonymous() {
		return isForcedAnonymous() || (httpHelper.isHttp() && isUserDNEmpty() && isUserFQANSEmpty());
	}
	
	public void forceAnonymous(String userDN, ArrayList<String> userFQANS) {
		/* it's a kind of security check... */
		if (userDN.equals(getUserDN()) && userFQANS.equals(getUserFQANS())) {
			setForcedAnonymous();
		}
	}

	/* private methods */
	
	private void initAsAnonymous() {
		userDN = getEmptyUserDN();
		userFQANS = getEmptyUserFQANS();
	}
	
	private void setForcedAnonymous() {
		httpHelper.getRequest().getSession().setAttribute("forced", true);
	}
	
	private boolean isForcedAnonymous() {
		return (Boolean) httpHelper.getRequest().getSession().getAttribute("forced");
	}
	

	private String getEmptyUserDN() {
		return EMPTY_USERDN;
	}
	
	private ArrayList<String> getEmptyUserFQANS() {
		return new ArrayList<String>();
	}
	
	private boolean isUserDNEmpty() {
		return userDN.isEmpty();
	}
	
	private boolean isUserFQANSEmpty() {
		return userFQANS.isEmpty();
	}
}