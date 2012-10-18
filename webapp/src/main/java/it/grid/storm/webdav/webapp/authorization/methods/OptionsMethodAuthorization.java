package it.grid.storm.webdav.webapp.authorization.methods;

import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.PingOutputData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptionsMethodAuthorization extends AbstractMethodAuthorization {

	public OptionsMethodAuthorization(HttpServletRequest HTTPRequest) {
		super(HTTPRequest);
		this.HTTPRequest = HTTPRequest;
	}

	private static final Logger log = LoggerFactory.getLogger(OptionsMethodAuthorization.class);

	@Override
	public Map<String, String> getOperationsMap() throws IOException, ServletException {

		Map<String, String> operationsMap = new HashMap<String, String>();

		log.debug("For the method OPTIONS no authorization is needed.");
		
		// ping
		String userDN = (String) HTTPRequest.getAttribute("SUBJECT_DN");
		ArrayList<String> userFQANs = new ArrayList<String>();
		String[] fqansArr = StringUtils.split((String) HTTPRequest.getAttribute("FQANS"), ",");
		for (String s : fqansArr)			
			userFQANs.add(s);
		
		String stormBEHostname = (String) HTTPRequest.getAttribute("STORM_BACKEND_HOST");
		int stormBEPort = (Integer) HTTPRequest.getAttribute("STORM_BACKEND_PORT");
		
		log.debug("userDN = " + userDN);
		log.debug("userFQANs = " + StringUtils.join(userFQANs.toArray(), ","));
		log.debug("stormBEHostname = " + stormBEHostname);
		log.debug("stormBEPort = " + stormBEPort);
		
    	try {
			log.info("ping " + stormBEHostname + ":" + stormBEPort);
			BackendApi be = new BackendApi(stormBEHostname, new Long(stormBEPort));
			PingOutputData pud = be.ping(userDN, userFQANs);
			log.info(pud.toString());
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		return operationsMap;
	}
}
