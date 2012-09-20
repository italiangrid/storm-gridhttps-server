package it.grid.storm.webdav.listeners;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.grid.storm.webdav.handlers.StormHandler;

import io.milton.event.Event;
import io.milton.event.EventListener;
import io.milton.event.RequestEvent;
import io.milton.http.Request.Method;

public abstract class StormEventListener implements EventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormEventListener.class);

	
	private List<Class<StormHandler>> handlersList;
	private Map<Method, StormHandler> handlersMap = new HashMap<Method, StormHandler>();

	public List<Class<StormHandler>> getHandlersList() {
		return handlersList;
	}

	public void setHandlersList(List<Class<StormHandler>> handlersList) {
		this.handlersList = handlersList;
	}

	public void init() {
		if (!handlersList.isEmpty()) {
			Object handler = null;
			for (Class<StormHandler> c : handlersList) {
				try {
					handler = Class.forName(c.getCanonicalName()).newInstance();
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				if (handler instanceof StormHandler) {
					log.debug("object " + handler.getClass().getCanonicalName()
							+ " instantiated");
					handlersMap.put(((StormHandler) handler).getMethod(),
							(StormHandler) handler);
				} else {
					log.warn("the object "
							+ handler.getClass().getCanonicalName()
							+ " is not an instance of "
							+ StormHandler.class.getCanonicalName());
				}
			}
			handlersList.clear();
		}
	}

	public boolean handlersMapContainsKey(String methodName) {
		return handlersMap.containsKey(Enum.valueOf(Method.class, methodName));
	}

	public StormHandler handlersMapGetValue(String methodName) {
		return handlersMap.get(Enum.valueOf(Method.class, methodName));
	}

	public abstract void onEvent(Event e);

	@SuppressWarnings("rawtypes")
	public abstract Class getEventType();

}