package it.grid.storm.webdav.listeners.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.PropPatchEvent;

import it.grid.storm.webdav.listeners.StormEventListener;

@SuppressWarnings({"rawtypes"})
public class StormPropPatchListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormPropPatchListener.class);

	private Class eventType = PropPatchEvent.class;

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

	}

}