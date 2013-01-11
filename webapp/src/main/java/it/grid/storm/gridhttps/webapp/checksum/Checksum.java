/*
 * Copyright (c) Istituto Nazionale di Fisica Nucleare (INFN). 2006-2013.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.grid.storm.gridhttps.webapp.checksum;

//import java.io.IOException;
//import java.io.InputStream;
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//import java.util.zip.Adler32;
//import java.util.zip.CRC32;
//import java.util.zip.CheckedInputStream;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

public class Checksum {

	

//	private static final Logger log = LoggerFactory.getLogger(Checksum.class);
//
//	/**
//	 * Computes checksum of the given file.
//	 * 
//	 * @param inputStream
//	 *            input stream to compute the checksum for.
//	 * @param checksumAlgorithm
//	 *            checksum algorithm to use.
//	 * @return a String containing the computed checksum.
//	 * @throws ChecksumReadException
//	 *             if there was an error reading the file.
//	 * @throws ChecksumNotSupportedException
//	 *             if the given algorithm is not supported.
//	 */
//	public static String computeChecksum(InputStream inputStream, ChecksumType checksumAlgorithm) {
//
//		if (checksumAlgorithm == null) {
//			throw new IllegalArgumentException("Checksum algorithm is null");
//		}
//		if (inputStream == null) {
//			throw new IllegalArgumentException("Input stream is null");
//		}
//
//		String checksum;
//		try {
//			if (checksumAlgorithm == ChecksumType.CRC32) {
//				log.debug("Computing " + ChecksumType.CRC32);
//				checksum = computeChecksumCRC32(inputStream, BUFFER_SIZE);
//			} else if (checksumAlgorithm == ChecksumType.ADLER32) {
//				log.debug("Computing " + ChecksumType.ADLER32);
//				checksum = computeChecksumAdler32(inputStream, BUFFER_SIZE);
//			} else {
//				checksum = computeChecksumMD(inputStream, BUFFER_SIZE, checksumAlgorithm);
//			}
//		} catch (IOException e) {
//			log.error(e.getMessage());
//			throw new ChecksumReadException(e);
//		} catch (NoSuchAlgorithmException e) {
//			log.error(e.getMessage());
//			throw new ChecksumNotSupportedException(e);
//		}
//
//		return checksum;
//	}

//	private static String computeChecksumAdler32(InputStream inputStream, int bufferSize) throws IOException {
//
//		byte[] bAarray = new byte[bufferSize];
//		CheckedInputStream cis = new CheckedInputStream(inputStream, new Adler32());
//		while (cis.read(bAarray) >= 0) {
//		}
//		String checksum = Long.toHexString(cis.getChecksum().getValue());
//		bAarray = null;
//		return checksum;
//	}
//
//	private static String computeChecksumCRC32(InputStream inputStream, int bufferSize) throws IOException {
//		String checksum = null;
//		byte[] bArray = new byte[bufferSize];
//		CRC32 crc32 = new CRC32();
//		int count;
//		while ((count = inputStream.read(bArray, 0, bufferSize)) != -1) {
//			crc32.update(bArray, 0, count);
//		}
//		bArray = null;
//		checksum = Long.toHexString(crc32.getValue());
//		return checksum;
//	}
//
//	private static String computeChecksumMD(InputStream inputStream, int bufferSize, ChecksumType checksumType) throws IOException,
//			NoSuchAlgorithmException {
//
//		String algorithm = checksumType.toString();
//		byte[] bArray = new byte[bufferSize];
//		MessageDigest md = MessageDigest.getInstance(algorithm);
//		log.trace("Computing " + algorithm);
//		int count;
//		while ((count = inputStream.read(bArray, 0, bufferSize)) != -1) {
//			md.update(bArray, 0, count);
//		}
//		byte[] hash = md.digest();
//		StringBuffer sb = new StringBuffer();
//		for (int i = 0; i < hash.length; i++) {
//			sb.append(Integer.toString((hash[i] & 0xff) + 0x100, 16).substring(1));
//		}
//		bArray = null;
//		hash = null;
//		return sb.toString();
//	}
}
