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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Checksum {

    public enum ChecksumAlgorithm {
        CRC32("CRC32"),
        ADLER32("Adler32"),
        MD2("MD2"),
        MD5("MD5"),
        SHA_1("SHA-1"),
        SHA_256("SHA-256"),
        SHA_384("SHA-384"),
        SHA_512("SHA-512");

        public static ChecksumAlgorithm getChecksumAlgorithm(String algorithm) {

            algorithm = algorithm.toLowerCase();

            for (ChecksumAlgorithm checksumAlgorithm : ChecksumAlgorithm.values()) {
                if (checksumAlgorithm.toString().toLowerCase().equals(algorithm)) {
                    return checksumAlgorithm;
                }
            }
            return null;
        }

        private final String value;

        ChecksumAlgorithm(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }

    private static final Logger log = LoggerFactory.getLogger(Checksum.class);
    private static final int MAX_BUFFER_SIZE = 1024 * 64;

    /**
     * Computes checksum of the given file.
     * 
     * @param fileName file to compute the checksum for.
     * @param algorithm checksum algorithm to use.
     * @return a String containing the computed checksum.
     * @throws ChecksumFileNotFoundException if the given file was not found.
     * @throws ChecksumFileReadException if there was an error reading the file.
     * @throws ChecksumNotSupportedException if the given algorithm is not supported.
     */
    public static String computeChecksum(String fileName, String algorithm) {

        ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.getChecksumAlgorithm(algorithm);

        if (checksumAlgorithm == null) {
            throw new ChecksumNotSupportedException("Checksum algorithm not supported: " + algorithm);
        }

        File file = new File(fileName);

        long fileSize = file.length();

        int bufferSize;

        if (fileSize > MAX_BUFFER_SIZE) {
            bufferSize = MAX_BUFFER_SIZE;
        } else {
            bufferSize = (int) fileSize;

            if (bufferSize < 1) {
                bufferSize = 1;
            }
        }

        String checksum;
        FileInputStream inputStream = null;

        try {

            inputStream = new FileInputStream(file);

            if (checksumAlgorithm == ChecksumAlgorithm.CRC32) {

                log.trace("Computing " + ChecksumAlgorithm.CRC32);

                checksum = computeChecksumCRC32(inputStream, bufferSize);
            } else if (checksumAlgorithm == ChecksumAlgorithm.ADLER32) {

                log.trace("Computing " + ChecksumAlgorithm.ADLER32);

                checksum = computeChecksumAdler32(inputStream, bufferSize);
                
            } else {
                checksum = computeChecksumMD(inputStream, bufferSize, checksumAlgorithm);
            }

        } catch (FileNotFoundException e) {

            throw new ChecksumFileNotFoundException("File not found: " + fileName, e);

        } catch (IOException e) {

            throw new ChecksumFileReadException("Error reading file: " + fileName, e);

        } catch (NoSuchAlgorithmException e) {

            throw new ChecksumNotSupportedException("Checksum algorithm not supported: " + algorithm);

        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new RuntimeException("A problem occurred while closing the input stream associated to the file: "
                                                   + file.getAbsolutePath(),
                                           e);
            }
        }

        return checksum;
    }

    private static String computeChecksumAdler32(FileInputStream inputStream, int bufferSize)
            throws IOException {

        byte[] bAarray = new byte[bufferSize];
        CheckedInputStream cis = new CheckedInputStream(inputStream, new Adler32());
        while (cis.read(bAarray) >= 0) {
        }
        String checksum = Long.toHexString(cis.getChecksum().getValue());

        bAarray = null;

        return checksum;
    }

    private static String computeChecksumCRC32(FileInputStream inputStream, int bufferSize)
            throws IOException {
        String checksum = null;

        byte[] bArray = new byte[bufferSize];
        CRC32 crc32 = new CRC32();

        while (true) {

            int count = inputStream.read(bArray, 0, bufferSize);

            if (count == -1) {
                break;
            }

            crc32.update(bArray, 0, count);
        }

        bArray = null;

        checksum = Long.toHexString(crc32.getValue());

        return checksum;
    }

    private static String computeChecksumMD(FileInputStream inputStream, int bufferSize,
            ChecksumAlgorithm checksumType) throws IOException, NoSuchAlgorithmException {

        String algorithm = checksumType.toString();

        byte[] bArray = new byte[bufferSize];
        MessageDigest md = MessageDigest.getInstance(algorithm);

        log.trace("Computing " + algorithm);

        while (true) {

            int count = inputStream.read(bArray, 0, bufferSize);

            if (count == -1) {
                break;
            }

            md.update(bArray, 0, count);
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
