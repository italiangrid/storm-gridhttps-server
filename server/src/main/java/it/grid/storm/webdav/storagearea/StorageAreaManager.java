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
package it.grid.storm.webdav.storagearea;

import it.grid.storm.webdav.remotecall.ConfigDiscoveryServiceConstants;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StorageAreaManager {

	private static final Logger log = LoggerFactory.getLogger(StorageAreaManager.class);

	/**
	 * The list of all storage areas configured at storm backend NOTE: if storm
	 * backend is restarted gridhttps server must be restarted too!
	 */
	private static List<StorageArea> storageAreas = null;

	public static List<StorageArea> getStorageAreas() {
		return storageAreas;
	}

	/**
	 * Avoid instantiation
	 */
	private StorageAreaManager() {
	}

	/**
	 * @return true if a list of storage areas is available
	 */
	public static boolean initialized() {
		return StorageAreaManager.storageAreas != null;
	}

	/**
	 * Initializes the class with a list of storage areas
	 * 
	 * @param SAList
	 */
	public static void init(List<StorageArea> SAList) {
		if (storageAreas == null) {
			log.info("Initializing the Storage Area manager with following StorageAreas: " + SAList.toString());
			storageAreas = SAList;
		}
	}

	public static void initFromStormBackend(String hostname, int port) throws Exception {
		String stormBackendIP = InetAddress.getByName(hostname).getHostAddress();
		ConfigurationParameters stormBackendParameters = new ConfigurationParameters(hostname, stormBackendIP, port);
		log.info("Initializing the StorageArea list");
		storageAreas = populateStorageAreaConfiguration(stormBackendParameters);
	}

	/**
	 * @param stormBackendParameters
	 * @return
	 * @throws ServletException
	 */
	private static LinkedList<StorageArea> populateStorageAreaConfiguration(final ConfigurationParameters stormBackendParameters)
			throws Exception {
		URI uri = buildConfigDiscoveryServiceUri(stormBackendParameters);
		log.info("Calling Configuration Discovery service at uri: ");
		log.info(uri.toString());
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException " + e.getLocalizedMessage());
			throw new Exception("Error contacting Configuration Discovery service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException " + e.getLocalizedMessage());
			throw new Exception("Error contacting Configuration Discovery service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new Exception("Unexpected error! response.getStatusLine() returned null! Please contact storm support");
		}
		int httpCode = status.getStatusCode();
		log.debug("Http call return code is: " + httpCode);
		String httpMessage = status.getReasonPhrase();
		log.debug("Http call return reason phrase is: " + httpMessage);
		HttpEntity entity = httpResponse.getEntity();
		String output = "";
		if (entity != null) {
			InputStream responseIS;
			try {
				responseIS = entity.getContent();
			} catch (IllegalStateException e) {
				log.error("Unable to get the input content stream from server answer. IllegalStateException " + e.getLocalizedMessage());
				throw new Exception("Error comunicationg with the Configuration Discovery service.");
			} catch (IOException e) {
				log.error("Unable to get the input content stream from server answer. IOException " + e.getLocalizedMessage());
				throw new Exception("Error comunicationg with the Configuration Discovery service.");
			}
			int l;
			byte[] tmp = new byte[1024];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output += new String(tmp, 0, l);
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException " + e.getMessage());
				throw new Exception("Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new Exception("Unable to get a valid configuration discovery response from the server.");
		}
		log.debug("Response is : \'" + output + "\'");
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : \'" + httpCode + "\' "
					+ httpMessage);
			throw new Exception("Unable to get a valid response from server. " + httpMessage);
		}
		log.debug("Decoding the receive response");

		/* DA RIMUOVERE!!! */
		// String output =
		// "name=DTEAMT0D1-FS&root=/storage/dteamt0d1&stfnRoot=/dteamt0d1;/dteam&protocols=https:name=ATLAST0D1-FS&root=/storage/atlast0d1&stfnRoot=/atlast0d1&protocols=https;http";

		LinkedList<StorageArea> storageAreaList = decodeStorageAreaList(output);
		// log.debug("Initializing the Storage Area Manager");
		// StorageAreaManager.init(storageAreaList);
		return storageAreaList;
	}

	/**
	 * Builds the URI of the configuration discovery service
	 * 
	 * @param stormBackendParameters
	 * 
	 * @return
	 * @throws ServletException
	 */
	private static URI buildConfigDiscoveryServiceUri(final ConfigurationParameters stormBackendParameters) throws Exception {
		log.debug("Building configurationd discovery rest service URI");
		String path = "/" + ConfigDiscoveryServiceConstants.RESOURCE + "/" + ConfigDiscoveryServiceConstants.VERSION + "/"
				+ ConfigDiscoveryServiceConstants.LIST_ALL_KEY;
		URI uri;
		try {
			uri = new URI("http", null, stormBackendParameters.getStormBackendHostname(), stormBackendParameters.getStormBackendRestPort(),
					path, null, null);
		} catch (URISyntaxException e) {
			log.error("Unable to create Configuration Discovery URI. URISyntaxException " + e.getLocalizedMessage());
			throw new Exception("Unable to create Configuration Discovery URI");
		}
		log.debug("Built configuration discovery URI: " + uri);
		return uri;
	}

	/**
	 * @param storageAreaListString
	 * @return never null, a list that contains the decoded storage areas. None
	 *         of the elements can be null
	 */
	private static LinkedList<StorageArea> decodeStorageAreaList(String storageAreaListString) {
		if (storageAreaListString == null) {
			log.error("Decoding failed, received a null storage area list string!");
			throw new IllegalArgumentException("Received a null storage area list string");
		}
		LinkedList<StorageArea> local = new LinkedList<StorageArea>();
		String[] SAEncodedArray = storageAreaListString.trim().split("" + ConfigDiscoveryServiceConstants.VFS_LIST_SEPARATOR);
		log.debug("Decoding " + SAEncodedArray.length + " storage areas");
		for (String SAEncoded : SAEncodedArray) {
			local.addAll(decodeStorageArea(SAEncoded));
		}
		return local;
	}

	/**
	 * Given a strings decodes the string in one or more StorageArea instances
	 * 
	 * @param sAEncoded
	 * @return a list of StorageArea instances, never null. None of the elements
	 *         can be null
	 */
	private static List<StorageArea> decodeStorageArea(String sAEncoded) {
		if (sAEncoded == null) {
			log.error("Decoding failed, received a null encoded storage area!");
			throw new IllegalArgumentException("Received a null encoded storage area");
		}
		log.debug("Deconding storage area string \'" + sAEncoded + "\'");
		LinkedList<StorageArea> producedList = new LinkedList<StorageArea>();
		String name = null;
		String root = null;
		List<String> stfnRootList = new LinkedList<String>();
		List<String> protocolList = new LinkedList<String>();
		String[] SAFields = sAEncoded.trim().split("" + ConfigDiscoveryServiceConstants.VFS_FIELD_SEPARATOR);
		for (String SAField : SAFields) {
			String[] keyValue = SAField.trim().split("" + ConfigDiscoveryServiceConstants.VFS_FIELD_MATCHER);
			if (ConfigDiscoveryServiceConstants.VFS_NAME_KEY.equals(keyValue[0])) {
				name = keyValue[1];
				log.debug("Found name: " + name);
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_ROOT_KEY.equals(keyValue[0])) {
				root = keyValue[1];
				log.debug("Found File System Root: " + root);
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_STFN_ROOT_KEY.equals(keyValue[0])) {
				String[] stfnRootArray = keyValue[1].trim().split("" + ConfigDiscoveryServiceConstants.VFS_STFN_ROOT_SEPARATOR);
				for (String stfnRoot : stfnRootArray) {
					stfnRootList.add(stfnRoot);
					log.debug("Found Storage File Name Root: " + stfnRoot);
				}
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_ENABLED_PROTOCOLS_KEY.equals(keyValue[0])) {
				String[] protocolsArray = keyValue[1].trim().split("" + ConfigDiscoveryServiceConstants.VFS_ENABLED_PROTOCOLS_SEPARATOR);
				for (String protocol : protocolsArray) {
					protocolList.add(protocol);
					log.debug("Found Storage WebDAV protocol: " + protocol);
				}
				continue;
			}
		}
		if (name == null || root == null || stfnRootList.size() == 0) {
			log.warn("Unable to decode the storage area. Some fileds are missin: name=" + name + " FSRoot=" + root + " stfnRootList="
					+ stfnRootList + " protocolList=" + protocolList);
			throw new IllegalArgumentException("");
		}
		int protocol = StorageArea.NONE_PROTOCOL;
		if (protocolList.size() > 0) {
			if ((protocolList.contains("http")) && (protocolList.contains("https")))
				protocol = StorageArea.HTTP_AND_HTTPS_PROTOCOLS;
			else if (protocolList.contains("http"))
				protocol = StorageArea.HTTP_PROTOCOL;
			else if (protocolList.contains("https"))
				protocol = StorageArea.HTTPS_PROTOCOL;
		}
		for (String stfnRoot : stfnRootList) {
			log.debug("Decoded storage area: [" + name + "," + root + "," + stfnRoot + ", " + StorageArea.protocolToStr(protocol) + "]");
			producedList.add(new StorageArea(name, root, stfnRoot, protocol));
		}
		log.debug("Decoded " + producedList.size() + " storage areas");
		return producedList;
	}

	/**
	 * Searches for a storage area in the available list that has an FSRoot that
	 * is the longest match with the provided file path
	 * 
	 * @param localFilePath
	 *            must not be null
	 * @return the best match StorageArea, null if none matches
	 * @throws IllegalArgumentException
	 *             if localFilePath is null
	 */
	public static StorageArea getMatchingSA(String localFilePath) throws IllegalArgumentException, IllegalStateException {
		if (localFilePath == null) {
			log.error("Unable to match StorageArea, the provided localFilePath is null");
			throw new IllegalArgumentException("Provided localFilePath il null!");
		}
		if (!initialized()) {
			log.error("Unable to match StorageArea, class not initialized. " + "Call init() first");
			throw new IllegalStateException("Unable to match any StorageArea, class not initialized.");
		}
		log.debug("Looking for a StorageArea that matches " + localFilePath);
		StorageArea mappedSA = null;
		int matchedSAFSRootLength = 0;
		for (StorageArea storageArea : storageAreas) {
			if (localFilePath.startsWith(storageArea.getFSRoot())
					&& (mappedSA == null || storageArea.getFSRoot().length() > mappedSA.getFSRoot().length())) {
				if (storageArea.getStfnRoot().length() > matchedSAFSRootLength) {
					mappedSA = storageArea;
					matchedSAFSRootLength = storageArea.getStfnRoot().length();
				}
			}
		}
		if (mappedSA == null) {
			log.debug("No match found");
		} else {
			log.debug("Matched StorageArea " + mappedSA.toString());
		}
		return mappedSA;
	}
}
