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
package it.grid.storm.gridhttps.server.mapperservlet;

import it.grid.storm.gridhttps.common.storagearea.StorageArea;
import it.grid.storm.gridhttps.common.storagearea.StorageAreaManager;
import it.grid.storm.gridhttps.configuration.DefaultConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Michele Dibenedetto
 * @author Enrico Vianello
 */
public class MapperServlet extends HttpServlet {

	private static Logger log = LoggerFactory.getLogger(MapperServlet.class);
	/**
     * 
     */
	private static final long serialVersionUID = 293463225950571516L;
	private static final String PATH_PARAMETER_KEY = "path";
	private static final String MAPPER_SERVLET_ENCODING_SCHEME = "UTF-8";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.debug("Serving a mapping request");
		String pathDecoded = getDecodedPath(request);
		log.debug("Decoded filePath = {}  . Retrieving matching StorageArea" , pathDecoded);
		StorageArea SA = getMatchingSA(pathDecoded);
		log.debug("Storage-Area: {}" , SA);
		String relativeUrl = File.separator + DefaultConfiguration.WEBAPP_FILETRANSFER_CONTEXTPATH + SA.getStfn(pathDecoded);
		log.info("GET-MAPPING: '{}' to '{}'" , pathDecoded , relativeUrl);
		sendResponse(response, relativeUrl);
	}
	
	@Override
	public void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.info("HEAD-MAPPING: {}" , request.getRequestURI());
	}
	
	
	private String getDecodedPath(HttpServletRequest request) throws ServletException {
		String path = request.getParameter(PATH_PARAMETER_KEY);
		String pathDecoded;
		try {
			pathDecoded = URLDecoder.decode(path, MAPPER_SERVLET_ENCODING_SCHEME);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to decode {} parameter. UnsupportedEncodingException : {}" , PATH_PARAMETER_KEY ,  e.getMessage(),e);
			throw new ServletException("Unable to decode " + PATH_PARAMETER_KEY + " parameter", e);
		}
		return pathDecoded;
	}
	
	private StorageArea getMatchingSA(String pathDecoded) throws ServletException {
		StorageArea SA = null;
		try {
			SA = StorageAreaManager.getMatchingSAFromFsPath(pathDecoded);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			throw new ServletException(e.getMessage(), e);
		}
		if (SA == null) {
			log.error("No matching StorageArea found for path '{}'", pathDecoded);
			throw new ServletException("No matching StorageArea found for the provided path");
		}
		return SA;
	}
	
	private void sendResponse(HttpServletResponse response, String message) throws IOException {
		response.setContentType("text/html");
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (IOException e) {
			log.error("Unable to obtain the PrintWriter for the response. IOException: {}" , e.getMessage(),e);
			throw e;
		}
		out.print(message);
	}
	
}