package it.grid.storm.webdav.listeners.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.DeleteEvent;
import io.milton.event.Event;

import it.grid.storm.webdav.listeners.StormEventListener;

@SuppressWarnings({"rawtypes"})
public class StormDeleteListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormDeleteListener.class);

	private Class eventType = DeleteEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

	}

}