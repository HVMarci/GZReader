package hu.hvj.marci.gzreader;

import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class Main {

	public static void main(String[] args) throws Exception {
//		String filename = "C:\\HashiCorp\\Minecraft.tar.gz";
		String filename = "C:\\Users\\marci\\java_erdekessegek\\GZReader\\gunzip.c.gz";
//		String filename = "/home/marci/Letöltések/gunzip.c.gz";
//		String filename = "/home/marci/Dokumentumok/text.txt.gz";
		File f = new File(filename);
		FileInputStream fis = new FileInputStream(f);
		JFrame loading = new JFrame("Kitömörítés folyamatban...");
		JLabel label = new JLabel("Kitömörítés folyamatban...");
		label.setFont(new Font("Arial", Font.PLAIN, 40));
		loading.add(label);
		loading.setSize(500, 200);
		loading.setVisible(true);
		GZip gz = new GZip(fis, f);
		loading.dispose();
//		gz.writeData(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - 3));
//		gz.printData();
		GZGui gui = new GZGui(gz);
		gui.setVisible(true);
	}

}
