package it.grid.storm;

import java.io.File;

public class WebApp {

	private String warFile = "";
	private String name = "";
	private String rootDirectory = "";


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(String rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public boolean isReadyToDeploy() {
		if (this.getRootDirectory().isEmpty())
			return false;
		if (this.getName().isEmpty())
			return false;
		if (!(new File(this.getWarFile())).isFile())
			return false;
		return true;
	}

	public String getWarFile() {
		return warFile;
	}

	public void setWarFile(String warFile) {
		this.warFile = warFile;
	}

}