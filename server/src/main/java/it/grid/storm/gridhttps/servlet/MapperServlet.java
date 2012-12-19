/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2010.
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
package it.grid.storm.gridhttps.servlet;

//import it.grid.storm.gridhttps.Configuration;
import it.grid.storm.storagearea.StorageArea;
import it.grid.storm.storagearea.StorageAreaManager;

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
 */
public class MapperServlet extends HttpServlet {

	private static Logger log = LoggerFactory.getLogger(MapperServlet.class);
	/**
     * 
     */
	private static final long serialVersionUID = 293463225950571516L;
	private static final String PATH_PARAMETER_KEY = "path";
	private static final String MAPPER_SERVLET_ENCODING_SCHEME = "UTF-8";
	public static final String MAPPER_SERVLET_CONTEXT_PATH = "/fileTransfer";

	public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		log.info("Serving a get request");
		String pathDecoded = getDecodedPath(req);
		log.debug("Decoded filePath = " + pathDecoded + " . Retrieving matching StorageArea");
		StorageArea SA = getMatchingSA(pathDecoded);
		if (SA == null) {
			log.error("No matching StorageArea found for path \'" + pathDecoded + "\' Unable to build http(s) relative path");
			throw new ServletException("No matching StorageArea found for the provided path");
		}
		String relativeUrl = getStfnPath(pathDecoded, SA);
		log.debug("Writing in the response the relative URL : " + relativeUrl);
		sendResponse(res, relativeUrl);
	}

	private String getDecodedPath(HttpServletRequest request) throws ServletException {
		String path = request.getParameter(PATH_PARAMETER_KEY);
		String pathDecoded;
		try {
			pathDecoded = URLDecoder.decode(path, MAPPER_SERVLET_ENCODING_SCHEME);
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to decode " + PATH_PARAMETER_KEY + " parameter. UnsupportedEncodingException : " + e.getMessage());
			throw new ServletException("Unable to decode " + PATH_PARAMETER_KEY + " parameter", e);
		}
		return pathDecoded;
	}
	
	private StorageArea getMatchingSA(String pathDecoded) throws ServletException {
		StorageArea SA = null;
		try {
			SA = StorageAreaManager.getMatchingSA(new File(pathDecoded));
		} catch (IllegalArgumentException e) {
			log.error("Unable to get matching SA for path " + pathDecoded + ". IllegalArgumentException : " + e.getMessage());
			throw new ServletException("Unable to get matching SA for path " + pathDecoded, e);
		} catch (IllegalStateException e) {
			log.error("Unable to get matching SA for path " + pathDecoded + ". IllegalStateException : " + e.getMessage());
			throw new ServletException("Unable to get matching SA for path " + pathDecoded, e);
		}
		return SA;
	}
	
	private String getStfnPath(String path, StorageArea SA) {
		log.debug("Building StfnPath for path " + path + " in StorageArea " + SA.getName());
		String Stfnpath = MAPPER_SERVLET_CONTEXT_PATH + SA.getStfnRoot() + path.substring(SA.getFSRoot().length(), path.length());
		log.debug("Stfnpath is \'" + Stfnpath + "\'");
		return Stfnpath;
	}

	private void sendResponse(HttpServletResponse response, String message) throws IOException {
		response.setContentType("text/html");
		PrintWriter out;
		try {
			out = response.getWriter();
		} catch (IOException e) {
			log.error("Unable to obtain the PrintWriter for the response. IOException: " + e.getMessage());
			throw e;
		}
		out.print(message);
	}
	
	public String getServletInfo() {
		return "A servlet providing a mapping between a fisical file path and it\'s relative URL";
	}
}
