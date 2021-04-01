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
	private final int xlen, crc32, isize;
	private final ExtraField extraField;
	private final Date mtime;
	private final String originalFilename, comment;
	private final byte[] decompressedData;

	public GZip(InputStream is) throws IOException {
		//ArrayList<Byte> alsForCRC16 = new ArrayList<Byte>();
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
			this.originalFilename = null;
		}

		if (this.fcomment) {
			this.comment = readComment(is);
		} else {
			this.comment = null;
		}

		if (this.fhcrc) {
			byte[] buf = new byte[2];
			is.read(buf);
			int crc16 = Helper.twoBytesToIntLSBFirst(buf);
			// TODO check CRC-16
		}
		
		this.decompressedData = Inflater.inflate(is);
		
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

		if (this.mtime.getTime() != 0) {
			System.out.printf("Az utolsó módosítás dátuma: %1$tY. %1$tB %1$te. %1$tH:%1$tM:%1$tS%n", this.mtime);
		} else {
			System.out.println("Az utolsó módosítás dátuma nem elérhető.");
		}

		switch (this.xfl) {
		case 2:
			System.out.println("A tömörített állomány maximálisan lett tömörítve.");
			break;
		case 4:
			System.out.println("A tömörített állomány a leggyorsabb tömörítőalgoritmussal lett tömörítve.");
			break;
		default:
			System.out.printf("A tömörítőprogram ismeretlen tömörítést alkalmazott. (%d)%n", this.xfl);
			break;
		}

		System.out.print("A tömörítő számítógép operációs rendszere: ");
		switch (this.os & 0xFF) {
		case FAT:
			System.out.println("FAT fájlrendszer (MS-DOS, OS/2, NT/Win32)");
			break;
		case AMIGA:
			System.out.println("Amiga");
			break;
		case VMS:
			System.out.println("VMS (vagy OpenVMS)");
			break;
		case UNIX:
			System.out.println("Unix");
			break;
		case VM_CMS:
			System.out.println("VM/CMS");
			break;
		case ATARI_TOS:
			System.out.println("Atari TOS");
			break;
		case HPFS:
			System.out.println("HPFS fájlrendszer (OS/2, NT");
			break;
		case MACINTOSH:
			System.out.println("Macintosh");
			break;
		case Z_SYSTEM:
			System.out.println("Z-System");
			break;
		case CP_M:
			System.out.println("CP/M");
			break;
		case TOPS20:
			System.out.println("TOPS-20");
			break;
		case NTFS:
			System.out.println("NTFS fájlrendszer (NT)");
			break;
		case QDOS:
			System.out.println("QDOS");
			break;
		case ACORN_RISKOS:
			System.out.println("Acorn RISCOS");
			break;
		case UNKNOWN_OS:
			System.out.println("ismeretlen");
			break;
		default:
			System.err.println("meghatározatlan érték");
			break;
		}
		
		System.out.printf("Eredeti fájlméret: %d bájt%n", (long) this.isize & 0xFFFFFFFFL);
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

}
