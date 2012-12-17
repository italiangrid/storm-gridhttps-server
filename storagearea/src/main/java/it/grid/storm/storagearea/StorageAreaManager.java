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
package it.grid.storm.storagearea;

import it.grid.storm.remotecall.ConfigDiscoveryServiceConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

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
	private List<StorageArea> storageAreas;
	private static StorageAreaManager SAManager = null;
	private HashMap<String, String> fsRootFromStfn;
	private HashMap<String, String> stfnRootFromFs;

	public static StorageAreaManager getInstance() {
		return SAManager;
	}

	public static void init(String stormBEHostname, int stormBEPort) throws Exception {
		SAManager = new StorageAreaManager(stormBEHostname, stormBEPort);
	}

	public static boolean isInitialized() {
		return getInstance() != null;
	}

	private StorageAreaManager(String stormBEHostname, int stormBEPort) throws Exception {
		storageAreas = retrieveStorageAreasFromStormBackend(stormBEHostname, stormBEPort);
		fsRootFromStfn = new HashMap<String, String>();
		for (StorageArea sa : storageAreas)
			fsRootFromStfn.put(sa.getStfnRoot(), sa.getFSRoot());
		stfnRootFromFs = new HashMap<String, String>();
		for (StorageArea sa : storageAreas)
			stfnRootFromFs.put(sa.getFSRoot(), sa.getStfnRoot());
	}

	public List<StorageArea> getStorageAreas() {
		return storageAreas;
	}

	public HashMap<String, String> getStfnRootFromFs() {
		return stfnRootFromFs;
	}

	public HashMap<String, String> getFsRootFromStfn() {
		return fsRootFromStfn;
	}

	public StorageArea getStorageAreaFromStfnRoot(String stfnRoot) {
		for (StorageArea sa : getStorageAreas()) {
			if (sa.getStfnRoot().equals(stfnRoot))
				return sa;
		}
		return null;
	}

	public StorageArea getStorageAreaFromFsRoot(String fsRoot) {
		String stfnRoot = getStfnRootFromFs().get(fsRoot);
		return getStorageAreaFromStfnRoot(stfnRoot);
	}

	private List<StorageArea> retrieveStorageAreasFromStormBackend(String hostname, int port) throws Exception {
		String stormBackendIP = InetAddress.getByName(hostname).getHostAddress();
		StormBackendInfo stormBackendParameters = new StormBackendInfo(hostname, stormBackendIP, port);
		log.info("Initializing the StorageArea list");
		return populateStorageAreaConfiguration(stormBackendParameters);
	}

	private LinkedList<StorageArea> populateStorageAreaConfiguration(StormBackendInfo stormBackendParameters) throws Exception {
		URI uri = buildConfigDiscoveryServiceUri(stormBackendParameters);
		log.info("Calling Configuration Discovery service at uri: " + uri);
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

		LinkedList<StorageArea> storageAreaList = decodeStorageAreaList(output);
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
	private URI buildConfigDiscoveryServiceUri(StormBackendInfo stormBackendParameters) throws Exception {
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
	private LinkedList<StorageArea> decodeStorageAreaList(String storageAreaListString) {
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
	private List<StorageArea> decodeStorageArea(String sAEncoded) {
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
		for (String stfnRoot : stfnRootList) {
			StorageArea storageArea = new StorageArea(name, root, stfnRoot, protocolList);
			log.debug("Decoded storage area: [" + storageArea.toString() + "]");
			producedList.add(storageArea);
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
			throw new IllegalArgumentException("Provided localFilePath is null!");
		}
		if (!isInitialized()) {
			log.error("Unable to match StorageArea, class not initialized. " + "Call init() first");
			throw new IllegalStateException("Unable to match any StorageArea, class not initialized.");
		}
		log.debug("Looking for a StorageArea that matches " + localFilePath);
		StorageArea mappedSA = null;
		int matchedSAFSRootLength = 0;
		for (StorageArea storageArea : StorageAreaManager.getInstance().getStorageAreas()) {
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

	/**
	 * Searches for a storage area in the available list that has an FSRoot that
	 * is the longest match with the provided file path
	 * 
	 * @param path
	 *            must not be null
	 * @return the best match StorageArea, null if none matches
	 * @throws IllegalArgumentException
	 *             if localFilePath is null
	 * @throws ServletException
	 * @throws UnsupportedEncodingException
	 */
	public static StorageArea getMatchingSAbyURI(String path) throws IllegalArgumentException, IllegalStateException,
			UnsupportedEncodingException {
		if (path == null) {
			log.error("Unable to match StorageArea, the provided localFilePath is null");
			throw new IllegalArgumentException("Provided localFilePath is null!");
		}
		if (!isInitialized()) {
			log.error("Unable to match StorageArea, class not initialized. " + "Call init() first");
			throw new IllegalStateException("Unable to match any StorageArea, class not initialized.");
		}
		String pathDecoded;
		try {
			pathDecoded = URLDecoder.decode(path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("Unable to decode path parameter. UnsupportedEncodingException : " + e.getMessage());
			throw e;
		}
		log.debug("Decoded filePath = " + pathDecoded + " . Retrieving matching StorageArea");
		StorageArea matchedSA = null;
		log.debug("Looking for a StorageArea that matches " + pathDecoded);
		int matchedSAFSRootLength = 0;
		for (StorageArea currentSA : StorageAreaManager.getInstance().getStorageAreas()) {
			if ((pathDecoded.startsWith(currentSA.getStfnRoot()) && (matchedSA == null || currentSA.getStfnRoot().length() > matchedSA
					.getStfnRoot().length()))) {
				if (currentSA.getStfnRoot().length() > matchedSAFSRootLength) {
					matchedSA = currentSA;
					matchedSAFSRootLength = currentSA.getStfnRoot().length();
				}
			}
		}
		if (matchedSA == null) {
			log.debug("No match found");
		} else {
			log.debug("Matched StorageArea " + matchedSA.toString());
		}
		return matchedSA;
	}

}
