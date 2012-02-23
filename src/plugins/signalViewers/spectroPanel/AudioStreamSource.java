package plugins.signalViewers.spectroPanel;

import java.io.IOException;
import java.net.URL;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.swing.JOptionPane;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataEndSignal;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.DataStartSignal;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.util.StreamDataSource;


/**
 * contient
 * 
 * @author cerisara
 *
 */
public class AudioStreamSource extends StreamDataSource {
	byte[] buf = new byte[1000000];
	AudioInputStream audiodata;
	AudioFormat format;
	int pos=-1;
	int nread=-1;

	public static void main(String args[]) throws Exception {
		URL u = new URL("http://rapsodis.loria.fr/jtrans/culture.wav");
		AudioInputStream ais = AudioSystem.getAudioInputStream(u);
		byte[] b = new byte[2];
		for (;;) {
			ais.read(b);
			System.out.println("debug "+b[0]);
		}
	}

	public AudioStreamSource(AudioInputStream ain) {
		audiodata = ain;
		format = ain.getFormat();
		try {
			nread=0;
			for (;;) {
				int n=audiodata.read(buf,nread,buf.length-nread);
				if (n<0) break;
				nread+=n;
				if (nread>=buf.length) break;
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "ARGH "+e);
			e.printStackTrace();
		}
	}
	int detnread = 0;
	@Override
	public Data getData() throws DataProcessingException {
		try {
			// applet debug: cette fonction est bien appelee plusieurs fois par FrontEnd, mais aucune data ne sort
			// de FrontEnd ??
			detnread++;
			if (pos<0) {
				pos=0;
				return new DataStartSignal(16000);
			}
			while (pos>=nread) {
				// debug !!!
if (true)  {
 pos=0; break;
}
				if (nread<buf.length) {
					return new DataEndSignal(0);
				}
				nread=audiodata.read(buf);
				pos=0;
			}
			short s = bytesToShort(format, buf, pos);
			double[] dd = {s};
			DoubleData d = new DoubleData(dd);
			pos+=2;
			return d;
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, "ARGH "+e);
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Convert the bytes starting at the given offset to a signed short based upon the AudioFormat.  If the frame size
	 * is 1, then the value is doubled to make it match a frame size of 2.
	 *
	 * @param format    the audio format
	 * @param byteArray the byte array
	 * @return a short
	 * @throws java.lang.ArrayIndexOutOfBoundsException
	 *
	 */
	public static short bytesToShort(AudioFormat format,
			byte[] byteArray) {
		short result = 0;
		Encoding encoding = format.getEncoding();
		int frameSize = format.getFrameSize();

		if (encoding == Encoding.PCM_SIGNED) {
			result = toShort(byteArray, format.isBigEndian());
			if (frameSize == 1) {
				result = (short) (result << 8);
			}
		} else if (encoding == Encoding.PCM_UNSIGNED) {
			int tmp = toUnsignedShort(byteArray, format.isBigEndian());
			if (frameSize == 1) {
				tmp = tmp << 8;
			}
			result = (short) (tmp - (2 << 14));
		} else if (encoding == Encoding.ULAW) {
			result = ulawTable[byteArray[0] + 128];
		} else {
			System.out.println("Unknown encoding: " + encoding);
		}
		return result;
	}
	public static short bytesToShort(AudioFormat format, byte[] byteArray, int deb) {
		short result = 0;
		Encoding encoding = format.getEncoding();
		int frameSize = format.getFrameSize();

		if (encoding == Encoding.PCM_SIGNED) {
			result = toShort(byteArray, format.isBigEndian(), deb);
			if (frameSize == 1) {
				result = (short) (result << 8);
			}
		} else if (encoding == Encoding.PCM_UNSIGNED) {
			int tmp = toUnsignedShort(byteArray, format.isBigEndian(), deb);
			if (frameSize == 1) {
				tmp = tmp << 8;
			}
			result = (short) (tmp - (2 << 14));
		} else if (encoding == Encoding.ULAW) {
			result = ulawTable[byteArray[deb] + 128];
		} else {
			System.out.println("Unknown encoding: " + encoding);
		}
		return result;
	}
	/** Converts a byte array to a signed short value. */
	static public short toShort(byte[] bytes, boolean bigEndian) {
		if (bytes.length == 1) {
			return (short) bytes[0];
		} else if (bigEndian) {
			return (short) ((bytes[0] << 8) | (0xff & bytes[1]));
		} else {
			return (short) ((bytes[1] << 8) | (0xff & bytes[0]));
		}
	}
	static public short toShort(byte[] bytes, boolean bigEndian, int deb) {
		if (bytes.length == 1) {
			return (short) bytes[deb];
		} else if (bigEndian) {
			return (short) ((bytes[deb] << 8) | (0xff & bytes[deb+1]));
		} else {
			return (short) ((bytes[deb+1] << 8) | (0xff & bytes[deb]));
		}
	}
	/** Converts a byte array into an unsigned short. */
	static public int toUnsignedShort(byte[] bytes, boolean bigEndian) {
		if (bytes.length == 1) {
			return 0xff & bytes[0];
		} else if (bigEndian) {
			return ((bytes[0] & 0xff) << 8) | (0xff & bytes[1]);
		} else {
			return ((bytes[1] & 0xff) << 8) | (0xff & bytes[0]);
		}
	}
	static public int toUnsignedShort(byte[] bytes, boolean bigEndian, int deb) {
		if (bytes.length == 1) {
			return 0xff & bytes[deb];
		} else if (bigEndian) {
			return ((bytes[deb] & 0xff) << 8) | (0xff & bytes[deb+1]);
		} else {
			return ((bytes[deb+1] & 0xff) << 8) | (0xff & bytes[deb]);
		}
	}
	/** Index = ulaw value, entry = signed 16 bit value. */
	static final private short[] ulawTable = {
		32760, 31608, 30584, 29560, 28536, 27512, 26488, 25464, 24440,
		23416, 22392, 21368, 20344, 19320, 18296, 17272, 16248, 15736,
		15224, 14712, 14200, 13688, 13176, 12664, 12152, 11640, 11128,
		10616, 10104, 9592, 9080, 8568, 8056, 7800, 7544, 7288, 7032,
		6776, 6520, 6264, 6008, 5752, 5496, 5240, 4984, 4728, 4472,
		4216, 3960, 3832, 3704, 3576, 3448, 3320, 3192, 3064, 2936,
		2808, 2680, 2552, 2424, 2296, 2168, 2040, 1912, 1848, 1784,
		1720, 1656, 1592, 1528, 1464, 1400, 1336, 1272, 1208, 1144,
		1080, 1016, 952, 888, 856, 824, 792, 760, 728, 696, 664, 632,
		600, 568, 536, 504, 472, 440, 408, 376, 360, 344, 328, 312,
		296, 280, 264, 248, 232, 216, 200, 184, 168, 152, 136, 120,
		112, 104, 96, 88, 80, 72, 64, 56, 48, 40, 32, 24, 16, 8, 0,
		-32760, -31608, -30584, -29560, -28536, -27512, -26488, -25464,
		-24440, -23416, -22392, -21368, -20344, -19320, -18296, -17272,
		-16248, -15736, -15224, -14712, -14200, -13688, -13176, -12664,
		-12152, -11640, -11128, -10616, -10104, -9592, -9080, -8568,
		-8056, -7800, -7544, -7288, -7032, -6776, -6520, -6264, -6008,
		-5752, -5496, -5240, -4984, -4728, -4472, -4216, -3960, -3832,
		-3704, -3576, -3448, -3320, -3192, -3064, -2936, -2808, -2680,
		-2552, -2424, -2296, -2168, -2040, -1912, -1848, -1784, -1720,
		-1656, -1592, -1528, -1464, -1400, -1336, -1272, -1208, -1144,
		-1080, -1016, -952, -888, -856, -824, -792, -760, -728, -696,
		-664, -632, -600, -568, -536, -504, -472, -440, -408, -376,
		-360, -344, -328, -312, -296, -280, -264, -248, -232, -216,
		-200, -184, -168, -152, -136, -120, -112, -104, -96, -88, -80,
		-72, -64, -56, -48, -40, -32, -24, -16, -8, 0};

}
