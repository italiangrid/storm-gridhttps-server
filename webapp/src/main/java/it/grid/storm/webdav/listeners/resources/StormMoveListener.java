package it.grid.storm.webdav.listeners.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.MoveEvent;

import it.grid.storm.webdav.listeners.StormEventListener;

@SuppressWarnings({"rawtypes"})
public class StormMoveListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormMoveListener.class);

	private Class eventType = MoveEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

	}

}