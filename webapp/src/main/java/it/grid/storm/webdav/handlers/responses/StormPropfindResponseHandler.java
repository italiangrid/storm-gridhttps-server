package it.grid.storm.webdav.handlers.responses;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.ResponseEvent;
import io.milton.http.Request.Method;
import it.grid.storm.webdav.handlers.StormHandler;

public class StormPropfindResponseHandler implements StormHandler {

	private static final Logger log = LoggerFactory
			.getLogger(StormPropfindResponseHandler.class);

	private Method m = Method.PROPFIND;

	public Method getMethod() {
		return m;
	}

	public void exec(Event e) {

		log.info("this is the exec function of "
				+ StormPropfindResponseHandler.class);

//		log.info("attributes of propfind response:\n"
//				+ ((ResponseEvent) e).getRequest().getAttributes());
//
//		log.info("_files:\n"
//				+ ((ResponseEvent) e).getRequest().getFiles().toString());
//
//		log.info("_params:\n"
//				+ ((ResponseEvent) e).getRequest().getParams().toString());

	}
}
