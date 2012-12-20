package it.grid.storm.filetransfer.factory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.ConflictException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.exceptions.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemResourceHelper {

	private static final Logger log = LoggerFactory.getLogger(FileSystemResourceHelper.class);
	
	public static InputStream doGetFile(FileResource source) throws NotFoundException {
		log.info("Called doGetFile()");
		InputStream in = null;
		try {
			in = source.contentService.getFileContent(source.file);
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
			throw new NotFoundException("Couldn't locate content");
		}
		return in;
	}
	
	public static boolean doPutOverwrite(FileResource source, InputStream in) throws BadRequestException, ConflictException, NotAuthorizedException {
		log.info("Called doPutOverewrite()");
		try {
			// overwrite
			source.contentService.setFileContent(source.file, in);
		} catch (IOException ex) {
			log.error(ex.getMessage());
			throw new RuntimeException("Couldnt write to: " + source.file.getAbsolutePath(), ex);
		} 
		return true;
	}	
}
