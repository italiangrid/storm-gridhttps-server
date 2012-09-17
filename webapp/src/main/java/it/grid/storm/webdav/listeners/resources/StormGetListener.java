package it.grid.storm.webdav.listeners.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.GetEvent;

import it.grid.storm.webdav.listeners.StormEventListener;

@SuppressWarnings({"rawtypes"})
public class StormGetListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormGetListener.class);

	private Class eventType = GetEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

	}

}