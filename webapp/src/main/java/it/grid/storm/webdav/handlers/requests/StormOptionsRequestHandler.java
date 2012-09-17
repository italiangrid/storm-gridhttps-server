package it.grid.storm.webdav.handlers.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.http.Request.Method;

import it.grid.storm.webdav.handlers.StormHandler;

public class StormOptionsRequestHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormOptionsRequestHandler.class);

	private Method m = Method.OPTIONS;

	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {

		log.info("this is the exec function of "
				+ StormOptionsRequestHandler.class);
		
		/* making ping to the StoRM server */
		ping();
		
	}
	
	private void ping(){
		/* simulating ping command */
		log.info("simulating ping command ...");
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
