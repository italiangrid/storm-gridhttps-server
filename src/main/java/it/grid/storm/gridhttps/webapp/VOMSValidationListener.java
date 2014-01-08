/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
