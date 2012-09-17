package it.grid.storm.webdav.handlers.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.RequestEvent;
import io.milton.http.Request.Method;
import it.grid.storm.webdav.handlers.StormHandler;

public class StormPutRequestHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormPutRequestHandler.class);

	private Method m = Method.PUT;

	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {

		log.info("this is the exec function of " + StormPutRequestHandler.class);
//
//		((RequestEvent) e).getRequest().getAttributes().put("KEY", "VALUE");
//		
//		log.info("attributes of put request:\n"
//				+ ((RequestEvent) e).getRequest().getAttributes().toString());

	}

}
