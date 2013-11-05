package it.grid.storm.gridhttps.webapp.common.checksum.algorithm;

import it.grid.storm.gridhttps.webapp.common.checksum.ChecksumReadException;
import it.grid.storm.gridhttps.webapp.common.checksum.ChecksumType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDChecksumAlgorithm implements ChecksumAlgorithm {

	private static final Logger log = LoggerFactory.getLogger(Adler32ChecksumAlgorithm.class);

	private MessageDigest md;
	
	public ChecksumType getType() {
		return ChecksumType.valueOf(md.getAlgorithm());
	}

	public MDChecksumAlgorithm(String algorithm) throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance(algorithm);
	}
	
	public String compute(InputStream inputStream) throws ChecksumReadException {
		log.debug("Computing " + getType().name() + " checksum");
		byte[] bArray = new byte[BUFFER_SIZE];
		int count;
		try {
			while ((count = inputStream.read(bArray, 0, BUFFER_SIZE)) != -1) {
				md.update(bArray, 0, count);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
		byte[] hash = md.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
		}
		bArray = null;
		hash = null;
		return sb.toString();
	}

	public String compute(InputStream inputStream, OutputStream outputStream) throws ChecksumReadException {
		log.debug("Computing " + getType().name() + " checksum");
		byte[] bArray = new byte[BUFFER_SIZE];
		int count;
		try {
			while ((count = inputStream.read(bArray, 0, BUFFER_SIZE)) != -1) {
				md.update(bArray, 0, count);
				outputStream.write(bArray, 0, count);
			}
		} catch (IOException e) {
			log.error(e.getMessage());
			throw new ChecksumReadException(e);
		}
		byte[] hash = md.digest();
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < hash.length; i++) {
			sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
		}
		bArray = null;
		hash = null;
		return sb.toString();
	}

}