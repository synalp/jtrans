package fr.loria.synalp.jtrans.utils;

public class BufferUtils {

	/**
	 * Returns a buffer whose capacity is at least minLength.
	 * Reuses buf if it is large enough, or creates a new buffer otherwise.
	 */
	public static byte[] grow(byte[] buf, int minLength) {
		if (buf == null || buf.length < minLength) {
			return new byte[minLength];
		} else
			return buf;
	}

}
