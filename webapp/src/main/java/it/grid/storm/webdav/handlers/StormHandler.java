
package it.grid.storm.webdav.handlers;

import io.milton.event.Event;
import io.milton.http.Request.Method;

public interface StormHandler {
	public void exec(Event e);
	public Method getMethod();
}
