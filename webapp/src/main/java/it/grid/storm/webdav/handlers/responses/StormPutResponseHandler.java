package it.grid.storm.webdav.handlers.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;import io.milton.event.ResponseEvent;
import io.milton.http.Request.Method;
import it.grid.storm.webdav.handlers.StormHandler;

public class StormPutResponseHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormPutResponseHandler.class);

	private Method m = Method.PUT;

	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {
		
		log.info("this is the exec function of "
				+ StormPutResponseHandler.class);
		
//		log.info("attributes of put response:\n"
//				+ ((ResponseEvent) e).getRequest().getAttributes().toString());
		
	}

}
