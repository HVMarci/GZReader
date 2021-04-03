package hu.hvj.marci.gzreader;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GZGui extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7331702592310606046L;
	private int yCounter = 25;

	public GZGui(GZip gz) throws IOException {
		super("GZip reader");
		setSize(800, 600);
		getContentPane().setLayout(null);

		Font font = new Font("Times New Roman", Font.PLAIN, 20);

		JLabel filename = new JLabel("Fájlnév: " + gz.getFilename());
		filename.setFont(font);
		resize(filename);
		getContentPane().add(filename);

		JLabel originalFilename = new JLabel("Eredeti fájlnév: " + gz.getOriginalFilename()
				+ (gz.getFNAME() ? " (el lett tárolva)" : " (fájlnévből kitalálva)"));
		originalFilename.setFont(font);
		resize(originalFilename);
		getContentPane().add(originalFilename);

		JLabel originalSize = new JLabel(String.format("Fájlméret tömörítés előtt: %,d bájt%s", gz.getOriginalSize(),
				Helper.unitToSmallest(gz.getOriginalSize())));
		originalSize.setFont(font);
		resize(originalSize);
		getContentPane().add(originalSize);

		long fileSize = Files.size(Paths.get(gz.getFile().getAbsolutePath()));
		JLabel size = new JLabel(
				String.format("Fájlméret tömörítés után: %,d bájt%s", fileSize, Helper.unitToSmallest(fileSize)));
		size.setFont(font);
		resize(size);
		getContentPane().add(size);

		long nyereseg = gz.getOriginalSize() - fileSize;
		JLabel kulonbseg = new JLabel(String.format("Nyereség: %,d bájt%s", nyereseg, Helper.unitToSmallest(nyereseg)));
		kulonbseg.setFont(font);
		resize(kulonbseg);
		getContentPane().add(kulonbseg);

		JLabel text = new JLabel(gz.getFTEXT() ? "A tömörített fájl valószínűleg egy ASCII szövegfájl"
				: "A tömörített fájl valószínűleg nem egy ASCII szövegfájl");
		text.setFont(font);
		resize(text);
		getContentPane().add(text);

		JLabel compressionMethod = new JLabel("Tömörítési metódus: " + gz.getCompressionMethodName());
		compressionMethod.setFont(font);
		resize(compressionMethod);
		getContentPane().add(compressionMethod);

		JLabel os = new JLabel("A tömörítő számítógép operációs rendszere: " + gz.getOSName());
		os.setFont(font);
		resize(os);
		getContentPane().add(os);

		JLabel algorithm = new JLabel(gz.getCompressionAlgorithm());
		algorithm.setFont(font);
		resize(algorithm);
		getContentPane().add(algorithm);

		JLabel crc16 = new JLabel(gz.getFHCRC() ? String.format("Az ellenőrző CRC-16: 0x%04X", gz.getCRC16())
				: "A fájl nem tartalmaz ellenőrző CRC-16-ot");
		crc16.setFont(font);
		resize(crc16);
		getContentPane().add(crc16);

		JLabel blockCount = new JLabel("Blokkok száma: " + gz.getInflater().getBlockCount());
		blockCount.setFont(font);
		resize(blockCount);
		getContentPane().add(blockCount);

		JLabel blockTypes = new JLabel("Blokk típusok:");
		blockTypes.setFont(font);
		resize(blockTypes);
		getContentPane().add(blockTypes);

		JLabel blockType1 = new JLabel("  Tárolva: " + gz.getInflater().getBlockTypeCount(Inflater.STORED) + " db");
		blockType1.setFont(font);
		resize(blockType1);
		getContentPane().add(blockType1);

		JLabel blockType2 = new JLabel(
				"  Fix huffman tábla: " + gz.getInflater().getBlockTypeCount(Inflater.FIXED_HUFFMAN_CODES) + " db");
		blockType2.setFont(font);
		resize(blockType2);
		getContentPane().add(blockType2);

		JLabel blockType3 = new JLabel("  Dinamikus huffman tábla: "
				+ gz.getInflater().getBlockTypeCount(Inflater.DYNAMIC_HUFFMAN_CODES) + " db");
		blockType3.setFont(font);
		resize(blockType3);
		getContentPane().add(blockType3);

		JLabel crc32 = new JLabel(String.format("Az ellenőrző CRC-32: 0x%08X", gz.getCRC32()));
		crc32.setFont(font);
		resize(crc32);
		getContentPane().add(crc32);

		JLabel timestamp = new JLabel("A tömörített fájl utolsó módosításásának az ideje: " + gz.getLastModificationTime());
		timestamp.setFont(font);
		resize(timestamp);
		getContentPane().add(timestamp);

		if (gz.getFCOMMENT()) {
			JLabel comment = new JLabel("A fájl tartalmaz egy kommentet: " + gz.getComment());
			comment.setFont(font);
			resize(comment);
			getContentPane().add(comment);
		}

		if (gz.getFEXTRA()) {
			JLabel extra = new JLabel("A fájl tartalmaz egy extra mezőt. Azonosítója: " + gz.getExtraField().getID());
			extra.setFont(font);
			resize(extra);
			getContentPane().add(extra);
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	public void resize(JLabel label) {
		label.setBounds(26, yCounter, 733, 24);
		yCounter += 25;
	}
}
