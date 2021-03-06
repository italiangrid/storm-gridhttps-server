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
package it.grid.storm.gridhttps.common.storagearea;

import it.grid.storm.gridhttps.common.remotecall.ConfigDiscoveryServiceConstants;
import it.grid.storm.gridhttps.common.remotecall.ConfigDiscoveryServiceConstants.HttpPerms;
import it.grid.storm.gridhttps.webapp.common.authorization.Constants;
import it.grid.storm.gridhttps.webapp.common.authorization.StormAuthorizationUtils;
import it.grid.storm.gridhttps.webapp.common.authorization.UserCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
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

	private final String beHostname;
	private final int bePort;

	private List<StorageArea> storageAreas;
	private static StorageAreaManager SAManager = null;
	private HashMap<String, String> fsRootFromStfn;
	private HashMap<String, String> stfnRootFromFs;

	public static void init(String stormBEHostname, int stormBEPort) throws Exception {
		SAManager = new StorageAreaManager(stormBEHostname, stormBEPort);
	}

	public static boolean isInitialized() {
		return getInstance() != null;
	}

	public static StorageAreaManager getInstance() {
		return SAManager;
	}

	public String getBeHostname() {
		return beHostname;
	}

	public int getBePort() {
		return bePort;
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
	
	public static StorageArea getMatchingSAFromFsPath(String filePath)
		throws IllegalArgumentException, IllegalStateException {
		
		if (filePath == null) {
			log.error("Unable to match StorageArea: filePath is null");
			throw new IllegalArgumentException("filePath is null!");
		}
		if (!isInitialized()) {
			log.error("Unable to match StorageArea: class not initialized. Call init() first");
			throw new IllegalStateException("Unable to match any StorageArea, class not initialized.");
		}

		log.debug("Looking for a StorageArea that matches {}", filePath);
		StorageArea mapped = null;
		for (StorageArea storageArea : StorageAreaManager.getInstance().getStorageAreas()) {
			if (filePath.startsWith(storageArea.getFSRoot())) {
					if (mapped == null || 
						storageArea.getFSRoot().length() > mapped.getFSRoot().length()) {
						mapped = storageArea;
						log.debug("Temporary matched StorageArea {}", mapped.getName());
					}
			}
		}
		if (mapped == null) {
			log.debug("No match found");
		} else {
			log.debug("Final matched StorageArea {}", mapped.getName());
		}
		return mapped;
	}

	public static StorageArea getMatchingSA(URI uri) throws IllegalArgumentException, IllegalStateException {
		if (uri == null) {
			log.error("Unable to match StorageArea, the provided uri is null");
			throw new IllegalArgumentException("Provided uri is null!");
		}
		return getMatchingSA(uri.getPath());
	}
	
	public static StorageArea getMatchingSA(String uriPath) throws IllegalArgumentException, IllegalStateException {
		if (uriPath == null) {
			log.error("Unable to match StorageArea, the provided uriPath is null");
			throw new IllegalArgumentException("Provided uriPath is null!");
		}
		if (!isInitialized()) {
			log.error("Unable to match StorageArea, class not initialized. Call init() first");
			throw new IllegalStateException("Unable to match any StorageArea, class not initialized.");
		}
		StorageArea matched = null;
		log.debug("Looking for a StorageArea that matches {}", uriPath);
		for (StorageArea storageArea : StorageAreaManager.getInstance().getStorageAreas()) {
			if (uriPath.startsWith(storageArea.getStfnRoot())) {
				if (matched == null || storageArea.getStfnRoot().length() > matched.getStfnRoot().length()) {
					matched = storageArea;				
				}
			}
		}
		if (matched == null) {
			log.debug("No match found");
		} else {
			log.debug("Matched StorageArea {}" , matched.toString());
		}
		return matched;
	}
	
	private StorageAreaManager(String stormBEHostname, int stormBEPort) throws Exception {
		this.beHostname = stormBEHostname;
		this.bePort = stormBEPort;
		this.storageAreas = retrieveStorageAreasFromStormBackend(stormBEHostname, stormBEPort);
		fsRootFromStfn = new HashMap<String, String>();
		for (StorageArea sa : storageAreas)
			fsRootFromStfn.put(sa.getStfnRoot(), sa.getFSRoot());
		stfnRootFromFs = new HashMap<String, String>();
		for (StorageArea sa : storageAreas)
			stfnRootFromFs.put(sa.getFSRoot(), sa.getStfnRoot());
	}

	private List<StorageArea> retrieveStorageAreasFromStormBackend(String beHostname, int bePort) throws Exception {
		log.info("Initializing StorageAreaManager from {}, {}", beHostname,bePort);

		URI uri = buildConfigDiscoveryServiceUri(beHostname, bePort);
		HttpResponse httpResponse = callConfigDiscoveryService(uri);
		String output = getResponseBodyAsString(httpResponse.getEntity());
		ArrayList<StorageArea> storageAreaList = decodeStorageAreaList(output);
		return storageAreaList;
	}

	private String getResponseBodyAsString(HttpEntity entity) throws Exception {
		String output = "";
		if (entity != null) {
			InputStream responseIS;
			try {
				responseIS = entity.getContent();
			} catch (IllegalStateException e) {
				log.error("Unable to get the input content stream from server answer. IllegalStateException {}" , e.getMessage(),e);
				throw new Exception("Error comunicationg with the Configuration Discovery service.");
			} catch (IOException e) {
				log.error("Unable to get the input content stream from server answer. IOException {}" , e.getMessage(),e);
				throw new Exception("Error comunicationg with the Configuration Discovery service.");
			}
			int l;
			byte[] tmp = new byte[1024];
			try {
				while ((l = responseIS.read(tmp)) != -1) {
					output += new String(tmp, 0, l);
				}
			} catch (IOException e) {
				log.error("Error reading from the connection error stream. IOException {}" , e.getMessage(),e);
				throw new Exception("Error comunicationg with the authorization service.");
			}
		} else {
			log.error("No HttpEntity found in the response. Unable to determine the answer");
			throw new Exception("Unable to get a valid configuration discovery response from the server.");
		}
		log.debug("Response is : {}" , output);
		return output;
	}

	private URI buildConfigDiscoveryServiceUri(String beHostname, int bePort) throws Exception {
		log.debug("Building configurationd discovery rest service URI");
		String path = "/" + ConfigDiscoveryServiceConstants.RESOURCE + "/" + ConfigDiscoveryServiceConstants.VERSION + "/"
				+ ConfigDiscoveryServiceConstants.LIST_ALL_KEY;
		URI uri;
		try {
			uri = new URI("http", null, beHostname, bePort, path, null, null);
		} catch (URISyntaxException e) {
			log.error("Unable to create Configuration Discovery URI. URISyntaxException {}" , e.getMessage(),e);
			throw new Exception("Unable to create Configuration Discovery URI");
		}
		log.debug("Built configuration discovery URI: {}" , uri);
		return uri;
	}

	private HttpResponse callConfigDiscoveryService(URI uri) throws Exception {
		log.info("Calling Configuration Discovery service at uri: {}" , uri);
		HttpGet httpget = new HttpGet(uri);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse httpResponse = null;
		try {
			httpResponse = httpclient.execute(httpget);
		} catch (ClientProtocolException e) {
			log.error("Error executing http call. ClientProtocolException {}" , e.getMessage(),e);
			throw new Exception("Error contacting Configuration Discovery service.");
		} catch (IOException e) {
			log.error("Error executing http call. IOException {}" , e.getMessage(),e);
			throw new Exception("Error contacting Configuration Discovery service.");
		}
		StatusLine status = httpResponse.getStatusLine();
		if (status == null) {
			// never return null
			log.error("Unexpected error! response.getStatusLine() returned null!");
			throw new Exception("Unexpected error! response.getStatusLine() returned null! Please contact storm support");
		}
		int httpCode = status.getStatusCode();
		String httpMessage = status.getReasonPhrase();
		log.debug("Http call return code is: {}" , httpCode);
		log.debug("Http call return reason phrase is: {}" , httpMessage);
		if (httpCode != HttpURLConnection.HTTP_OK) {
			log.warn("Unable to get a valid response from server. Received a non HTTP 200 response from the server : {}, {}" , httpCode , httpMessage);
			throw new Exception("Unable to get a valid response from server. " + httpMessage);
		}
		return httpResponse;
	}

	/**
	 * @param storageAreaListString
	 * @return never null, a list that contains the decoded storage areas. None
	 *         of the elements can be null
	 */
	private ArrayList<StorageArea> decodeStorageAreaList(String storageAreaListString) {
		log.debug("Decoding the receive response");
		if (storageAreaListString == null) {
			log.error("Decoding failed, received a null storage area list string!");
			throw new IllegalArgumentException("Received a null storage area list string");
		}
		ArrayList<StorageArea> local = new ArrayList<StorageArea>();
		String[] SAEncodedArray = storageAreaListString.trim().split("" + ConfigDiscoveryServiceConstants.VFS_LIST_SEPARATOR);
		log.info("Decoding {} storage areas", SAEncodedArray.length);
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
		log.debug("Decoding storage area string '{}'", sAEncoded);
		ArrayList<StorageArea> producedList = new ArrayList<StorageArea>();
		String name = null;
		String root = null;
		ArrayList<String> stfnRootList = new ArrayList<String>();
		ArrayList<String> protocolList = new ArrayList<String>();
		HttpPerms httpPerms = HttpPerms.NOREAD;
		String[] SAFields = sAEncoded.trim().split("" + ConfigDiscoveryServiceConstants.VFS_FIELD_SEPARATOR);
		for (String SAField : SAFields) {
			String[] keyValue = SAField.trim().split("" + ConfigDiscoveryServiceConstants.VFS_FIELD_MATCHER);
			if (ConfigDiscoveryServiceConstants.VFS_NAME_KEY.equals(keyValue[0])) {
				name = keyValue[1];
				log.debug("Found name: {}" , name);
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_ROOT_KEY.equals(keyValue[0])) {
				root = keyValue[1];
				log.debug("Found File System Root: {}" , root);
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_STFN_ROOT_KEY.equals(keyValue[0])) {
				String[] stfnRootArray = keyValue[1].trim().split("" + ConfigDiscoveryServiceConstants.VFS_STFN_ROOT_SEPARATOR);
				for (String stfnRoot : stfnRootArray) {
					stfnRootList.add(stfnRoot);
					log.debug("Found Storage File Name Root: {}" , stfnRoot);
				}
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_ENABLED_PROTOCOLS_KEY.equals(keyValue[0])) {
				String[] protocolsArray = keyValue[1].trim().split("" + ConfigDiscoveryServiceConstants.VFS_ENABLED_PROTOCOLS_SEPARATOR);
				for (String protocol : protocolsArray) {
					protocolList.add(protocol);
					log.debug("Found Storage WebDAV protocol: {}" , protocol);
				}
				continue;
			}
			if (ConfigDiscoveryServiceConstants.VFS_ANONYMOUS_PERMS_KEY.equals(keyValue[0])) {
				httpPerms = HttpPerms.valueOf(keyValue[1]);
				log.debug("Found http-permissions: {}" , httpPerms);
				continue;
			}
		}
		if (name == null || root == null || stfnRootList.size() == 0) {
			log.warn("Unable to decode the storage area. Some fileds are missing: name={} FSRoot={} stfnRootList={}  protocolList={}" , name , root , stfnRootList , protocolList);
			throw new IllegalArgumentException("");
		}
		for (String stfnRoot : stfnRootList) {
			StorageArea storageArea = new StorageArea(name, root, stfnRoot, protocolList, httpPerms);
			log.info("Decoded storage area: {}" , storageArea.getName());
			log.debug("- details: [ {} ]", storageArea.toString());
			producedList.add(storageArea);
		}
		log.debug("Decoded {} storage areas" , producedList.size());
		return producedList;
	}

	public List<StorageArea> getUserAuthorizedStorageAreas(UserCredentials user, String reqProtocol) {
		List<StorageArea> out = new ArrayList<StorageArea>();
		for (StorageArea current : storageAreas) {
			if (current.getProtocols().contains(reqProtocol)) {
				if (isUserAllowedAccess(user, current)) {
					out.add(current);
				}
			}
		}
		return out;
	}
	
	private boolean isUserAllowedAccess(UserCredentials user, StorageArea sa) {
		if (sa.isHTTPReadable()) {
			return true;
		}
		if (user.isAnonymous()) {
			return false;
		}
		boolean response = false;
		try {
			response = StormAuthorizationUtils.isUserAuthorized(user, Constants.LS_OPERATION, sa.getFSRoot());
		} catch (Throwable e) {
			log.error(e.getMessage(),e);
			return false;
		} 
		return response;
	}
	
	
}
