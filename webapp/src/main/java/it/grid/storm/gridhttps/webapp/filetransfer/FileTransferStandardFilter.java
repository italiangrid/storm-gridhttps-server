/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.grid.storm.gridhttps.webapp.filetransfer;

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
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.HttpHelper;
import it.grid.storm.gridhttps.webapp.data.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormRequestFailureException;
import it.grid.storm.gridhttps.webapp.data.exceptions.StormResourceException;

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
				printCommand();
				handler.process(manager, request, response);
				if (response.getEntity() != null) {
					manager.sendResponseEntity(response);
				} else {
					log.debug("No response entity to send to client");
				}
				printExitStatus(method.name().toUpperCase(), response);
			}
		} catch (RuntimeApiException ex) {
			printErrorCommand(ex.getMessage(), ex.getReason());
			manager.getResponseHandler().respondServerError(request, response, ex.getReason().toString());
		} catch (StormRequestFailureException ex) {
			printErrorCommand(ex.getMessage(), ex.getReason());
			manager.getResponseHandler().respondServerError(request, response, ex.getReason().toString());
		} catch (StormResourceException ex) {
			printErrorCommand(ex.getMessage(), ex.getReason());
			String serverError = "<html><body><h1>Service Unavailable</h1><p>"+ex.getReason().toString()+"</p></body></html>";
			sendResponse(response, Status.SC_SERVICE_UNAVAILABLE, serverError);
		} catch (BadRequestException ex) {
			printErrorCommand(ex.getMessage(), ex.getReason());
			manager.getResponseHandler().respondBadRequest(ex.getResource(), response, request);
		} catch (ConflictException ex) {
			printErrorCommand(ex.getMessage(), "The requested operation could not be performed because of prior state.");
			manager.getResponseHandler().respondConflict(ex.getResource(), response, request, INTERNAL_SERVER_ERROR_HTML);
		} catch (NotAuthorizedException ex) {
			printErrorCommand(ex.getMessage(), "The current user is not able to perform the requested operation.");
			manager.getResponseHandler().respondUnauthorised(ex.getResource(), response, request);
		} catch (Throwable e) {
			// Looks like in some cases we can be left with a connection in an
			// indeterminate state
			// due to the content length not being equal to the content length
			// header, so
			// fall back on the udnerlying connection provider to manage the
			// error
			printErrorCommand(e.getMessage(), "exception sending content");
			response.sendError(Response.Status.SC_INTERNAL_SERVER_ERROR, INTERNAL_SERVER_ERROR_HTML);
		} finally {
			// manager.closeResponse(response);
		}
	}
	
	private void printExitStatus(String method, Response response) {
		int code = response.getStatus().code;
		String msg = method + " response: " + code + response.getStatus().text;
		if (code >= 200 && code < 300) {
			log.info(msg);
		} else if (code >= 400 && code < 600) {
			log.error(msg);
		} else {
			log.warn(msg);
		}
	}

	private String getCommand() {
		HttpHelper httpHelper = new HttpHelper(MiltonServlet.request(), MiltonServlet.response());
		String msg = "";
		msg += httpHelper.getRequestMethod();
		msg += " " + httpHelper.getRequestURI().getPath();
		if (httpHelper.hasDestinationHeader()) {
			msg += " to " + httpHelper.getDestinationURI().getPath();
		}
		return msg;
	}
	
	private void printCommand() {
		log.info(getCommand());
	}
	
	private void printErrorCommand(String message, String reason) {
		String msg = "";
		if (message != null)
			msg += message + " ";
		if (reason != null)
			msg += reason;
		log.error(getCommand() + " has failed: " + msg);
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
