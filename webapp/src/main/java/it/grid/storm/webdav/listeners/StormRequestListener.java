package it.grid.storm.webdav.listeners;


import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.event.Event;
import io.milton.event.RequestEvent;

import it.grid.storm.webdav.StormAuthorizationUtils;
import it.grid.storm.webdav.handlers.StormHandler;

@SuppressWarnings({ "rawtypes" })
public class StormRequestListener<T> extends StormEventListener {

	private static final Logger log = LoggerFactory
			.getLogger(StormRequestListener.class);

	private Class eventType = RequestEvent.class;

	private boolean isAuthorized(String methodName) {

//		String resourcePath = "/";
//		String subjectDN = "DN=C=IT, O=INFN, OU=Personal Certificate, L=Ferrara, CN=Matteo Manzali";
//		String fqans[] = {};
//		boolean ret = false;
//
//		try {
//			StormAuthorizationUtils auth = new StormAuthorizationUtils("etics-06-vm03.cnaf.infn.it",9998);
//			ret = auth.isUserAuthorized(resourcePath, methodName, subjectDN,
//					fqans);
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return ret;
		return true;
	}

	@Override
	public Class getEventType() {
		return eventType;
	}

	@Override
	public void onEvent(Event e) {
		log.debug("Triggered event " + e.getClass().getCanonicalName());

		String methodName = super.getRequestMethodName(e);
		log.debug("methodName: " + methodName);

		/* checks if the user is authorized for the requested method */
		if (isAuthorized(methodName)) {

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

}