package it.grid.storm.gridhttps.server.statushandler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class StatusHandler extends AbstractHandler
{
		@Override
		public void handle(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

			if (request.getMethod().toUpperCase().equals("HEAD")
				&& (target.isEmpty() || target.equals("/")))
			{
				response.setStatus(HttpServletResponse.SC_OK);
			} else {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			baseRequest.setHandled(true);
		}
}