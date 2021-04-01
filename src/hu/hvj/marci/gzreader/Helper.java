package hu.hvj.marci.gzreader;

public class Helper {

	public static int fourBytesToIntLSBFirst(byte... b) {
		return ((int) b[0] & 0xFF) | (((int) b[1] & 0xFF) << 8) | (((int) b[2] & 0xFF) << 16)
				| (((int) b[3] & 0xFF) << 24);
	}

	public static int twoBytesToIntLSBFirst(byte... b) {
		return ((int) b[0] & 0xFF) | (((int) b[1] & 0xFF) << 8);
	}

	public static boolean[] byteToBooleansLSBFirst(byte b) {
		boolean[] bits = new boolean[8];
		bits[0] = ((b & 1) != 0);
		bits[1] = ((b & 2) != 0);
		bits[2] = ((b & 4) != 0);
		bits[3] = ((b & 8) != 0);
		bits[4] = ((b & 16) != 0);
		bits[5] = ((b & 32) != 0);
		bits[6] = ((b & 64) != 0);
		bits[7] = ((b & 128) != 0);
		return bits;
	}
	
	public static boolean[] byteToBooleansMSBFirst(byte b) {
		boolean[] bits = new boolean[8];
		bits[0] = ((b & 128) != 0);
		bits[1] = ((b & 64) != 0);
		bits[2] = ((b & 32) != 0);
		bits[3] = ((b & 16) != 0);
		bits[4] = ((b & 8) != 0);
		bits[5] = ((b & 4) != 0);
		bits[6] = ((b & 2) != 0);
		bits[7] = ((b & 1) != 0);
		return bits;
	}
	
	public static String decimalToBinary(int d, int len) {
		StringBuilder sb = new StringBuilder();
		if (len > 8) {
			sb.append(decimalToBinary(d >> 8, len - 8));
			len = 8;
		}
		d %= 256;
		boolean[] b = byteToBooleansMSBFirst((byte) d);
		for (int i = 8 - len; i < b.length; i++) {
			sb.append(b[i] ? 1 : 0);
		}
		return new String(sb);
	}

}
