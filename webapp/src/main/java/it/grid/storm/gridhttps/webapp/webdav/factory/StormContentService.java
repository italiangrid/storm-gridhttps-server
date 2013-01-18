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
package it.grid.storm.gridhttps.webapp.webdav.factory;

import io.milton.http.fs.FileContentService;
import it.grid.storm.ea.remote.Constants;
import it.grid.storm.gridhttps.webapp.Configuration;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumReadException;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumType;
import it.grid.storm.gridhttps.webapp.checksum.algorithm.Adler32ChecksumAlgorithm;
import it.grid.storm.gridhttps.webapp.checksum.algorithm.CRC32ChecksumAlgorithm;
import it.grid.storm.gridhttps.webapp.checksum.algorithm.ChecksumAlgorithm;
import it.grid.storm.gridhttps.webapp.checksum.algorithm.MDChecksumAlgorithm;
import it.grid.storm.gridhttps.webapp.utils.Chronometer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormContentService implements FileContentService {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);
	
	public void setFileContent(File file, InputStream in) throws FileNotFoundException, IOException {
		OutputStream out = new FileOutputStream(file);
		Chronometer chrono = new Chronometer();
		if (Configuration.getComputeChecksum()) {
			String checksum = null;
			ChecksumAlgorithm algorithm = null;
			try {
				algorithm = getChecksumAlgorithm(Configuration.getChecksumType());
				chrono.start();
				checksum = algorithm.compute(in, out);
				chrono.stop();
				log.debug("Checksum: " + checksum);
				IOUtils.closeQuietly(out);
				sendChecksum(file, algorithm.getType(), checksum);
			} catch (NoSuchAlgorithmException e) {
				log.error(e.getMessage());
				log.warn("Checksum algorithm '" + Configuration.getChecksumType() + "' not supported!");
				log.debug("Proceeding with nochecksum file transfer...");
				chrono.start();
				doSimpleSetFileContent(in, out);
				chrono.stop();
			} catch (ChecksumReadException e) {
				log.error(e.getMessage());
				log.error("File transfer is failed!");
				log.warn("Trying to transfer file without checksum...");
				in.reset();
				chrono.start();
				doSimpleSetFileContent(in, out);
				chrono.stop();
			}
		} else {
			chrono.start();
			doSimpleSetFileContent(in, out);
			chrono.stop();
		}
		log.debug("File-transfer time: " + chrono.getElapsedTime());
	}

	public InputStream getFileContent(File file) throws FileNotFoundException {
		FileInputStream fin = new FileInputStream(file);
		return fin;
	}
	
	private void doSimpleSetFileContent(InputStream in, OutputStream out) throws FileNotFoundException, IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
	
	private ChecksumAlgorithm getChecksumAlgorithm(String checksumTypeStr) throws NoSuchAlgorithmException {
		ChecksumType checksumType = ChecksumType.getChecksumAlgorithm(checksumTypeStr);
		if (checksumType == null)
			throw new NoSuchAlgorithmException(checksumTypeStr + " not a valid checksum algorithm!");
		if (checksumType.equals(ChecksumType.ADLER32)) {
			return new Adler32ChecksumAlgorithm();
		} else if (checksumType.equals(ChecksumType.CRC32)) {
			return new CRC32ChecksumAlgorithm();
		} else {
			return new MDChecksumAlgorithm(checksumType.name());
		}
	}
	
	private void sendChecksum(File file, ChecksumType type, String checksum) {
		log.debug("Set checksum " + type.name() + " = " + checksum + " to file " + file.getAbsolutePath());
		try {
			HttpResponse response = callSetChecksumService(buildSetChecksumValueUri(file, type, checksum));
			StatusLine status = response.getStatusLine();
			if (status != null) {
				log.debug("Http call return code is: " + status.getStatusCode());
				log.debug("Http call return reason phrase is: " + status.getReasonPhrase());
				if (status.getStatusCode() == HttpURLConnection.HTTP_NO_CONTENT) {
					log.debug("Checksum successfully set!");
				} else {
					throw new Exception("Unable to get a valid response from server. Received a non HTTP 204 response from the server!");
				}
			} else { 
				throw new Exception("Unexpected error! response.getStatusLine() returned null!");
			}
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
	
	public URI buildSetChecksumValueUri(File target, ChecksumType type, String checksum) throws UnsupportedEncodingException, URISyntaxException {
		String encodedFilename = URLEncoder.encode(target.getAbsolutePath(), "UTF-8");
		String path = "/" + Constants.RESOURCE + "/" + Constants.VERSION + "/" + encodedFilename + "/" + type.toString();
		String query = Constants.CHECKSUM_VALUE_KEY + "=" + checksum;
		URI uri = new URI("http", null, Configuration.getBackendHostname(), Configuration.getBackendServicePort(), path, query, null);
		log.debug("Built checksum service URI: " + uri);
		return uri;
	}

	private HttpResponse callSetChecksumService(URI uri) throws Exception {
		log.debug("Put checksum value at uri: " + uri);
		HttpPut httpput = new HttpPut(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse = null;
		try {
			httpResponse = httpclient.execute(httpput);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException " + e.getLocalizedMessage());
			throw new Exception("Error contacting set checksum service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException " + e.getLocalizedMessage());
			throw new Exception("Error contacting set checksum service.");
		}
		return httpResponse;
	}
	
}
