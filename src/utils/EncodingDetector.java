package utils;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;

/**
 * Detects Unicode byte order marks and falls back to juniversalchardet's
 * detection if needed.
 */
public class EncodingDetector {
	private static long bytesToLong(byte[] b, int len) {
		long magic = 0;
		for (int i = 0; i < len; i++)
			magic = (magic << 8) | (b[i]&0xff);
		return magic;
	}


	/**
	 * Returns a BufferedReader ready to use with the right encoding for the
	 * given file. Any byte order marks are skipped automatically.
	 */
	public static BufferedReader properReader(File file) throws IOException {
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