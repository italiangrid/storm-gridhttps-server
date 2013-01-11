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
import it.grid.storm.gridhttps.webapp.utils.Chronometer.ElapsedTime;

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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormContentService implements FileContentService {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);

	public void setFileContent(File file, InputStream in) throws FileNotFoundException, IOException {
		OutputStream out = new FileOutputStream(file);
		Chronometer chrono = new Chronometer();
		if (Configuration.getComputeChecksum()) {
			ChecksumAlgorithm algorithm = null;
			try {
				algorithm = getChecksumAlgorithm(Configuration.getChecksumType());
			} catch (NoSuchAlgorithmException e) {
				log.error(e.getMessage());
			}
			if (algorithm != null) {
				String checksum = null;
				log.debug("Computing " + algorithm.getType().name());
				try {
					chrono.start();
					checksum = algorithm.compute(in, out);
					chrono.stop();
					IOUtils.closeQuietly(out);
					sendChecksum(file, algorithm.getType(), checksum);
				} catch (ChecksumReadException e) {
					log.error(e.getMessage());
					log.error("Impossible to terminate file transfer!");
					log.warn("Trying to transfer file without checksum...");
					in.reset();
					chrono.start();
					doSimpleSetFileContent(in, out);
					chrono.stop();
				} finally {
					log.debug("Checksum: " + checksum);
				}
			} else {
				log.warn("Checksum algorithm '" + Configuration.getChecksumType() + "' not supported! Proceeding with nochecksum file transfer...");
				chrono.start();
				doSimpleSetFileContent(in, out);
				chrono.stop();
			}
		} else {
			chrono.start();
			doSimpleSetFileContent(in, new FileOutputStream(file));
			chrono.stop();
		}
		ElapsedTime elapsed = chrono.getElapsedTime();
		log.debug("ELAPSED TIME: " + elapsed.getMinutes() + "':" + elapsed.getSeconds() + "'':" + elapsed.getMilliseconds());
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
		HttpResponse response = null;
		try {
			response = callSetChecksumService(buildSetChecksumValueUri(file, type, checksum));
			log.info(response.getEntity().toString());
		} catch (Exception e) {
			log.error(e.getMessage());
		}
	}
	
	public URI buildSetChecksumValueUri(File target, ChecksumType type, String checksum) throws UnsupportedEncodingException, URISyntaxException {
		String encodedFilename = URLEncoder.encode(target.getAbsolutePath(), "UTF-8");
		String path = "/" + Constants.RESOURCE + "/" + Constants.VERSION + "/" + encodedFilename + "/" + type.toString() + "?" + Constants.CHECKSUM_VALUE_KEY + "=" + checksum;
		URI uri = new URI("http", null, Configuration.getBackendHostname(), Configuration.getBackendServicePort(), path, null, null);
		log.debug("Built set checksum value URI: " + uri);
		return uri;
	}

	private HttpResponse callSetChecksumService(URI uri) throws Exception {
		log.info("Calling set checksum service at uri: " + uri);
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse = null;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException " + e.getLocalizedMessage());
			throw new Exception("Error contacting set checksum service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException " + e.getLocalizedMessage());
			throw new Exception("Error contacting set checksum service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new Exception("Unexpected error! response.getStatusLine() returned null! Please contact storm support");
		}
		int httpCode = status.getStatusCode();
		String httpMessage = status.getReasonPhrase();
		log.debug("Http call return code is: " + httpCode);
		log.debug("Http call return reason phrase is: " + httpMessage);
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : \'" + httpCode + "\' "
					+ httpMessage);
			throw new Exception("Unable to get a valid response from server. " + httpMessage);
		}
		return httpResponse;
	}
	
}
