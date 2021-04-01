package hu.hvj.marci.gzreader;

public class CRC {
	private static int[] crc16Table = new int[256];
	private static int[] crc32Table = new int[256];
	private static boolean isCRC16Computed = false, isCRC32Computed = false;
	
	// TODO CRC16POLY ellenőrzése!
	public static final int CRC16POLY = 0xA001, CRC32POLY = 0xEDB88320;

	public static void makeCRC16Table() {
		for (int n = 0; n < 256; n++) {
			int c = n;
			for (int k = 0; k < 8; k++) {
				if ((c & 1) != 1) {
					c = CRC16POLY ^ (c >>> 1);
				} else {
					c >>>=1;
				}
			}
			crc16Table[n] = c;
		}
		isCRC16Computed = true;
	}
	
	public static void makeCRC32Table() {
		for (int n = 0; n < 256; n++) {
			int c = n;
			for (int k = 0; k < 8; k++) {
				if ((c & 1) != 0) {
					c = CRC32POLY ^ (c >>> 1);
				} else {
					c >>>= 1;
				}
			}
			crc32Table[n] = c;
		}
		isCRC32Computed = true;
	}
	
	public static int updateCRC16(int crc, byte[] buf) {
		int c = ~crc;
		
		if (!isCRC16Computed) {
			makeCRC16Table();
		}
		
		for (int n = 0; n < buf.length; n++) {
			c = crc16Table[(c ^ buf[n]) & 0xFF] ^ (c >>> 8);
		}
		
		return ~c & 0xFFFF;
	}

	public static int updateCRC32(int crc, byte[] buf) {
		int c = ~crc;

		if (!isCRC32Computed) {
			makeCRC32Table();
		}

		for (int n = 0; n < buf.length; n++) {
			c = crc32Table[(c ^ buf[n]) & 0xFF] ^ (c >>> 8);
		}

		return ~c;
	}
	
	public static int crc16(byte[] buf) {
		return updateCRC16(0, buf);
	}
	
	public static int crc32(byte[] buf) {
		return updateCRC32(0, buf);
	}
}
