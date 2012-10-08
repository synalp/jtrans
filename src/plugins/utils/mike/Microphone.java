/*
This source code is copyrighted by Christophe Cerisara, CNRS, France.

It is licensed under the terms of the INRIA Cecill-C licence, as described in:
http://www.cecill.info/licences/Licence_CeCILL-C_V1-en.html
*/

package plugins.utils.mike;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class Microphone {
	private TargetDataLine line;

	public static int sampleRate = 16000;

	private static Microphone unik=null;
	private Microphone() {
		AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, true);
		try {
			line = AudioSystem.getTargetDataLine(format);
			line.open();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}
	public static Microphone getMicrophone() {
		if (unik==null) unik = new Microphone();
		return unik;
	}

	public AudioInputStream getAudioStream() {
		line.start();
		return new AudioInputStream(line);
	}
}
