package hu.hvj.marci.gzreader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Inflater {

	public static final int STORED = 0;
	public static final int FIXED_HUFFMAN_CODES = 1;
	public static final int DYNAMIC_HUFFMAN_CODES = 2;
	public static final int RESERVED_COMPRESSED_METHOD = 3;

	private int blockCount;
	private int[] blockTypes;

	public byte[] inflate(InputStream is) throws IOException {
		ArrayList<Byte> als = new ArrayList<Byte>();
		BooleanArrayList bals = new BooleanArrayList();
		byte[] buf = new byte[1];

		boolean isLastBlock;
		this.blockCount = 0;
		this.blockTypes = new int[3];
		do {
			is.read(buf);
			addBitsToAls(bals, buf);
			isLastBlock = bals.getLast();
			int compressionMethod = bals.getNextTwoBitInteger();
			String tomoritesiMetodus;
			this.blockTypes[compressionMethod]++;
			switch (compressionMethod) {
			case STORED:
				tomoritesiMetodus = "tárolva";
				break;
			case FIXED_HUFFMAN_CODES:
				tomoritesiMetodus = "fix huffman tábla";
				break;
			case DYNAMIC_HUFFMAN_CODES:
				tomoritesiMetodus = "dinamikus huffman tábla";
				break;
			default:
				tomoritesiMetodus = "definiálatlan (" + compressionMethod + ") HIBA";
				break;
			}
			System.out.printf("Blokk %d megkezdve (%s, tömörítési mód: %s)%n", ++this.blockCount,
					isLastBlock ? "utolsó" : "nem utolsó", tomoritesiMetodus);
			if (compressionMethod == STORED) {
				bals.nextByte();
				if (bals.size() < 32) {
					buf = new byte[4];
					is.read(buf);
					addBitsToAls(bals, buf);
					int len = bals.getNextXBitIntegerLSBFirst(16);
					int nlen = bals.getNextXBitIntegerLSBFirst(16);
					System.out.println("  hossz: " + len);
					if (nlen != (~len & 0xFFFF)) {
						System.err.printf("Az NLEN hibás! (LEN: 0x%04X, NLEN: 0x%04X)%n", len, nlen);
						break;
					} else {
						System.out.printf("Az NLEN helyes! (LEN: 0x%04X, NLEN: 0x%04X)%n", len, nlen);
					}
					len -= bals.size() / 8;
					while (bals.size() > 0) {
						als.add(bals.getNextByte());
					}
					byte[] sbuf = new byte[len];
					is.read(sbuf);
					for (byte b : sbuf) {
						als.add(b);
					}
				}
			} else {
				HuffmanCode huffman;
				if (compressionMethod == DYNAMIC_HUFFMAN_CODES) {
					huffman = new HuffmanCode(bals, is);
				} else {
					huffman = HuffmanCode.STATIC_HUFFMAN_TABLE;
				}
				while (true) {
					int val = 0, len = 0;
					while (HuffmanCode.needBits(val, len, huffman.literal)) {
						if (bals.size() < 1) {
							buf = new byte[1];
							is.read(buf);
							Inflater.addBitsToAls(bals, buf);
						}
						val <<= 1;
						val |= bals.getLast() ? 1 : 0;
						len++;
					}
					int decodedVal = HuffmanCode.getValue(val, len, huffman.literal);
					if (decodedVal < 256) {
						als.add((byte) decodedVal);
					} else if (decodedVal >= 257 && decodedVal <= 285) {
						int lenExtraBitCount = lengthExtraBits(decodedVal);
						if (bals.size() < lenExtraBitCount) {
							buf = new byte[(int) Math.ceil(lenExtraBitCount / 8.)];
							is.read(buf);
							Inflater.addBitsToAls(bals, buf);
						}
						int lenExtraBits = bals.getNextXBitIntegerLSBFirst(lenExtraBitCount);
						int keszlen = lengthValue(decodedVal, lenExtraBits);

						int dval = 0, dlen = 0;
						while (HuffmanCode.needBits(dval, dlen, huffman.dist)) {
							if (bals.size() < 1) {
								buf = new byte[1];
								is.read(buf);
								Inflater.addBitsToAls(bals, buf);
							}
							dval <<= 1;
							dval |= bals.getLast() ? 1 : 0;
							dlen++;
						}
						int decodedDist = HuffmanCode.getValue(dval, dlen, huffman.dist);
						int distExtraBitCount = distExtraBits(decodedDist);
						if (distExtraBitCount == -1) {
							System.err.printf("Hiba! decodedDist = %d%n", decodedDist);
						}
						if (bals.size() < distExtraBitCount) {
							buf = new byte[(int) Math.ceil(distExtraBitCount / 8.)];
							is.read(buf);
							Inflater.addBitsToAls(bals, buf);
						}
						int distExtraBits = bals.getNextXBitIntegerLSBFirst(distExtraBitCount);
						int dist = distValue(decodedDist, distExtraBits);

						int startIndex = als.size() - dist;
						int index = startIndex;
						for (int i = 0; i < keszlen; i++) {
							byte szam = als.get(index);
							als.add(szam);
							if (++index >= als.size()) {
								index = startIndex;
							}
						}
					} else if (decodedVal == 256) {
						break;
					}
				}
			}
		} while (!isLastBlock);

		byte[] r = new byte[als.size()];
		for (int i = 0; i < r.length; i++) {
			r[i] = als.get(i);
		}
		return r;
	}

	public static void addBitsToAls(ArrayList<Boolean> als, byte[] buf) {
		ArrayList<Boolean> tmp = new ArrayList<Boolean>();
		als.forEach(i -> tmp.add(i));
		als.clear();
		for (int i = buf.length - 1; i >= 0; i--) {
			for (boolean bo : Helper.byteToBooleansMSBFirst(buf[i])) {
				als.add(new Boolean(bo));
			}
		}
		tmp.forEach(i -> als.add(i));
	}

	public static int lengthExtraBits(int length) {
		if (length >= 257 && length <= 264) {
			return 0;
		} else if (length >= 265 && length <= 268) {
			return 1;
		} else if (length >= 269 && length <= 272) {
			return 2;
		} else if (length >= 273 && length <= 276) {
			return 3;
		} else if (length >= 277 && length <= 280) {
			return 4;
		} else if (length >= 281 && length <= 284) {
			return 5;
		} else if (length == 285) {
			return 0;
		} else {
			return -1;
		}
	}

	public static int lengthValue(int length, int extraBits) {
		if (length >= 257 && length <= 264) {
			return length - 254;
		} else if (length >= 265 && length <= 268) {
			int lower = (length - 265) * 2 + 11;
			return lower + extraBits;
		} else if (length >= 269 && length <= 272) {
			int lower = (length - 269) * 4 + 19;
			return lower + extraBits;
		} else if (length >= 273 && length <= 276) {
			int lower = (length - 273) * 8 + 35;
			return lower + extraBits;
		} else if (length >= 277 && length <= 280) {
			int lower = (length - 277) * 16 + 67;
			return lower + extraBits;
		} else if (length >= 281 && length <= 284) {
			int lower = (length - 281) * 32 + 131;
			return lower + extraBits;
		} else if (length == 285) {
			return 258;
		} else {
			return -1;
		}
	}

	public static int distExtraBits(int dist) {
		if (dist < 4) {
			return 0;
		} else {
			return (dist - 2) / 2;
		}
	}

	public static int distValue(int dist, int extraBits) {
		return distLowerBound(dist) + extraBits;
	}

	public static int distLowerBound(int dist) {
		if (dist < 4) {
			return dist + 1;
		} else if (dist == 4) {
			return 5;
		} else {
			return distUpperBound(dist - 1) + 1;
		}
	}

	public static int distUpperBound(int dist) {
		if (dist < 4) {
			return dist + 1;
		} else if (dist == 4) {
			return 6;
		} else {
			int elozo = distUpperBound(dist - 1);
			return (int) Math.pow(2, distExtraBits(dist)) + elozo;
		}
	}

	public int getBlockCount() {
		return this.blockCount;
	}

	public int[] getBlockTypes() {
		return this.blockTypes.clone();
	}

	public int getBlockTypeCount(int type) {
		return this.blockTypes[type];
	}

}
