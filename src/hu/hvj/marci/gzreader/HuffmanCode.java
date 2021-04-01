package hu.hvj.marci.gzreader;

import java.io.IOException;
import java.io.InputStream;

public class HuffmanCode {
	static class HuffmanNode {
		int len, code;

		public HuffmanNode(int len) {
			this.len = len;
		}
		
		@Override
		public String toString() {
			return String.format("Length: %d, Code: %s (%d)", this.len, Helper.decimalToBinary(this.code, this.len), this.code);
		}
	}

	public static final int[] HCLEN_INDEXES = { 16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15 };
	public static final int STATIC_HLIT = 288, STATIC_HDIST = 32;
	public static final int STATIC_FIRST_LIT_SECTION = 48, STATIC_SECOND_LIT_SECTION = 400,
			STATIC_THIRD_LIT_SECTION = 0, STATIC_FOURTH_LIT_SECTION = 192;

	private final int hlit, hdist, hclen;
	public final HuffmanNode[] literal, dist;

	public static final HuffmanCode STATIC_HUFFMAN_TABLE = new HuffmanCode();

	public HuffmanCode(BooleanArrayList als, InputStream is) throws IOException {
		System.out.println("Huffman tábla készítése megkezdve");
		if (als.size() < 14) {
			byte[] buf = new byte[14];
			is.read(buf);
			Inflater.addBitsToAls(als, buf);
		}
		this.hlit = als.getNextXBitIntegerLSBFirst(5) + 257;
		this.hdist = als.getNextXBitIntegerLSBFirst(5) + 1;
		this.hclen = als.getNextXBitIntegerLSBFirst(4) + 4;

		if (als.size() < 19) {
			byte[] buf = new byte[19 * 3];
			is.read(buf);
			Inflater.addBitsToAls(als, buf);
		}

		System.out.print("  Tábla a táblákhoz megkezdve");
		HuffmanNode[] forHuffmanTables = new HuffmanNode[19];
		for (int i = 0; i < hclen; i++) {
			int a = als.getNextXBitIntegerLSBFirst(3);
			forHuffmanTables[HCLEN_INDEXES[i]] = new HuffmanNode(a);
		}
		for (int i = hclen; i < forHuffmanTables.length; i++) {
			forHuffmanTables[HCLEN_INDEXES[i]] = new HuffmanNode(0);
		}

		buildHuffmanTree(forHuffmanTables, 7);
		System.out.println("  Tábla a táblákhoz kész");
		System.out.println("  Tényleges/hossz tábla megkezdve");
		System.out.println("    minBits számolása megkezdve");
		int minBits = Integer.MAX_VALUE;
		for (int i = 0; i < forHuffmanTables.length; i++) {
			if (forHuffmanTables[i].len < minBits && forHuffmanTables[i].len != 0) {
				minBits = forHuffmanTables[i].len;
			}
		}
		System.out.println("    minBits számolása kész");

		System.out.println("    tábla dekódolása megkezdve");
		this.literal = new HuffmanNode[hlit];
		decodeHuffmanTree(literal, forHuffmanTables, als, is, minBits);
		System.out.println("    tábla dekódolása kész");

		System.out.println("    tábla építése megkezdve");
		buildHuffmanTree(literal, 15);
		System.out.println("    tábla építése kész");
		System.out.println("  Tényleges/hossz tábla kész");

		this.dist = new HuffmanNode[hdist];
		decodeHuffmanTree(dist, forHuffmanTables, als, is, minBits);

		buildHuffmanTree(dist, 15);
		System.out.println("  Távolság tábla kész");
		System.out.println("Huffman tábla kész");
	}

	private HuffmanCode() {
		this.hlit = STATIC_HLIT;
		this.hdist = STATIC_HDIST;
		this.hclen = 0;

		this.literal = new HuffmanNode[this.hlit];
		for (int i = 0; i <= 143; i++) {
			literal[i] = new HuffmanNode(8);
			literal[i].code = STATIC_FIRST_LIT_SECTION + i;
		}
		for (int i = 144; i <= 255; i++) {
			literal[i] = new HuffmanNode(9);
			literal[i].code = STATIC_SECOND_LIT_SECTION + i - 144;
		}
		for (int i = 256; i <= 279; i++) {
			literal[i] = new HuffmanNode(7);
			literal[i].code = STATIC_THIRD_LIT_SECTION + i - 256;
		}
		for (int i = 280; i <= 287; i++) {
			literal[i] = new HuffmanNode(8);
			literal[i].code = STATIC_FOURTH_LIT_SECTION + i - 280;
		}

		this.dist = new HuffmanNode[this.hdist];
		for (int i = 0; i < dist.length; i++) {
			dist[i] = new HuffmanNode(5);
			dist[i].code = i;
		}
	}

	public static boolean needBits(int val, int len, HuffmanNode[] table) {
		for (HuffmanNode h : table) {
			if (h.code == val && h.len == len && h.len != 0) {
				return false;
			}
		}
		return true;
	}

	public static void buildHuffmanTree(HuffmanNode[] tree, int maxCodeLength) {
		int[] bl_count = new int[maxCodeLength + 1];
		for (int i = 0; i < tree.length; i++) {
			bl_count[tree[i].len]++;
		}

		bl_count[0] = 0;
		int max_bits = 0;
		for (int i = bl_count.length - 1; i >= 0; i--) {
			if (bl_count[i] > 0) {
				max_bits = i;
				break;
			}
		}

		int[] next_code = new int[max_bits + 1];
		int code = 0;
		for (int bits = 1; bits <= max_bits; bits++) {
			code = (code + bl_count[bits - 1]) << 1;
			next_code[bits] = code;
		}

		for (int i = 0; i < tree.length; i++) {
			int len = tree[i].len;
			if (len != 0) {
				tree[i].code = next_code[len];
				next_code[len]++;
			}
		}
	}

	public static void decodeHuffmanTree(HuffmanNode[] tree, HuffmanNode[] forHuffmanTables, BooleanArrayList als,
			InputStream is, int minBits) throws IOException {
		for (int i = 0; i < tree.length; i++) {
			if (als.size() < minBits) {
				byte[] buf = new byte[1];
				is.read(buf);
				Inflater.addBitsToAls(als, buf);
			}
			int val = als.getNextXBitIntegerMSBFirst(minBits), len = minBits;
			while (needBits(val, len, forHuffmanTables)) {
				if (als.size() < 1) {
					byte[] buf = new byte[1];
					is.read(buf);
					Inflater.addBitsToAls(als, buf);
				}
				val <<= 1;
				val |= als.getLast() ? 1 : 0;
				len++;
			}
			int eredmeny = getValue(val, len, forHuffmanTables);
			if (eredmeny >= 0 && eredmeny <= 15) {
				tree[i] = new HuffmanNode(eredmeny);
			} else if (eredmeny == 16) {
				int elozo = tree[i - 1].len;
				if (als.size() < 2) {
					byte[] buf = new byte[1];
					is.read(buf);
					Inflater.addBitsToAls(als, buf);
				}
				int repeat = als.getNextXBitIntegerLSBFirst(2) + 3;
				for (int j = 0; j < repeat; j++, i++) {
					tree[i] = new HuffmanNode(elozo);
				}
				i--;
			} else if (eredmeny == 17) {
				if (als.size() < 3) {
					byte[] buf = new byte[1];
					is.read(buf);
					Inflater.addBitsToAls(als, buf);
				}
				int repeat = als.getNextXBitIntegerLSBFirst(3) + 3;
				for (int j = 0; j < repeat; j++, i++) {
					tree[i] = new HuffmanNode(0);
				}
				i--;
			} else if (eredmeny == 18) {
				if (als.size() < 7) {
					byte[] buf = new byte[1];
					is.read(buf);
					Inflater.addBitsToAls(als, buf);
				}
				int repeat = als.getNextXBitIntegerLSBFirst(7) + 11;
				for (int j = 0; j < repeat; j++, i++) {
					tree[i] = new HuffmanNode(0);
				}
				i--;
			} else {
				System.err.println("MI A FENE?!");
			}
		}
	}

	public static int getValue(int val, int len, HuffmanNode[] table) {
		for (int i = 0; i < table.length; i++) {
			HuffmanNode h = table[i];
			if (h.code == val && h.len == len) {
				return i;
			}
		}
		return -1;
	}
}
