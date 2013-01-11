package it.grid.storm.gridhttps.webapp.checksum.algorithm;

import it.grid.storm.gridhttps.webapp.checksum.ChecksumReadException;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Adler32ChecksumAlgorithm implements ChecksumAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(Adler32ChecksumAlgorithm.class);
	
	public ChecksumType getType() {
		return ChecksumType.ADLER32;
	}

	public String compute(InputStream inputStream) throws ChecksumReadException {
		byte[] bAarray = new byte[BUFFER_SIZE];
		CheckedInputStream cis = new CheckedInputStream(inputStream, new Adler32());
		try {
			while (cis.read(bAarray) >= 0) {
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
		String checksum = Long.toHexString(cis.getChecksum().getValue());
		bAarray = null;
		return checksum;
	}

	public String compute(InputStream inputStream, OutputStream outputStream) throws ChecksumReadException {
		byte[] bArray = new byte[BUFFER_SIZE];
        CheckedInputStream cis = new CheckedInputStream(inputStream, new Adler32());
        int bytes_read;
        try {
			while ((bytes_read = cis.read(bArray)) != -1) {
				outputStream.write(bArray, 0, bytes_read);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
        String checksum = Long.toHexString(cis.getChecksum().getValue());
        bArray = null;
        return checksum;
	}
	
}