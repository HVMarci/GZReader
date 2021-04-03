package hu.hvj.marci.gzreader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class GZip {

	public static final byte ID1 = 0x1F;
	public static final byte ID2 = (byte) 0x8B;

	public static final int FAT = 0;
	public static final int AMIGA = 1;
	public static final int VMS = 2;
	public static final int UNIX = 3;
	public static final int VM_CMS = 4;
	public static final int ATARI_TOS = 5;
	public static final int HPFS = 6;
	public static final int MACINTOSH = 7;
	public static final int Z_SYSTEM = 8;
	public static final int CP_M = 9;
	public static final int TOPS20 = 10;
	public static final int NTFS = 11;
	public static final int QDOS = 12;
	public static final int ACORN_RISKOS = 13;
	public static final int UNKNOWN_OS = 255;

	private final boolean ftext, fhcrc, fextra, fname, fcomment;
	private final byte cm, flg, xfl, os;
	private final short crc16;
	private final int xlen, crc32, isize;
	private final ExtraField extraField;
	private final Date mtime;
	private final String filename, originalFilename, comment;
	private final byte[] decompressedData;
	private final Inflater inflater;
	private final File file;

	public GZip(InputStream is, File file) throws IOException {
		this.filename = file.getName();
		this.file = file;
		// ArrayList<Byte> alsForCRC16 = new ArrayList<Byte>();
		byte[] ids = new byte[2];
		is.read(ids);
		if (ids[0] == ID1 && ids[1] == ID2) {
			System.out.println("Az ID1 és az ID2 bájt helyes.");
		} else {
			System.err.println("Az ID1 és/vagy az ID2 bájt hibás! " + Arrays.toString(ids));
		}

		is.read(ids);
		this.cm = ids[0];
		if (cm != 8) {
			System.err.printf("A tömörítési metódus nem támogatott! (%d)%n", cm);
		}
		this.flg = ids[1];

		boolean[] flags = Helper.byteToBooleansLSBFirst(flg);
		this.ftext = flags[0];
		this.fhcrc = flags[1];
		this.fextra = flags[2];
		this.fname = flags[3];
		this.fcomment = flags[4];

		boolean reserved1 = flags[5], reserved2 = flags[6], reserved3 = flags[7];
		if (!(reserved1 && reserved2 && reserved3)) {
			System.out.println("A reserved bitek helyesek.");
		} else {
			System.err.printf("A reserved bitek helytelenek! (%d, %d, %d)%n", reserved1 ? 1 : 0, reserved2 ? 1 : 0,
					reserved3 ? 1 : 0);
		}

		byte[] mtimebuf = new byte[4];
		is.read(mtimebuf);
		long mtime = Helper.fourBytesToIntLSBFirst(mtimebuf);
		this.mtime = new Date(mtime * 1000l);

		is.read(ids);
		this.xfl = ids[0];
		this.os = ids[1];

		if (this.fextra) {
			is.read(ids);
			this.xlen = Helper.twoBytesToIntLSBFirst(ids);
			byte[] xfbuf = new byte[xlen];
			is.read(xfbuf);
			this.extraField = new ExtraField(xfbuf);
		} else {
			this.xlen = 0;
			this.extraField = null;
		}

		if (this.fname) {
			this.originalFilename = readOriginalFilename(is);
		} else {
			this.originalFilename = filename.substring(0, filename.length() - 3);
		}

		if (this.fcomment) {
			this.comment = readComment(is);
		} else {
			this.comment = null;
		}

		if (this.fhcrc) {
			byte[] buf = new byte[2];
			is.read(buf);
			this.crc16 = (short) Helper.twoBytesToIntLSBFirst(buf);
			// TODO check CRC-16
		} else {
			this.crc16 = 0;
		}

		this.inflater = new Inflater();
		this.decompressedData = inflater.inflate(is);

		byte[] buf = new byte[4];
		is.read(buf);
		this.crc32 = Helper.fourBytesToIntLSBFirst(buf);
		if (this.crc32 != CRC.crc32(this.decompressedData)) {
			System.err.println("Hibás CRC32!");
		}

		is.read(buf);
		this.isize = Helper.fourBytesToIntLSBFirst(buf);
	}

	public void printData() {
		if (cm == 8) {
			System.out.println("A tömörítési metódus deflate.");
		} else {
			System.out.printf("A tömörítési metódus nem támogatott! (%d)%n", cm);
		}

		if (ftext) {
			System.out.println("A tömörített állomány egy ASCII szövegfájl.");
		} else {
			System.out.println("A tömörített állomány nem ASCII szövegfájl.");
		}

		if (fhcrc) {
			System.out.println("Van ellenőrző CRC-16.");
		} else {
			System.out.println("Nincs ellenőrző CRC-16.");
		}

		if (fextra) {
			System.out.println("Van opcionális extra mező.");
			System.out.printf("Az opcionális mező hossza %d bájt.%n", this.extraField);
			this.extraField.printData();
		} else {
			System.out.println("Nincs opcionális extra mező.");
		}

		if (fname) {
			System.out.println("Az eredeti fájlnév: " + this.originalFilename);
		} else {
			System.out.println("Nincs eltárolva az eredeti fájlnév.");
		}

		if (fcomment) {
			System.out.println("A komment: " + this.comment);
		} else {
			System.out.println("A fájl nem tartalmaz kommentet.");
		}

		System.out.println("Az utolsó módosítás dátuma: " + this.getLastModificationTime());

		System.out.println(this.getCompressionAlgorithm());

		System.out.println("A tömörítő számítógép operációs rendszere: " + this.getOSName());

		System.out.printf("Eredeti fájlméret: %d bájt%n", this.getOriginalSize());
	}

	public int getCompressionMethod() {
		return this.cm;
	}

	public String getCompressionMethodName() {
		if (this.cm == 8) {
			return "deflate";
		} else {
			return "ismeretlen";
		}
	}

	public String getOriginalFilename() {
		return this.originalFilename;
	}

	public void writeData(String filename) throws IOException {
		File f = new File(filename);
		if (this.originalFilename != null) {
			String levagott = f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - f.getName().length());
			f = new File(levagott.concat(this.originalFilename));
		}
		f.createNewFile();
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(this.decompressedData);
		fos.close();
	}

	public static String readOriginalFilename(InputStream is) throws IOException {
		ArrayList<Byte> als = new ArrayList<Byte>();
		byte[] buf = new byte[1];
		do {
			is.read(buf);
			als.add(buf[0]);
		} while (buf[0] != 0);

		byte[] name = new byte[als.size() - 1];
		for (int i = 0; i < name.length; i++) {
			name[i] = als.get(i).byteValue();
		}

		return new String(name, Charset.forName("ISO-8859-1"));
	}

	public static String readComment(InputStream is) throws IOException {
		return readOriginalFilename(is);
	}

	public long getOriginalSize() {
		return (long) this.isize & 0xFFFFFFFFL;
	}

	public String getLastModificationTime() {
		if (this.mtime.getTime() != 0) {
			return String.format("%1$tY. %1$tB %1$te. %1$tH:%1$tM:%1$tS%n", this.mtime);
		} else {
			return "nem elérhető";
		}
	}

	public String getCompressionAlgorithm() {
		switch (this.xfl) {
		case 2:
			return "A tömörített állomány maximálisan lett tömörítve.";
		case 4:
			return "A tömörített állomány a leggyorsabb tömörítőalgoritmussal lett tömörítve.";
		default:
			return String.format("A tömörítőprogram ismeretlen hatékonyságú tömörítést alkalmazott. (%d)", this.xfl);
		}
	}

	public String getOSName() {
		switch (this.os & 0xFF) {
		case FAT:
			return "FAT fájlrendszer (MS-DOS, OS/2, NT/Win32)";
		case AMIGA:
			return "Amiga";
		case VMS:
			return "VMS (vagy OpenVMS)";
		case UNIX:
			return "Unix";
		case VM_CMS:
			return "VM/CMS";
		case ATARI_TOS:
			return "Atari TOS";
		case HPFS:
			return "HPFS fájlrendszer (OS/2, NT";
		case MACINTOSH:
			return "Macintosh";
		case Z_SYSTEM:
			return "Z-System";
		case CP_M:
			return "CP/M";
		case TOPS20:
			return "TOPS-20";
		case NTFS:
			return "NTFS fájlrendszer (NT)";
		case QDOS:
			return "QDOS";
		case ACORN_RISKOS:
			return "Acorn RISCOS";
		case UNKNOWN_OS:
			return "ismeretlen";
		default:
			return "meghatározatlan érték";
		}
	}

	public String getFilename() {
		return this.filename;
	}

	public boolean getFNAME() {
		return this.fname;
	}

	public Inflater getInflater() {
		return this.inflater;
	}

	public File getFile() {
		return this.file;
	}

	public boolean getFTEXT() {
		return this.ftext;
	}

	public boolean getFCOMMENT() {
		return this.fcomment;
	}

	public boolean getFEXTRA() {
		return this.fextra;
	}

	public boolean getFHCRC() {
		return this.fhcrc;
	}

	public short getCRC16() {
		return this.crc16;
	}

	public int getCRC32() {
		return this.crc32;
	}

	public String getComment() {
		return this.comment;
	}

	public ExtraField getExtraField() {
		return this.extraField;
	}

	public int getFLG() {
		return this.flg;
	}

	public int getXFL() {
		return this.xfl;
	}

	public int getOS() {
		return (int) this.os & 0xFF;
	}
	
	public int getXLEN() {
		return this.xlen;
	}
	
	public Date getMTIME() {
		return this.mtime;
	}

}
