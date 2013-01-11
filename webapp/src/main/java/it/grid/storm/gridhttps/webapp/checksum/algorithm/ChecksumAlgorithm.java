package it.grid.storm.gridhttps.webapp.checksum.algorithm;

import it.grid.storm.gridhttps.webapp.checksum.ChecksumReadException;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumType;

import java.io.InputStream;
import java.io.OutputStream;

public interface ChecksumAlgorithm {
	
	static final int BUFFER_SIZE = 4096;
	
	public ChecksumType getType();
	
	public String compute(InputStream inputStream) throws ChecksumReadException;
	
	public String compute(InputStream inputStream, OutputStream outputStream) throws ChecksumReadException;
	
}