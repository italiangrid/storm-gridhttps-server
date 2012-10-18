package it.grid.storm.webdav.webapp.factory;

import java.util.ArrayList;

import io.milton.servlet.MiltonServlet;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationFilter;
import it.grid.storm.webdav.webapp.authorization.StormAuthorizationUtils;

import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.italiangrid.utils.voms.VOMSSecurityContext;

public class StormResourceHelper {

//	private static final Logger log = LoggerFactory.getLogger(StormResourceHelper.class);

	public static String getUserDN() {
		VOMSSecurityContext sc = StormAuthorizationUtils.getVomsSecurityContext(MiltonServlet.request());
		return StormAuthorizationUtils.getUserDN(sc);
//		return (String) MiltonServlet.request().getAttribute("SUBJECT_DN");
	}
	
	public static ArrayList<String> getUserFQANs() {
		VOMSSecurityContext sc = StormAuthorizationUtils.getVomsSecurityContext(MiltonServlet.request());
		ArrayList<String> userFQANs = StormAuthorizationUtils.getUserFQANs(sc);
		
		/********************************TEST***********************************/
		userFQANs.clear();
		userFQANs.add("/dteam/Role=NULL/Capability=NULL");
		userFQANs.add("/dteam/NGI_IT/Role=NULL/Capability=NULL");
		/********************************TEST***********************************/
		
		
//		ArrayList<String> userFQANs = new ArrayList<String>();
//		String[] fqansArr = StringUtils.split((String) MiltonServlet.request().getAttribute("FQANS"), ",");
//		for (String s : fqansArr)			
//			userFQANs.add(s);
		return userFQANs;
	}
	
}
