package it.grid.storm.webdav.server;

import it.grid.storm.gridhttps.servlet.MapperServlet;
import it.grid.storm.storagearea.StorageArea;

import java.io.File;

public class FileTransferWebApp extends WebApp{

	public FileTransferWebApp(File baseDirectory, StorageArea SA) throws Exception {
		super(baseDirectory, MapperServlet.MAPPER_SERVLET_CONTEXT_PATH + SA.getStfnRoot(), SA.getFSRoot(), SA.getProtocol());
	}
	
}