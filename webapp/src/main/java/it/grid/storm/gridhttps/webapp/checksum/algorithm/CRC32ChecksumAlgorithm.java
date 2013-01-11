package it.grid.storm.gridhttps.webapp.checksum.algorithm;

import it.grid.storm.gridhttps.webapp.checksum.ChecksumReadException;
import it.grid.storm.gridhttps.webapp.checksum.ChecksumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CRC32ChecksumAlgorithm implements ChecksumAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(Adler32ChecksumAlgorithm.class);

	public ChecksumType getType() {
		return ChecksumType.CRC32;
	}

	public String compute(InputStream inputStream) throws ChecksumReadException {
		String checksum = null;
		byte[] bArray = new byte[BUFFER_SIZE];
		CRC32 crc32 = new CRC32();
		int count;
		try {
			while ((count = inputStream.read(bArray, 0, BUFFER_SIZE)) != -1) {
				crc32.update(bArray, 0, count);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
		bArray = null;
		checksum = Long.toHexString(crc32.getValue());
		return checksum;
	}

	public String compute(InputStream inputStream, OutputStream outputStream) throws ChecksumReadException {
		String checksum = null;
		byte[] bArray = new byte[BUFFER_SIZE];
		CRC32 crc32 = new CRC32();
		int count;
		try {
			while ((count = inputStream.read(bArray, 0, BUFFER_SIZE)) != -1) {
				crc32.update(bArray, 0, count);
				outputStream.write(bArray, 0, count);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
		bArray = null;
		checksum = Long.toHexString(crc32.getValue());
		return checksum;
	}

}