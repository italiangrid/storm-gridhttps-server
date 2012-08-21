package it.grid.storm.webdav;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.webdav.listeners.StormEventListener;

import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.EventManager;
import io.milton.event.EventManagerImpl;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;

public class StormEventManager implements EventManager {

	private static final Logger log = LoggerFactory
			.getLogger(StormEventManager.class);
	private final EventManagerImpl manager = new EventManagerImpl();
	private List<StormEventListener> listenersList = new ArrayList<StormEventListener>();

	public List<StormEventListener> getListenersList() {
		return listenersList;
	}

	public void setListenersList(List<StormEventListener> listenersList) {
		this.listenersList = listenersList;
	}

	public void init() {
		log.info("Registring event listeners from the list");
		if (!listenersList.isEmpty()) {
			for (StormEventListener l : listenersList) {
				registerEventListener(l, l.getEventType());
			}
		}
	}

	public void fireEvent(Event e) throws ConflictException,
			BadRequestException, NotAuthorizedException {
		manager.fireEvent(e);
	}

	public <T extends Event> void registerEventListener(EventListener l,
			Class<T> c) {
		manager.registerEventListener(l, c);
	}

}