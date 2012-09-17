package it.grid.storm.webdav.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.ResponseEvent;

import it.grid.storm.webdav.handlers.StormHandler;

@SuppressWarnings({ "rawtypes" })
public class StormResponseListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormResponseListener.class);

	private Class eventType = ResponseEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

		String methodName = super.getRequestMethodName(e);
		log.debug("methodName: " + methodName);

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