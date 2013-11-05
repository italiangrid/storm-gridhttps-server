package it.grid.storm.gridhttps.webapp.common.checksum;

public enum ChecksumType {
	CRC32("CRC32"), ADLER32("Adler32"), MD2("MD2"), MD5("MD5"), 
	SHA_1("SHA-1"), SHA_256("SHA-256"), SHA_384("SHA-384"), SHA_512("SHA-512");

	public static ChecksumType getChecksumAlgorithm(String algorithm) {
		algorithm = algorithm.toLowerCase();
		for (ChecksumType checksumAlgorithm : ChecksumType.values()) {
			if (checksumAlgorithm.toString().toLowerCase().equals(algorithm)) {
				return checksumAlgorithm;
			}
		}
		return null;
	}

	private final String value;

	ChecksumType(String value) {
		this.value = value;
	}

	public String toString() {
		return value;
	}
	
}