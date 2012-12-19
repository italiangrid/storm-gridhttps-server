package it.grid.storm.filetransfer;

import java.io.OutputStream;
import java.io.PrintWriter;

import io.milton.http.Filter;
import io.milton.http.FilterChain;
import io.milton.http.Handler;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.Response.Status;
import io.milton.http.exceptions.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import it.grid.storm.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.webdav.factory.exceptions.StormResourceException;

public class FileTransferStandardFilter implements Filter {

	private Logger log = LoggerFactory.getLogger(FileTransferStandardFilter.class);
	public static final String INTERNAL_SERVER_ERROR_HTML = "<html><body><h1>Internal Server Error (500)</h1></body></html>";

	public FileTransferStandardFilter() {
	}

	public void process(FilterChain chain, Request request, Response response) {
		HttpManager manager = chain.getHttpManager();
		try {
			Request.Method method = request.getMethod();
			Handler handler = manager.getMethodHandler(method);
			if (handler == null) {
				log.trace("No handler for: " + method);
				manager.getResponseHandler().respondMethodNotImplemented(null, response, request);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("delegate to method handler: " + handler.getClass().getCanonicalName());
				}
				handler.process(manager, request, response);
				if (response.getEntity() != null) {
					manager.sendResponseEntity(response);
				} else {
					log.debug("No response entity to send to client");
				}
			}
		} catch (RuntimeApiException ex) {
			log.error(ex.getMessage());
			manager.getResponseHandler().respondServerError(request, response, ex.getMessage());
		} catch (StormResourceException ex) {
			log.error(ex.getMessage());
			String serverError = "<html><body><h1>Service Unavailable</h1><p>"+ex.getMessage()+"</p></body></html>";
			sendResponse(response, Status.SC_SERVICE_UNAVAILABLE, serverError);
		} catch (BadRequestException ex) {
			log.warn("BadRequestException: " + ex.getReason(), ex);
			manager.getResponseHandler().respondBadRequest(ex.getResource(), response, request);
		} catch (ConflictException ex) {
			log.warn("conflictException: ", ex);
			manager.getResponseHandler().respondConflict(ex.getResource(), response, request, INTERNAL_SERVER_ERROR_HTML);
		} catch (NotAuthorizedException ex) {
			log.warn("NotAuthorizedException", ex);
			manager.getResponseHandler().respondUnauthorised(ex.getResource(), response, request);
		} catch (Throwable e) {
			// Looks like in some cases we can be left with a connection in an
			// indeterminate state
			// due to the content length not being equal to the content length
			// header, so
			// fall back on the udnerlying connection provider to manage the
			// error
			log.error("exception sending content", e);
			response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_HTML);
		} finally {
			// manager.closeResponse(response);
		}
	}
	
	private void sendResponse(Response response, Status status, final String htmlPage) {
		response.setStatus(status);
		response.setEntity(new Response.Entity() {
            public void write(Response response, OutputStream outputStream) throws Exception {
                PrintWriter pw = new PrintWriter(outputStream, true);
                pw.print(htmlPage);
                pw.flush();
            }
        });
	}
}
