/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by
 * applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
 * OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package it.grid.storm.gridhttps.webapp;

import io.milton.http.Filter;
import io.milton.http.FilterChain;
import io.milton.http.Handler;
import io.milton.http.HttpManager;
import io.milton.http.Request;
import io.milton.http.Response;
import io.milton.http.Response.Status;
import io.milton.http.entity.ByteArrayEntity;
import io.milton.http.exceptions.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.servlet.MiltonServlet;
import it.grid.storm.gridhttps.webapp.common.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.common.exceptions.SRMOperationException;
import it.grid.storm.srm.types.TStatusCode;

public class StormStandardFilter implements Filter {

	private Logger log = LoggerFactory.getLogger(StormStandardFilter.class);
	public static final String INTERNAL_SERVER_ERROR_HTML = "<html><body><h1>Internal Server Error (500)</h1></body></html>";

	public StormStandardFilter() {

	}

	public void process(FilterChain chain, Request request, Response response) {

		HttpManager manager = chain.getHttpManager();
		try {
			Request.Method method = request.getMethod();
			Handler handler = manager.getMethodHandler(method);
			if (handler == null) {
				log.trace("No handler for: {}" , method);
				manager.getResponseHandler().respondMethodNotImplemented(null,
					response, request);
			} else {
				if (log.isTraceEnabled()) {
					log.trace("delegate to method handler: {}"
						, handler.getClass().getCanonicalName());
				}
				handler.process(manager, request, response);
				if (response.getEntity() != null) {
					manager.sendResponseEntity(response);
				} else {
					log.debug("No response entity to send to client");
				}
			}
		} catch (IllegalArgumentException ex) {
			log.error(ex.getMessage(), ex);
			response.sendError(Status.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
			response.setStatus(Status.SC_INTERNAL_SERVER_ERROR);
		} catch (RuntimeApiException ex) {
			log.error(ex.getMessage(), ex);
			response.sendError(Status.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
			response.setStatus(Status.SC_INTERNAL_SERVER_ERROR);
		} catch (SRMOperationException ex) {
			log.debug(ex.getMessage(),ex);
			TStatusCode sc = ex.getStatus().getStatusCode();
			if (sc.equals(TStatusCode.SRM_AUTHORIZATION_FAILURE)) {
				response.sendError(Status.SC_FORBIDDEN, ex.getStatus().toString());
				response.setStatus(Status.SC_FORBIDDEN);
			} else if (sc.equals(TStatusCode.SRM_DUPLICATION_ERROR)
				|| sc.equals(TStatusCode.SRM_FILE_BUSY)) {
				response.sendError(Status.SC_CONFLICT, ex.getStatus().toString());
				response.setStatus(Status.SC_CONFLICT);
			} else if (sc.equals(TStatusCode.SRM_INVALID_PATH)) {
				response.sendError(Status.SC_NOT_FOUND, ex.getStatus().toString());
				response.setStatus(Status.SC_NOT_FOUND);
			} else {
				response.sendError(Status.SC_INTERNAL_SERVER_ERROR, ex.getStatus()
					.toString());
				response.setStatus(Status.SC_INTERNAL_SERVER_ERROR);
			}
		} catch (RuntimeException ex) {
			log.error(ex.getMessage(), ex);
			response.sendError(Status.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
			response.setStatus(Status.SC_INTERNAL_SERVER_ERROR);
		} catch (BadRequestException ex) {
			log.warn(ex.getReason());
			manager.getResponseHandler().respondBadRequest(ex.getResource(),
				response, request);
		} catch (ConflictException ex) {
			log.warn(ex.getMessage());
			manager.getResponseHandler().respondConflict(ex.getResource(), response,
				request, INTERNAL_SERVER_ERROR_HTML);
		} catch (NotAuthorizedException ex) {
			log.warn("Authorization error: {}. Resource: {}", ex.getMessage(),
				ex.getResource());
			manager.getResponseHandler().respondUnauthorised(ex.getResource(),
				response, request);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);

			/*
			 * Looks like in some cases we can be left with a connection in an
			 * indeterminate state due to the content length not being equal to the
			 * content length header, so fall back on the underlying connection
			 * provider to manage the error
			 */
			int contentLength = Integer.valueOf(response.getHeaders().get(
				"Content-Length"));
			int entityDimension = ((ByteArrayEntity) response.getEntity()).getArr().length;
			if (contentLength != entityDimension) {
				log.warn("Response header Content-Length ({}) different from entity byte dimension ({})" , entityDimension, entityDimension);
				response.getHeaders().put("Content-Length", "" + entityDimension);				
			} else {
				log.error("{}: exception sending content" , e.getMessage(),e);
				response.sendError(Status.SC_INTERNAL_SERVER_ERROR, e.getMessage()
					+ ": exception sending content");
				response.setStatus(Status.SC_INTERNAL_SERVER_ERROR);
			}
		} finally {
			MiltonServlet.request().setAttribute("STATUS_MSG",
				response.getStatus().text);
		}
	}

}
