package it.grid.storm.webdav.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.http.Request.Method;

public class StormPropfindResponseHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormPropfindResponseHandler.class);

	private Method m = Method.PROPFIND;

	@Override
	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {
		log.debug("this is the exec function of "
				+ StormPropfindResponseHandler.class);
	}

}
