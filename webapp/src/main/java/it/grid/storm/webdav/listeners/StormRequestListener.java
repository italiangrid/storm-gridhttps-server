package it.grid.storm.webdav.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.RequestEvent;
import io.milton.http.Request;
import it.grid.storm.webdav.handlers.StormHandler;


@SuppressWarnings({ "rawtypes" })
public class StormRequestListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormRequestListener.class);

	private Class eventType = RequestEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

		Request req = ((RequestEvent) e).getRequest();

		String methodName = req.getMethod().toString();
		log.debug("methodName: " + methodName);

//		log.info("getHeaders: " + req.getHeaders().toString());

		/* calls the right handler, if there is one */
		if (super.handlersMapContainsKey(methodName)) {
			log.debug(methodName + " is contained in handlersMap");
			StormHandler handler = super.handlersMapGetValue(methodName);
			log.debug("handler: " + handler.getClass().getCanonicalName());
			handler.exec(e);
		} else {
			log.warn(methodName + " is not contained in handlersMap");
		}

	}

}