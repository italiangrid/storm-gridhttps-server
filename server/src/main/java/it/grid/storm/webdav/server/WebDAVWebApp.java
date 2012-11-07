package it.grid.storm.webdav.server;

import it.grid.storm.storagearea.StorageArea;

import java.io.File;

public class WebDAVWebApp extends WebApp{

	public WebDAVWebApp(File baseDirectory, StorageArea SA) throws Exception {
		super(baseDirectory, SA.getStfnRoot(), SA.getFSRoot(), SA.getProtocol());
	}
	
}