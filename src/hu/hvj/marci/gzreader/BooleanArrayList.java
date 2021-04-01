package hu.hvj.marci.gzreader;

import java.util.ArrayList;

public class BooleanArrayList extends ArrayList<Boolean> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5261307688658786823L;

	@Override
	public Boolean get(int index) {
		Boolean b = super.get(index);
		this.remove(index);
		return b;
	}

	public boolean getLast() {
		return this.get(this.size() - 1).booleanValue();
	}

	public int getNextTwoBitInteger() {
		return this.getNextXBitIntegerLSBFirst(2);
	}
	
	public int getNextXBitIntegerLSBFirst(int x) {
		int szam = 0;
		for (int i = 0; i < x; i++) {
			int b = this.getLast() ? 1 : 0;
			szam |= b << i;
		}
		return szam;
	}
	
	public int getNextXBitIntegerMSBFirst(int x) {
		int szam = 0;
		for (int i = 0; i < x; i++) {
			int b = this.getLast() ? 1 : 0;
			szam <<= 1;
			szam |= b;
		}
		return szam;
	}

	public int getNextTwoByteIntegerLSBFirst() {
		int counter = 0, szam = 0;
		while (counter < 16) {
			int b = this.getLast() ? 1 : 0;
			szam |= b << counter;
			counter++;
		}
//		counter = 0;
//		while (counter < 8) {
//			int b = this.getLast() ? 1 : 0;
//			szam |= b << counter;
//			counter++;
//		}
		return szam;
	}

	public void nextByte() {
		while (this.size() % 8 != 0) {
			this.remove(this.size() - 1);
		}
	}
	
	public byte getNextByte() {
		int counter = 0, szam = 0;
		while (counter < 8) {
			int b = this.getLast() ? 1 : 0;
			szam |= b << counter;
			counter++;
		}
		return (byte) szam;
	}

}
