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
package it.grid.storm.gridhttps.webapp.filetransfer.factory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;
import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.authorization.UserCredentials;
import it.grid.storm.gridhttps.webapp.backendApi.StormBackendApi;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.RuntimeApiException;
import it.grid.storm.gridhttps.webapp.webdav.factory.exceptions.StormResourceException;
import it.grid.storm.xmlrpc.BackendApi;
import it.grid.storm.xmlrpc.outputdata.LsOutputData;
import it.grid.storm.xmlrpc.outputdata.LsOutputData.SurlInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(FileSystemResourceHelper.class);
	
	public static InputStream doGetFile(FileResource source) throws NotFoundException {
		log.debug("Called doGetFile()");
		InputStream in = null;
		try {
			in = source.contentService.getFileContent(source.file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		return in;
	}
	
	public static boolean doPutOverwrite(FileResource source, InputStream in) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.debug("Called doPutOverwrite()");
		try {
			// overwrite
			source.contentService.setFileContent(source.file, in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			throw new RuntimeException("Couldnt write to: " + source.file.getAbsolutePath(), ex);
		} 
		return true;
	}	
	
	public static ArrayList<SurlInfo> doLs(FileSystemResource source) throws RuntimeApiException, StormResourceException {
		UserCredentials user = UserCredentials.getUser();
		return doLs(source, user);
	}
	
	public static ArrayList<SurlInfo> doLs(FileSystemResource source, UserCredentials user) throws RuntimeApiException, StormResourceException {
		log.debug("Called doLs()");
		BackendApi backend = StormBackendApi.getBackend(Configuration.getBackendHostname(), Configuration.getBackendPort());
		LsOutputData output = StormBackendApi.ls(backend, source.getSurl().asString(), user);
		return (ArrayList<SurlInfo>) output.getInfos();
	}
}
