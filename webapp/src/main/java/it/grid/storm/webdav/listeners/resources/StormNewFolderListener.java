package it.grid.storm.webdav.listeners.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.NewFolderEvent;
import io.milton.http.fs.FsDirectoryResource;

import it.grid.storm.webdav.listeners.StormEventListener;

@SuppressWarnings({ "rawtypes" })
public class StormNewFolderListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormNewFolderListener.class);

	private Class eventType = NewFolderEvent.class; // MKCOL

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());
		FsDirectoryResource r = (FsDirectoryResource)((NewFolderEvent) e).getResource();
		
	}

}