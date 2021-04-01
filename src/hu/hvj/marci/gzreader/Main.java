package hu.hvj.marci.gzreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Main {

	public static void main(String[] args) throws Exception {
		String filename = "C:\\HashiCorp\\Minecraft.tar.gza.gz";
//		String filename = "C:\\Users\\marci\\java_erdekessegek\\GZReader\\Minecraft.tar.gz";
//		String filename = "/home/marci/Letöltések/gunzip.c.gz";
//		String filename = "/home/marci/Dokumentumok/text.txt.gz";
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		GZip gz = new GZip(fis);
		gz.writeData(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - 3));
//		gz.printData();
	}

	public static void main2(String[] args) throws Exception {
		byte[] testStored = { 0x01, 0x04, 0x00, ~0x04, ~0x00, 'S', 'z', 'i', 'a' };
		File f = new File("/home/marci/Dokumentumok/deflate");
		f.createNewFile();
		FileOutputStream bemenet = new FileOutputStream(f);
		bemenet.write(testStored);
		bemenet.close();
		FileInputStream fis = new FileInputStream(f);
		byte[] decompressed = Inflater.inflate(fis);
		File f2 = new File("/home/marci/Dokumentumok/kitomoritett");
		FileOutputStream eredmeny = new FileOutputStream(f2);
		eredmeny.write(decompressed);
		eredmeny.close();
	}
	
	public static void main3(String[] args) throws Exception {
//		byte[] huffman = { 69, (byte) 167, 28 };
		byte[] huffman = { 125, 7, 75, 102, (byte) 218, 126, 3 };
		File f = new File("/home/marci/Dokumentumok/huffman");
		FileOutputStream os = new FileOutputStream(f);
		os.write(huffman);
		os.close();
		FileInputStream fis = new FileInputStream(f);
		BooleanArrayList bals = new BooleanArrayList();
		HuffmanCode hc = new HuffmanCode(bals, fis);
		fis.close();
	}
	
	public static void main4(String[] args) {
		HuffmanCode h = HuffmanCode.STATIC_HUFFMAN_TABLE;
		System.out.println(h.literal[256]);
	}

}
