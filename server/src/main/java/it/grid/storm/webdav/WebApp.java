package it.grid.storm.webdav;

import it.grid.storm.storagearea.StorageArea;

import java.io.File;
import java.io.IOException;

public class WebApp {

	private String warFile = "";
	private StorageArea sa;

	public WebApp(StorageArea sa, String warFile) throws Exception, IOException {
		if (!(new File(warFile)).isFile())
			throw new IOException("template war file not found!");
		if (sa == null)
			throw new Exception("storage area is null!");
		this.warFile = warFile;
		this.sa = sa;
	}
	
	public String getName() {
		return sa.getName();
	}

	public String getRootDirectory() {
		return this.sa.getFSRoot();
	}

	public String getWarFile() {
		return warFile;
	}

}