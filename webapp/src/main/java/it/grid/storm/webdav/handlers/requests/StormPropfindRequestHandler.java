package it.grid.storm.webdav.handlers.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.RequestEvent;
import io.milton.http.Request.Method;
import it.grid.storm.webdav.handlers.StormHandler;

public class StormPropfindRequestHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormPropfindRequestHandler.class);

	private Method m = Method.PROPFIND;

	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {

		log.info("this is the exec function of "
				+ StormPropfindRequestHandler.class);

		log.info("Header:\n"+((RequestEvent) e).getRequest().getHeaders().toString());
		
	}
}
