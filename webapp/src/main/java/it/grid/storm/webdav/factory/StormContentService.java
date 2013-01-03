package it.grid.storm.webdav.factory;

import io.milton.http.fs.FileContentService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StormContentService implements FileContentService {

	@Override
	public void setFileContent(File file, InputStream in) throws FileNotFoundException, IOException {
		FileOutputStream out = null;
		byte[] toMatch = new byte[] {97, 98, 99}; //abc
		try {
			out = new FileOutputStream(file);
			byte[] buffer = new byte[3]; // To hold file contents
			int bytes_read; // How many bytes in buffer
			while ((bytes_read = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytes_read); // write
				if ((new String(buffer)).equals(new String(toMatch)))
					System.out.println("+");
			}
		} finally {
			if (in != null)
				try {
					out.close();
				} catch (IOException e) {
					;
				}
			if (out != null)
				try {
					out.close();
				} catch (IOException e) {
					;
				}
		}
	}

	@Override
	public InputStream getFileContent(File file) throws FileNotFoundException {
		FileInputStream fin = new FileInputStream(file);
		return fin;
	}
}
