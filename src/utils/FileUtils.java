/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
 */

package utils;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;

public class FileUtils {
	public static BufferedReader openFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(nom);
		return new BufferedReader(new InputStreamReader(fis, "UTF-8"));
	}

	public static BufferedReader openFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		FileInputStream fis = new FileInputStream(nom);
		return new BufferedReader(new InputStreamReader(fis, "ISO-8859-1"));
	}

	public static PrintWriter writeFileISO(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		return new PrintWriter(nom, "ISO-8859-1");
	}

	public static PrintWriter writeFileUTF(String nom) throws UnsupportedEncodingException, FileNotFoundException {
		return new PrintWriter(nom, "UTF-8");
	}

	/**
	 * enleve l'extension d'un nom de fichier
	 */
	public static String noExt(String fich) {
		int i = fich.lastIndexOf('.');
		if (i < 0) {
			return fich;
		}
		return fich.substring(0, i);
	}



	private static long bytesToLong(byte[] b, int len) {
		long magic = 0;
		for (int i = 0; i < len; i++)
			magic = (magic << 8) | (b[i]&0xff);
		return magic;
	}


	/**
	 * Returns a BufferedReader ready to use with the right encoding for the
	 * given file. Any byte order marks are skipped automatically.
	 *
	 * Detects Unicode byte order marks and falls back to juniversalchardet's
	 * detection if needed.
	 */
	public static BufferedReader openFileAutoCharset(File file) throws IOException {
		String encoding = null;
		int skip = 0;

		InputStream bis = new FileInputStream(file);

		byte[] b = new byte[4];
		int len = bis.read(b);

		if (len >= 2) {
			long bom = bytesToLong(b, 2);
			if (0xFFFE == bom) {
				encoding = "UnicodeLittleUnmarked";
				skip = 2;
			} else if (0xFEFF == bom) {
				encoding = "UnicodeBigUnmarked";
				skip = 2;
			}
		}

		if (len >= 3 && 0xEFBBBF == bytesToLong(b, 3)) {
			encoding = "UTF8";
			skip = 3;
		}

		if (encoding == null) {
			b = new byte[4096];

			UniversalDetector detector = new UniversalDetector(null);

			int nread;
			while ((nread = bis.read(b)) > 0 && !detector.isDone()) {
				detector.handleData(b, 0, nread);
			}

			detector.dataEnd();
			encoding = detector.getDetectedCharset();
		}

		bis.close();
		bis = new FileInputStream(file);
		if (skip > 0)
			bis.skip(skip);

		if (encoding != null) {
			System.out.println("Detected encoding: " + encoding);
			return new BufferedReader(new InputStreamReader(bis, encoding));
		} else
			return new BufferedReader(new InputStreamReader(bis));
	}
}
