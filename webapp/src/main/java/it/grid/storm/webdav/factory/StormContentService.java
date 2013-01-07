package it.grid.storm.webdav.factory;

import io.milton.http.fs.FileContentService;
import it.grid.storm.Configuration;
import it.grid.storm.checksum.ChecksumFileReadException;
import it.grid.storm.checksum.ChecksumNotSupportedException;
import it.grid.storm.checksum.Checksum.ChecksumAlgorithm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StormContentService implements FileContentService {

	private static final Logger log = LoggerFactory.getLogger(StormResourceFactory.class);
	private static final int BUFFER_SIZE = 4096;

	public void setFileContent(File file, InputStream in) throws FileNotFoundException, IOException {
		if (Configuration.getComputeChecksum()) {
			String checksum = computeCopyWithChecksum(in, new FileOutputStream(file), Configuration.getChecksumType());
			log.debug("checksum: " + checksum);
		} else {
			doSimpleSetFileContent(in, new FileOutputStream(file));
		}
	}

	public InputStream getFileContent(File file) throws FileNotFoundException {
		FileInputStream fin = new FileInputStream(file);
		return fin;
	}

	private void doSimpleSetFileContent(InputStream in, OutputStream out) throws FileNotFoundException, IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
	
	private String computeCopyWithChecksum(InputStream inputStream, FileOutputStream outputStream, ChecksumAlgorithm checksumAlgorithm) {

		String checksum;
		try {
			if (checksumAlgorithm == ChecksumAlgorithm.CRC32) {
				log.debug("Computing " + ChecksumAlgorithm.CRC32);
				checksum = computeCopyWithChecksumCRC32(inputStream, outputStream);
			} else if (checksumAlgorithm == ChecksumAlgorithm.ADLER32) {
				log.debug("Computing " + ChecksumAlgorithm.ADLER32);
				checksum = computeCopyWithChecksumAdler32(inputStream, outputStream);
			} else {
				checksum = computeCopyWithChecksumMD(inputStream, outputStream, checksumAlgorithm);
			}
		} catch (IOException e) {
			throw new ChecksumFileReadException("Error reading inputstream", e);
		} catch (NoSuchAlgorithmException e) {
			throw new ChecksumNotSupportedException("Checksum algorithm not supported: " + checksumAlgorithm.toString());
		} finally {
			if (inputStream != null)
				try {
					inputStream.close();
				} catch (IOException e) {
					log.warn("Some errors occured closing inputstream");
				}
			if (outputStream != null)
				try {
					outputStream.close();
				} catch (IOException e) {
					log.warn("Some errors occured closing outputstream");
				}	
		}
		return checksum;
	}

	private static String computeCopyWithChecksumAdler32(InputStream inputStream, FileOutputStream outputStream)
            throws IOException {

        byte[] bArray = new byte[BUFFER_SIZE];
        CheckedInputStream cis = new CheckedInputStream(inputStream, new Adler32());
        int bytes_read;
        while ((bytes_read = cis.read(bArray)) != -1) {
        	outputStream.write(bArray, 0, bytes_read);
        }
        String checksum = Long.toHexString(cis.getChecksum().getValue());
        bArray = null;
        return checksum;
    }

    private static String computeCopyWithChecksumCRC32(InputStream inputStream, FileOutputStream outputStream)
            throws IOException {
        
    	String checksum = null;
        byte[] bArray = new byte[BUFFER_SIZE];
        CRC32 crc32 = new CRC32();
        while (true) {
            int count = inputStream.read(bArray, 0, BUFFER_SIZE);
            if (count == -1) {
                break;
            }
            crc32.update(bArray, 0, count);
            outputStream.write(bArray, 0, count);
        }
        bArray = null;
        checksum = Long.toHexString(crc32.getValue());
        return checksum;
    }

    private static String computeCopyWithChecksumMD(InputStream inputStream, FileOutputStream outputStream,
            ChecksumAlgorithm checksumType) throws IOException, NoSuchAlgorithmException {

        String algorithm = checksumType.toString();
        byte[] bArray = new byte[BUFFER_SIZE];
        MessageDigest md = MessageDigest.getInstance(algorithm);
        log.debug("Computing " + algorithm);
        while (true) {
            int count = inputStream.read(bArray, 0, BUFFER_SIZE);
            if (count == -1) {
                break;
            }
            md.update(bArray, 0, count);
            outputStream.write(bArray, 0, count);
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
