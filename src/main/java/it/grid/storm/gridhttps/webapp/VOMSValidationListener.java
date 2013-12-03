package it.grid.storm.gridhttps.webapp;

import org.italiangrid.voms.ac.VOMSValidationResult;
import org.italiangrid.voms.ac.ValidationResultListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logs information about VOMS attributes validations in incoming
 * requests.
 *
 */
public class VOMSValidationListener implements ValidationResultListener {
	
	public static final Logger log = 
		LoggerFactory.getLogger(VOMSValidationListener.class);

	@Override
	public void notifyValidationResult(VOMSValidationResult result) {
		if (!result.isValid())
			log.warn("VOMS attributes validation result: {}", result);
		else
			log.debug("VOMS attributes validation result: {}", result);
	}

}
