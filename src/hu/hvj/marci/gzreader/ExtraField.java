package hu.hvj.marci.gzreader;

import java.util.Arrays;

public class ExtraField {

	private final byte si1, si2;
	private final int len;
	private final byte[] data;

	public ExtraField(byte[] b) {
		this.si1 = b[0];
		this.si2 = b[1];

		this.len = Helper.twoBytesToIntLSBFirst(Arrays.copyOfRange(b, 2, 4));

		this.data = Arrays.copyOfRange(b, 5, 5 + this.len);
	}

	public void printData() {
		System.out.printf("Azonosító: %c%c%nTartalom hossza:%d%n", (char) si1, (char) si2, len);
	}

	public byte[] getData() {
		return this.data;
	}

	public String getID() {
		return new String(new byte[] { this.si1, this.si2 });
	}

}
