package it.grid.storm.webdav.server;

import it.grid.storm.storagearea.StorageArea;

import java.io.File;

public class FileTransferWebApp extends WebApp{

	public FileTransferWebApp(File baseDirectory, StorageArea SA) throws Exception {
		super(baseDirectory, "/filetransfer" + SA.getStfnRoot(), SA.getFSRoot(), SA.getProtocol());
	}
	
}