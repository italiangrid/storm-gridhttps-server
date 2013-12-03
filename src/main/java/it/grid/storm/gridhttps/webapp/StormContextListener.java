package it.grid.storm.gridhttps.webapp;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.italiangrid.voms.VOMSValidators;
import org.italiangrid.voms.ac.VOMSACValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Gridhttps webapp startup/shutdown events.
 * 
 *
 */
public class StormContextListener implements ServletContextListener {

	public static final Logger log = 
		LoggerFactory.getLogger(StormContextListener.class);

	private void initializeVOMSValidator(ServletContextEvent sce){

		try{
			VOMSACValidator validator = 
				VOMSValidators.newValidator(new VOMSValidationListener()); 

			sce.getServletContext().setAttribute(HttpHelper.VOMS_VALIDATOR_KEY, 
				validator);
			
			log.debug("VOMS validator initialized succesfully.");

		}catch(Throwable t){

			String msg = String.format("Error initializing VOMS validator: %s", 
				t.getMessage());
			log.error(msg, t);

			throw new RuntimeException(msg, t);
		}
	}
	
	
	private void shutdownVOMSValidator(ServletContextEvent sce){
		try{
			
			VOMSACValidator validator = 
				(VOMSACValidator) sce.getServletContext()
				.getAttribute(HttpHelper.VOMS_VALIDATOR_KEY);
			
			if (validator != null){
				validator.shutdown();
				log.debug("VOMS validator stopped succesfully.");
			}else{
				log.warn("VOMS validator not found in application context. Bug?!");
			}

		}catch (Throwable t){
			String msg = String.format("Error shutting down VOMS validator: %s", 
				t.getMessage());

			log.error(msg, t);

			throw new RuntimeException(msg, t);
		}
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		log.info("Storm Gridhttps webapp rooted at {} starting...", 
			sce.getServletContext().getContextPath());
		
		initializeVOMSValidator(sce);
		log.info("Storm Gridhttps webapp rooted at {} started.", 
			sce.getServletContext().getContextPath());
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {

		log.info("Storm Gridhttps webapp rooted at {} stopping...", 
			sce.getServletContext().getContextPath());

		shutdownVOMSValidator(sce);

		log.info("Storm Gridhttps webapp rooted at {} stopped.",
			sce.getServletContext().getContextPath());


	}

}
