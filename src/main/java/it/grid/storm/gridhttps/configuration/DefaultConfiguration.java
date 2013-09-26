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
package it.grid.storm.gridhttps.configuration;

public class DefaultConfiguration {

	/* mapper servlet */
	public final static String 		MAPPER_SERVLET_CONTEXT_PATH = "gridhttps_webapp";
	public final static String 		MAPPER_SERVLET_CONTEXT_SPEC = "resourceMapping";

	/* storm backend */
	public final static int 		STORM_BE_PORT = 8080;
	public final static int 		STORM_BE_SERVICE_PORT = 9998;

	/* storm frontend */
	public final static int 		STORM_FE_PORT = 8444;

	/* storm gridhttps-server configuration*/
	public final static int 		SERVER_MAPPER_SERVLET_PORT = 8086;
	public final static boolean 	SERVER_WEBAPP_USE_HTTP = true;
	public final static int 		SERVER_WEBAPP_HTTP_PORT = 8085;
	public final static int 		SERVER_WEBAPP_HTTPS_PORT = 8443;
	public final static boolean 	SERVER_WEBAPP_HTTPS_WANT_CLIENT_AUTH = true;
	public final static boolean 	SERVER_WEBAPP_HTTPS_NEED_CLIENT_AUTH = true;
	public final static String 		SERVER_WEBAPP_HTTPS_CERTIFICATE_FILE = "/etc/grid-security/gridhttps/hostcert.pem";
	public final static String 		SERVER_WEBAPP_HTTPS_KEY_FILE = "/etc/grid-security/gridhttps/hostkey.pem";
	public final static String 		SERVER_WEBAPP_HTTPS_TRUST_STORE_DIRECTORY = "/etc/grid-security/certificates";
	public final static long 		SERVER_WEBAPP_TRUST_STORE_REFRESH_INTERVAL = 600000L;
		
	public final static int 		SERVER_ACTIVE_THREADS_MAX = 150;
	public final static int 		SERVER_QUEUED_THREADS_MAX = 300;
	
	
	/* webapp (webdav+filetransfer) */
	
	public final static String 		WEBAPP_WEBDAV_CONTEXTPATH = "/";
	public final static String 		WEBAPP_FILETRANSFER_CONTEXTPATH = "fileTransfer";
	public final static String 		WEBAPP_GPFS_ROOTDIRECTORY = "/";
	public final static boolean 	WEBAPP_COMPUTE_CHECKSUM = true;
	public final static String 		WEBAPP_CHECKSUM_TYPE = "adler32";
	
	/* general */
	public final static String 		LOG_FILE = "/etc/storm/gridhttps-server/logging.xml";

}
